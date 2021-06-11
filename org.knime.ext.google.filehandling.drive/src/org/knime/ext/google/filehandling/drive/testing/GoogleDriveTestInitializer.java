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
package org.knime.ext.google.filehandling.drive.testing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.UUID;

import org.knime.core.node.NodeLogger;
import org.knime.ext.google.filehandling.drive.fs.FileMetadata;
import org.knime.ext.google.filehandling.drive.fs.FileMetadata.FileType;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFSConnection;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFileAttributes;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFileSystem;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveHelper;
import org.knime.ext.google.filehandling.drive.fs.GoogleDrivePath;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.testing.DefaultFSTestInitializer;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.model.Drive;
import com.google.api.services.drive.model.File;

/**
 * Google Drive test initializer.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
class GoogleDriveTestInitializer extends DefaultFSTestInitializer<GoogleDrivePath, GoogleDriveFileSystem> {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(GoogleDriveTestInitializer.class);

    private boolean m_isWorkingDirCreated = false;

    private GoogleDriveHelper m_helper;
    private final GoogleDriveFileSystem m_fileSystem;

    /**
     * Creates new instance
     * @param connection {@link FSConnection} object.
     */
    public GoogleDriveTestInitializer(final GoogleDriveFSConnection connection) {
        super(connection);
        m_fileSystem = connection.getFileSystem();
        m_helper = m_fileSystem.provider().getHelper();
    }

    @Override
    public GoogleDrivePath createFileWithContent(final String content, final String... pathComponents)
            throws IOException {
        final GoogleDrivePath path = makePath(pathComponents);
        GoogleDrivePath parent = path.getParent();

        // possible mkdirs
        // get nearest available attributes and path to mkdirs
        GoogleDrivePath current = parent;
        LinkedList<String> dirsToMk = new LinkedList<>();
        FileMetadata parentAttr = null;
        while (parentAttr == null) {
            try {
                parentAttr = readMetadata(current);
            } catch (IOException ex) { // NOSONAR it is expected exception if folder not exists
                dirsToMk.add(0, current.getFileName().toString());
            }
            current = current.getParent();
        }

        // mkdirs if need
        for (String name : dirsToMk) {
            File folder = m_helper.createFolder(parentAttr.getDriveId(), parentAttr.getId(), name);
            parentAttr = new FileMetadata(folder);
        }

        // create regular file with content
        String name = pathComponents[pathComponents.length - 1];
        m_helper.createFile(parentAttr.getDriveId(), parentAttr.getId(), name,
                new ByteArrayContent(null, content.getBytes(StandardCharsets.UTF_8)));

        return path;
    }

    private FileMetadata readMetadata(final GoogleDrivePath current) throws IOException {
        return ((GoogleDriveFileAttributes) m_fileSystem.provider().readAttributes(current, BasicFileAttributes.class))
                .getMetadata();
    }

    @Override
    protected void beforeTestCaseInternal() throws IOException {
        m_fileSystem.clearAttributesCache();
        final GoogleDrivePath scratchDir = getTestCaseScratchDir();

        GoogleDrivePath testRoot = scratchDir.getParent();
        FileMetadata testRootMeta;

        if (!m_isWorkingDirCreated) {
            // create
            FileMetadata parentOfRoot = readMetadata(testRoot.getParent());
            if (parentOfRoot.getType() == FileType.FOLDER) {
                testRootMeta = new FileMetadata(m_helper.createFolder(parentOfRoot.getDriveId(),
                        parentOfRoot.getId(), testRoot.getFileName().toString()));
            } else if (parentOfRoot.getType() == FileType.MY_DRIVE || parentOfRoot.getType() == FileType.SHARED_DRIVE) {
                testRootMeta = new FileMetadata(
                        m_helper.createFolder(parentOfRoot.getDriveId(), null,
                                testRoot.getFileName().toString()));
            } else {
                throw new IOException("Unexpected root folder " + testRoot.getParent());
            }

            m_isWorkingDirCreated = true;
        } else {
            testRootMeta = readMetadata(testRoot);
        }

        m_helper.createFolder(testRootMeta.getDriveId(), testRootMeta.getId(),
                scratchDir.getFileName().toString());
    }

    @Override
    protected void afterTestCaseInternal() throws IOException {
        final GoogleDrivePath scratchDir = getTestCaseScratchDir();
        deletePath(scratchDir);
        m_fileSystem.clearAttributesCache();
    }

    /**
     * @param name
     *            drive name.
     * @return drive with given name.
     * @throws IOException
     */
    public Drive createSharedDrive(final String name) throws IOException {
        Drive drive = new Drive();
        drive.setName(name);
        drive.setHidden(false);
        return m_helper.getDriveService().drives().create(UUID.randomUUID().toString(), drive).execute();
    }

    private void deletePath(final GoogleDrivePath path) throws IOException {
        try {
            m_helper.deleteFile(readMetadata(path).getId());
        } catch (IOException ex) {
            LOGGER.error("Failed to delete file: " + path, ex);
        }
    }

    @Override
    public void afterClass() throws IOException {
        final GoogleDrivePath scratchDir = getTestCaseScratchDir();

        if (m_isWorkingDirCreated) {
            try {
                deletePath(scratchDir.getParent());
            } finally {
                m_isWorkingDirCreated = false;
            }
        }
    }
}
