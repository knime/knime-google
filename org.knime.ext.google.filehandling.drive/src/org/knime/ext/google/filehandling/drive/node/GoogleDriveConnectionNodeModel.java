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
 *   2020-09-01 (Vyacheslav Soldatov): created
 */
package org.knime.ext.google.filehandling.drive.node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

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
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFSConnection;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFSDescriptorProvider;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFileSystemProvider;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.google.api.credential.GoogleCredential;

import com.google.auth.Credentials;

/**
 * Google Drive Connection node.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
final class GoogleDriveConnectionNodeModel extends NodeModel {

    private static final String FILE_SYSTEM_NAME = "Google Drive";

    private final GoogleDriveConnectionSettingsModel m_settings;

    private String m_fsId;

    private GoogleDriveFSConnection m_fsConnection;

    /**
     * Creates new instance.
     */
    protected GoogleDriveConnectionNodeModel() {
        super(new PortType[] { CredentialPortObject.TYPE }, new PortType[] { FileSystemPortObject.TYPE });

        m_settings = new GoogleDriveConnectionSettingsModel();
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final var credentials = getCredentials(inObjects[0].getSpec());

        m_fsConnection = new GoogleDriveFSConnection(m_settings.toFSConnectionConfig(credentials));
        testConnection(m_fsConnection);
        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);

        return new PortObject[] { new FileSystemPortObject(createSpec()) };
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        getCredentials(inSpecs[0]);
        m_fsId = FSConnectionRegistry.getInstance().getKey();
        return new PortObjectSpec[] { createSpec() };
    }

    /**
     * @param connection
     *            connection to test.
     * @throws IOException
     */
    public static void testConnection(final GoogleDriveFSConnection connection) throws IOException {
        @SuppressWarnings("resource")
        FSPath root = connection.getFileSystem().getPath("/" + GoogleDriveFileSystemProvider.MY_DRIVE);
        try (Stream<Path> files = Files.list(root)) {
            // Do nothing. The file listing is not lazy implemented
            // in provider therefore should throw exception if connection is bad
        }
    }

    private static Credentials getCredentials(final PortObjectSpec inSpec) throws InvalidSettingsException {
        final var googleSpec = ((CredentialPortObjectSpec) inSpec);
        final var credentialsOpt = googleSpec.getCredential(GoogleCredential.class);
        if (credentialsOpt.isEmpty()) {
            throw new InvalidSettingsException(GoogleCredential.NO_AUTH_ERROR);
        }
        return credentialsOpt.get().getCredentials();
    }

    private FileSystemPortObjectSpec createSpec() {
        return new FileSystemPortObjectSpec(FILE_SYSTEM_NAME, m_fsId, GoogleDriveFSDescriptorProvider.FS_LOCATION_SPEC);
    }

    /**
     * @param settings
     *            connection settings.
     * @param inputSpec
     *            port object input specification.
     * @return file system connection.
     * @throws InvalidSettingsException
     */
    public static GoogleDriveFSConnection createConnection(final GoogleDriveConnectionSettingsModel settings,
            final PortObjectSpec[] inputSpec) throws InvalidSettingsException {
        return new GoogleDriveFSConnection(settings.toFSConnectionConfig(getCredentials(inputSpec[0])));
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
    protected void saveSettingsTo(final NodeSettingsWO output) {
        m_settings.save(output);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO input) throws InvalidSettingsException {
        m_settings.validate(input);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO input) throws InvalidSettingsException {
        m_settings.load(input);
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
