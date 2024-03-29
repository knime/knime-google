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
package org.knime.ext.google.filehandling.cloudstorage.node;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFSConnection;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFSDescriptorProvider;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFileSystem;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.google.api.credential.CredentialUtil;

import com.google.cloud.storage.StorageException;

/**
 * Node model for Google Cloud Storage Connector node. Takes
 * {@link CredentialPortObject} and creates {@link FileSystemPortObject}.
 *
 * @author Alexander Bondaletov
 */
class CloudStorageConnectorNodeModel extends NodeModel {

    private static final String FILE_SYSTEM_NAME = "Google Cloud Storage";

    private String m_fsId;
    private CloudStorageFSConnection m_fsConnection;

    private final CloudStorageConnectorSettings m_settings = new CloudStorageConnectorSettings();

    /**
     * Creates new instance.
     */
    protected CloudStorageConnectorNodeModel() {
        super(new PortType[] { CredentialPortObject.TYPE }, new PortType[] { FileSystemPortObject.TYPE });
    }

    @SuppressWarnings("resource")
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final var inSpec = (CredentialPortObjectSpec) inObjects[0].getSpec();

        final var creds = CredentialUtil.toOAuth2Credentials(inSpec);

        m_fsConnection = new CloudStorageFSConnection(m_settings.toFSConnectionConfig(creds));
        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);

        try {
            ((CloudStorageFileSystem) m_fsConnection.getFileSystem()).getClient().listBuckets(null);
        } catch (StorageException e) {
            final var message = e.getMessage() + " " + StringUtils.trimToEmpty(e.getReason());
            throw new InvalidSettingsException(message, e);
        }
        return new PortObject[] { new FileSystemPortObject(createSpec()) };
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_fsId = FSConnectionRegistry.getInstance().getKey();
        return new PortObjectSpec[] { createSpec() };
    }

    private FileSystemPortObjectSpec createSpec() {
        return new FileSystemPortObjectSpec(FILE_SYSTEM_NAME, //
                m_fsId, //
                CloudStorageFSDescriptorProvider.FS_LOCATION_SPEC);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        setWarningMessage("Connection no longer available. Please re-execute the node.");

    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to save
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    @Override
    protected void onDispose() {
        // close the file system also when the workflow is closed
        reset();
    }

    @Override
    protected void reset() {
        if (m_fsConnection != null) {
            m_fsConnection.closeInBackground();
            m_fsConnection = null;
        }
        m_fsId = null;
    }
}
