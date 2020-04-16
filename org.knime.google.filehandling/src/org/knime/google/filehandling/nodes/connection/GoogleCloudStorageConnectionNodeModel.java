/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   2020-03-24 (Alexander Bondaletov): created
 */
package org.knime.google.filehandling.nodes.connection;

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.google.api.data.GoogleApiConnectionPortObject;
import org.knime.google.filehandling.connections.GoogleCloudStorageConnection;
import org.knime.google.filehandling.connections.GoogleCloudStorageFileSystem;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

/**
 * Google Cloud Storage Connection node. Takes
 * {@link GoogleApiConnectionPortObject} and creates
 * {@link FileSystemPortObject}.
 *
 * @author Alexander Bondaletov
 */
public class GoogleCloudStorageConnectionNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(GoogleCloudStorageConnectionNodeModel.class);

    private static final String FILE_SYSTEM_NAME = "Google Cloud Storage";
    private static final String KEY_PROJECT_ID = "projectId";

    private String m_fsId;
    private GoogleCloudStorageConnection m_fsConnection;

    private SettingsModelString m_projectIdSettings = createProjectIdSettings();

    static SettingsModelString createProjectIdSettings() {
        return new SettingsModelString(KEY_PROJECT_ID, "");
    }

    /**
     * Creates new instance.
     */
    protected GoogleCloudStorageConnectionNodeModel() {
        super(new PortType[] { GoogleApiConnectionPortObject.TYPE }, new PortType[] { FileSystemPortObject.TYPE });
    }

    @SuppressWarnings("resource")
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        GoogleApiConnectionPortObject apiConnection = (GoogleApiConnectionPortObject) inObjects[0];
        m_fsConnection = new GoogleCloudStorageConnection(apiConnection.getGoogleApiConnection(),
                m_projectIdSettings.getStringValue());
        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);

        try {
            ((GoogleCloudStorageFileSystem) m_fsConnection.getFileSystem()).getClient().listBuckets(null);
        } catch (TokenResponseException e) {
            throw new InvalidSettingsException(e.getDetails().getErrorDescription(), e);
        } catch (GoogleJsonResponseException e) {
            throw new InvalidSettingsException(e.getDetails().getMessage(), e);
        }

        return new PortObject[] { new FileSystemPortObject(createSpec()) };
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_projectIdSettings.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Project ID is not configured");
        }
        m_fsId = FSConnectionRegistry.getInstance().getKey();
        return new PortObjectSpec[] { createSpec() };
    }

    private FileSystemPortObjectSpec createSpec() {
        return new FileSystemPortObjectSpec(FILE_SYSTEM_NAME, m_fsId);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        setWarningMessage("Google Cloud Storage connection no longer available. Please re-execute the node.");

    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to save
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_projectIdSettings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_projectIdSettings.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_projectIdSettings.loadSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        try {
            if (m_fsConnection != null) {
                m_fsConnection.closeFileSystem();
            }
        } catch (final IOException ex) {
            LOGGER.error("Exception closing file system: " + ex.getMessage(), ex);
        } finally {
            FSConnectionRegistry.getInstance().deregister(m_fsId);
            m_fsId = null;
            m_fsConnection = null;
        }
    }

}