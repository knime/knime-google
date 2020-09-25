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
 *   2020-09-12 (Vyacheslav Soldatov): created
 */
package org.knime.ext.google.filehandling.drive.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.List;

import org.knime.google.api.data.GoogleApiConnection;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.Drive;
import com.google.api.services.drive.model.DriveList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

/**
 * Helper for work with native Google Drive library.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class GoogleDriveHelper {
    static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
    private static final String APPLICATION_NAME = "KNIME Google Drive File System";

    /**
     * Authorization scope for read/create/write/delete Google Drive files
     */
    public static final String DRIVE_AUTH_SCOPE = "https://www.googleapis.com/auth/drive";

    private final com.google.api.services.drive.Drive m_driveService;

    /**
     * @param connection
     *            Google Drive connection.
     */
    public GoogleDriveHelper(final GoogleApiConnection connection) {
        m_driveService = new com.google.api.services.drive.Drive.Builder(GoogleApiConnection.getHttpTransport(),
                GoogleApiConnection.getJsonFactory(), connection.getCredential())
                        .setApplicationName(APPLICATION_NAME).
                        build();
    }

    /**
     * @param driveId
     *            ID of shared drive.
     * @param name
     *            file name.
     * @return found file.
     * @throws IOException
     */
    public File getFileOfDrive(final String driveId, final String name) throws IOException {
        return getFileOfDriveImpl(driveId, name);
    }

    /**
     * @param name
     *            file name.
     * @return found file.
     * @throws IOException
     */
    public File getFileOfMyDrive(final String name) throws IOException {
        return getFileOfDriveImpl(null, name);
    }

    /**
     * @param driveId
     * @param name
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    private File getFileOfDriveImpl(final String driveId, final String name) throws IOException, FileNotFoundException {
        Files.List query = m_driveService.files().list().setQ("name='" + escapeSearchValue(name) + "'")
                .setPageToken(null).setSpaces("drive");
        if (driveId != null) {
            query.setDriveId(driveId);
        }

        FileList result = query.execute();
        List<File> files = result.getFiles();
        if (!files.isEmpty()) {
            return files.get(0);
        }

        throw new NoSuchFileException("Child file " + name + " of drive " + driveId + " not found");
    }

    /**
     * @param parentFolderId
     *            parent folder ID.
     * @param name
     *            child name.
     * @return file with given name and parent.
     * @throws IOException
     */
    public File getFile(final String parentFolderId, final String name) throws IOException {
        Files.List query = m_driveService.files().list()
                .setQ("name='" + escapeSearchValue(name) + "' and '" + parentFolderId + "' in parents")
                .setPageToken(null).setSpaces("drive");

        FileList result = query.execute();
        List<File> files = result.getFiles();
        if (!files.isEmpty()) {
            return files.get(0);
        }

        throw new NoSuchFileException("Child file " + name + " of file " + parentFolderId + " not found");
    }

    /**
     * @param name
     *            drive name.
     * @return drive with given name or null if not found.
     * @throws IOException
     */
    public Drive getDrive(final String name) throws IOException {
        if (GoogleDriveFileSystemProvider.MY_DRIVE.equals(name)) {
            return null;
        }

        DriveList result = m_driveService.drives().list().setQ("name='" + escapeSearchValue(name) + "'")
                .setPageToken(null).setFields("nextPageToken, drives(id, name)").execute();

        List<Drive> drives = result.getDrives();
        if (!drives.isEmpty()) {
            return drives.get(0);
        }

        throw new NoSuchFileException("Driver " + name + " not found");
    }

    /**
     * @param name
     *            string value for add to search.
     * @return escaped string.
     */
    private static String escapeSearchValue(final String name) {
        return name.replace("'", "\\'");
    }

    /**
     * @param id
     *            file ID to delete.
     * @throws IOException
     */
    public void delete(final String id) throws IOException {
        m_driveService.files().delete(id);
    }

    /**
     * @param driveId
     *            shared drive ID.
     * @param name
     *            file name.
     * @param content
     *            file content.
     * @return Google file.
     * @throws IOException
     */
    public File createTopLevelFileInDrive(final String driveId, final String name, final ByteArrayContent content)
            throws IOException {
        File file = new File();
        file.setName(name);
        if (driveId != null) {
            file.setDriveId(driveId);
        }

        return m_driveService.files().create(file, content).execute();
    }

    /**
     * @param driveId
     *            shared drive ID.
     * @param folderId
     *            folder ID.
     * @param name
     *            file name.
     * @param content
     *            file content.
     * @return file with given content.
     * @throws IOException
     */
    public File createFile(final String driveId, final String folderId, final String name,
            final ByteArrayContent content) throws IOException {
        File file = new File();
        file.setName(name);
        file.setDriveId(driveId);
        file.setParents(Collections.singletonList(folderId));

        return m_driveService.files().create(file, content).execute();
    }

    /**
     * @param driveId
     *            drive ID.
     * @param name
     *            folder name.
     * @return Google folder.
     * @throws IOException
     */
    public File createTopLevelFolderInDrive(final String driveId, final String name) throws IOException {
        File file = new File();
        file.setName(name);
        file.setMimeType(MIME_TYPE_FOLDER);

        if (driveId != null) {
            file.setDriveId(driveId);
            file.setParents(Collections.singletonList(driveId));
        }

        return m_driveService.files().create(file).execute();
    }

    /**
     * @param driveId
     *            drive ID.
     * @param parentId
     *            parent folder ID.
     * @param name
     *            folder name.
     * @return Google file.
     * @throws IOException
     */
    public File createFolder(final String driveId, final String parentId, final String name) throws IOException {
        File file = new File();
        file.setName(name);
        file.setMimeType(MIME_TYPE_FOLDER);
        file.setDriveId(driveId);
        file.setParents(Collections.singletonList(parentId));

        return m_driveService.files().create(file).execute();
    }
}
