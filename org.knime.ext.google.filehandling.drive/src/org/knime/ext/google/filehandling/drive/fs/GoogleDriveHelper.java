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

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.knime.google.api.data.GoogleApiConnection;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.Drive;
import com.google.api.services.drive.model.DriveList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

/**
 * Helper for work with native Google Drive API library.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
public class GoogleDriveHelper {
    /**
     * List of fields which should be retrieved from service while receiving list
     * files.
     */
    private static final String FILE_FIELDS = "id, name, mimeType, driveId, modifiedTime, createdTime, size";
    /**
     * Google API query part for describe the list of file properties which should
     * be sent by server on the list files request.
     */
    private static final String FILES_FIELDS_QUERY_PART = "files(" + FILE_FIELDS + ")";
    /**
     * Google API application name.
     */
    private static final String APPLICATION_NAME = "KNIME Google Drive File System";
    /**
     * Mime type of folder.
     */
    public static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";

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
                        .setApplicationName(APPLICATION_NAME)
                        .build();
    }

    /**
     * For unit tests only
     */
    protected GoogleDriveHelper() {
        m_driveService = null;
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
        return getFile(driveId, driveId == null ? "root" : driveId, name);
    }

    /**
     * @param driveId
     *            drive ID.
     * @param parentId
     *            parent folder ID.
     * @param name
     *            child name.
     * @return file with given name and parent.
     * @throws IOException
     */
    public File getFile(final String driveId, final String parentId, final String name) throws IOException {
        StringBuilder searchCriterias = new StringBuilder("trashed = false and name='").append(escapeSearchValue(name))
                .append("' and '").append(parentId).append("' in parents ");

        Files.List query = m_driveService.files().list()
                .setQ(searchCriterias.toString())
                .setPageToken(null).setFields(FILES_FIELDS_QUERY_PART)
                .setSpaces("drive");
        query.setDriveId(driveId);

        FileList result = query.execute();
        List<File> files = result.getFiles();
        if (!files.isEmpty()) {
            return files.get(0);
        }

        throw new NoSuchFileException("Child file " + name + " of " + parentId + " not found");
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

        DriveList result = m_driveService.drives().list()
                .setQ("name='" + escapeSearchValue(name) + "'")
                .setPageToken(null).setFields("nextPageToken, drives(id, name, createdTime)").execute();

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
    public void deleteFile(final String id) throws IOException {
        m_driveService.files().delete(id).execute();
    }

    /**
     * @param id
     *            drive ID.
     * @throws IOException
     */
    public void deleteDrive(final String id) throws IOException {
        m_driveService.drives().delete(id).execute();
    }

    /**
     * @param driveId
     *            shared drive ID.
     * @param parentId
     *            folder ID.
     * @param name
     *            file name.
     * @param content
     *            file content.
     * @return created file.
     * @throws IOException
     */
    public File createFile(final String driveId, final String parentId, final String name,
            final AbstractInputStreamContent content) throws IOException {
        File file = new File();
        file.setName(name);
        file.setDriveId(driveId);
        if (parentId != null) {
            file.setParents(Collections.singletonList(parentId));
        }

        return m_driveService.files().create(file, content).setFields(FILE_FIELDS).execute();
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
        if (parentId != null) {
            file.setParents(Collections.singletonList(parentId));
        }

        return m_driveService.files().create(file).setFields(FILE_FIELDS).execute();
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
        return m_driveService.drives().create(UUID.randomUUID().toString(), drive).execute();
    }

    /**
     * @return list of shared drives.
     * @throws IOException
     */
    public List<Drive> listSharedDrives() throws IOException {
        String pageToken = null;
        com.google.api.services.drive.Drive.Drives.List query = m_driveService.drives().list()
                .setFields("nextPageToken, drives(id, name, createdTime)");

        List<Drive> drives = new LinkedList<>();
        do {
            DriveList result = query.setPageToken(pageToken).execute();
            drives.addAll(result.getDrives());

            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return drives;
    }

    /**
     * @param driveId
     *            drive ID or null in case of 'My Drive'
     * @return files files.
     * @throws IOException
     */
    public List<File> listDrive(final String driveId) throws IOException {
        return listParent(driveId, driveId == null ? "root" : driveId);
    }
    /**
     * @param driveId
     *            drive ID or null in case of 'My Drive'
     * @param parentId
     *            parent ID.
     * @return files files.
     * @throws IOException
     */
    public List<File> listFolder(final String driveId, final String parentId) throws IOException {
        return listParent(driveId, parentId);
    }

    /**
     * @param driveId
     *            drive ID or null in case of 'My Drive'
     * @param parentId
     *            parent ID.
     * @return files files.
     * @throws IOException
     */
    private List<File> listParent(final String driveId, final String parentId) throws IOException {
        final Files.List query = m_driveService.files().list().setDriveId(driveId)
                .setQ("trashed = false and '" + parentId + "' in parents")
                .setFields(FILES_FIELDS_QUERY_PART)
                .setSpaces("drive");

        final List<File> files = new LinkedList<>();
        String nextPageToken = null;
        do {
            query.setPageToken(nextPageToken);

            FileList result = query.execute();
            nextPageToken = result.getNextPageToken();

            for (File file : result.getFiles()) {
                files.add(file);
            }
        } while (nextPageToken != null);

        return files;
    }
}
