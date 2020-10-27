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
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFSConnection;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFileSystem;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFileSystemProvider;
import org.knime.ext.google.filehandling.drive.fs.GoogleDrivePath;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.google.api.data.GoogleApiConnection;
import org.knime.google.api.data.GoogleApiConnectionPortObject;
import org.knime.google.api.data.GoogleApiConnectionPortObjectSpec;

/**
 * Google Drive Connection node.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class GoogleDriveConnectionNodeModel extends NodeModel {

    private static final String FILE_SYSTEM_NAME = "Google Drive";

    private String m_fsId;

    private GoogleDriveFSConnection m_fsConnection;

    /**
     * Creates new instance.
     */
    protected GoogleDriveConnectionNodeModel() {
        super(new PortType[] { GoogleApiConnectionPortObject.TYPE }, new PortType[] { FileSystemPortObject.TYPE });
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        GoogleApiConnection connection = ((GoogleApiConnectionPortObject) inObjects[0])
                .getGoogleApiConnection();
        // to do check the grant to Google Drive Service.
        m_fsConnection = new GoogleDriveFSConnection(connection, "/");
        testConnection();
        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);
        return new PortObject[] { new FileSystemPortObject(createSpec()) };
    }

    private void testConnection() throws IOException {
        @SuppressWarnings("resource")
        GoogleDriveFileSystem fs = m_fsConnection.getFileSystem();
        GoogleDrivePath root = fs.getPath("/" + GoogleDriveFileSystemProvider.MY_DRIVE);
        try (Stream<Path> files = Files.list(root)) {
            // Do nothing. The file listing is not lazy implemented
            // in provider therefore should throw exception if connection is bad
        }
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        GoogleApiConnection connection = ((GoogleApiConnectionPortObjectSpec) inSpecs[0]).getGoogleApiConnection();
        if (connection == null) {
            throw new InvalidSettingsException("Not authenticated");
        }
        m_fsId = FSConnectionRegistry.getInstance().getKey();
        return new PortObjectSpec[] { createSpec() };
    }

    private FileSystemPortObjectSpec createSpec() {
        return new FileSystemPortObjectSpec(FILE_SYSTEM_NAME, m_fsId, GoogleDriveFileSystem.createFSLocationSpec());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        setWarningMessage("Google Drive connection no longer available. Please re-execute the node.");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to save

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // Have not any settings. Nothing to save
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // Have not any settings. Nothing to validate
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // Have not any settings. Nothing to load
    }

    @Override
    protected void onDispose() {
        // close the file system also when the workflow is closed
        reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_fsId = null;
    }
}
