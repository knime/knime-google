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
 *   2020-11-23 (Vyacheslav Soldatov): added exponential retry on 'exceeded request rate limit'
 */
package org.knime.ext.google.filehandling.drive.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.knime.google.api.nodes.util.GoogleApiUtil;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Create;
import com.google.api.services.drive.Drive.Files.Update;
import com.google.api.services.drive.model.Drive;
import com.google.api.services.drive.model.DriveList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;

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
     * @param config
     *            connection configuration.
     */
    public GoogleDriveHelper(final GoogleDriveFSConnectionConfig config) {
        m_driveService = new com.google.api.services.drive.Drive.Builder(GoogleApiUtil.getHttpTransport(),
                GoogleApiUtil.getJsonFactory(), req -> initializeRequest(req, config))
                        .setApplicationName(APPLICATION_NAME).build();
    }

    private static void initializeRequest(final HttpRequest req, final GoogleDriveFSConnectionConfig config)
            throws IOException {
        new HttpCredentialsAdapter(config.getCredentials()).initialize(req);

        req.setConnectTimeout((int) config.getConnectionTimeOut().toMillis());
        req.setReadTimeout((int) config.getReadTimeOut().toMillis());
    }

    /**
     * @param driveId
     *            ID of shared drive.
     * @param name
     *            file name.
     * @param additionalName
     *            additional file name to search.
     * @return found file.
     * @throws IOException
     */
    public List<File> getFilesOfDriveByNameOrId(final String driveId, final String name,
            final String additionalName)
            throws IOException {
        return getFilesByNameOrId(driveId, driveId == null ? "root" : driveId, name, additionalName);
    }

    /**
     * @param driveId
     *            drive ID.
     * @param parentId
     *            parent folder ID.
     * @param name
     *            child name.
     * @param fileId
     *            additional file name.
     * @return file with given name and parent.
     * @throws IOException
     */
    public List<File> getFilesByNameOrId(final String driveId, final String parentId, final String name,
            final String fileId)
            throws IOException {
        return doWithRetry(() -> getFilesByNameOrIdImpl(driveId, parentId, name, fileId));
    }

    private List<File> getFilesByNameOrIdImpl(final String driveId, final String parentId, final String name,
            final String fileId) throws IOException {
        StringBuilder searchCriterias = new StringBuilder("trashed = false and ")
                .append(createNameAndIdQueryPart(name, fileId)).append(" and '")
                .append(parentId)
                .append("' in parents ");

        Files.List query = m_driveService.files().list()
                .setQ(searchCriterias.toString())
                .setPageToken(null).setFields("nextPageToken, " + FILES_FIELDS_QUERY_PART)
                .setSpaces("drive");

        if (driveId != null) {
            addDriveIdToQuery(query, driveId);
        }

        FileList result = query.execute();
        List<File> files = result.getFiles();
        if (files.isEmpty()) {
            throw new NoSuchFileException("Child file " + name + " of " + parentId + " not found");
        }

        return files;
    }

    /**
     * @param name
     *            drive name.
     * @param driveId
     *            additional driver name.
     * @return drive with given name or null if not found.
     * @throws IOException
     */
    public List<Drive> getDrives(final String name, final String driveId) throws IOException {
        return doWithRetry(() -> getDrivesImpl(name, driveId));
    }

    private List<Drive> getDrivesImpl(final String name, final String driveId) throws IOException {
        if (GoogleDriveFileSystemProvider.MY_DRIVE.equals(name)) {
            return new LinkedList<>();
        }

        // From Google Docs: Search for specific drives in an organization (need to
        // useDomainAdminAccess)
        // For allow not admins to use it: all drives is listed then is selected one
        // with given name
        List<Drive> drives = new LinkedList<>();
        for (Drive drive : listSharedDrives()) {
            if (drive.getName().equals(name) || (driveId != null && driveId.equals(drive.getId()))) {
                drives.add(drive);
            }
        }

        if (drives.isEmpty()) {
            throw new NoSuchFileException("Driver " + name + " not found");
        }

        return drives;
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
        doWithRetry(() -> {
            deleteFileImpl(id);
            return null;
        });
    }

    private void deleteFileImpl(final String id) throws IOException {
        m_driveService.files().delete(id).setSupportsAllDrives(true).execute();
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
        return createFileOrFolder(driveId, parentId, name, content);
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
        return createFileOrFolder(driveId, parentId, name, null);
    }

    /**
     * @param driveId
     *            shared drive ID.
     * @param parentId
     *            folder ID.
     * @param name
     *            file name.
     * @param content
     *            file content or null if folder created.
     * @return created file.
     * @throws IOException
     */
    private File createFileOrFolder(final String driveId, final String parentId, final String name,
            final AbstractInputStreamContent content) throws IOException {
        return doWithRetry(() -> createFileOrFolderImpl(driveId, parentId, name, content));
    }

    private File createFileOrFolderImpl(final String driveId, final String parentId, final String name,
            final AbstractInputStreamContent content) throws IOException {
        File file = new File();
        file.setName(name);
        file.setDriveId(driveId);

        boolean isDriveFile = driveId != null;
        if (parentId != null) {
            file.setParents(Collections.singletonList(parentId));
        } else if (isDriveFile) { // NOSONAR it is correct
            // if drive ID is null not need to specify a parent
            file.setParents(Collections.singletonList(driveId));
        }

        Create query;
        if (content != null) {
            // create file
            query = m_driveService.files().create(file, content);
        } else {
            // create folder
            file.setMimeType(MIME_TYPE_FOLDER);
            query = m_driveService.files().create(file);
        }

        query.setFields(FILE_FIELDS);
        if (isDriveFile) {
            query.setSupportsAllDrives(true);
        }

        return query.execute();
    }

    /**
     * @param fileId
     *            file ID.
     * @param in
     *            new file content.
     * @throws IOException
     */
    public void rewriteFile(final String fileId, final AbstractInputStreamContent in) throws IOException {
        doWithRetry(() -> {
            rewriteFileImpl(fileId, in);
            return null;
        });
    }

    private void rewriteFileImpl(final String fileId, final AbstractInputStreamContent in) throws IOException {
        m_driveService.files().update(fileId, null, in).setSupportsAllDrives(true).execute();
    }

    /**
     * @return list of shared drives.
     * @throws IOException
     */
    public List<Drive> listSharedDrives() throws IOException {
        return doWithRetry(this::listSharedDrivesImpl);
    }

    private List<Drive> listSharedDrivesImpl() throws IOException {
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
        return doWithRetry(() -> listParentImpl(driveId, parentId));
    }

    private List<File> listParentImpl(final String driveId, final String parentId) throws IOException {
        final Files.List query = m_driveService.files().list()
                .setQ("trashed = false and '" + parentId + "' in parents")
                .setFields("nextPageToken, " + FILES_FIELDS_QUERY_PART)
                .setSpaces("drive");
        if (driveId != null) {
            addDriveIdToQuery(query, driveId);
        }

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

    /**
     * @param query
     * @param driveId
     */
    private static void addDriveIdToQuery(final Files.List query, final String driveId) {
        query.setDriveId(driveId);
        query.setIncludeItemsFromAllDrives(true);
        query.setCorpora("drive");
        query.setSupportsAllDrives(true);
    }

    /**
     * @param id
     *            file ID.
     * @return file input stream.
     * @throws IOException
     */
    public InputStream readFile(final String id) throws IOException {
        return doWithRetry(() -> readFileImpl(id));
    }

    private InputStream readFileImpl(final String id) throws IOException {
        return m_driveService.files().get(id).setSupportsAllDrives(true).executeMediaAsInputStream();
    }

    /**
     * @param sourceId
     *            source file ID.
     * @param newParentId
     *            ID of new parent of moving file.
     * @param newName
     *            new file name. Can be equals by previous.
     * @return moved file.
     * @throws IOException
     */
    public File move(final String sourceId, final String newParentId, final String newName) throws IOException {
        return doWithRetry(() -> moveImpl(sourceId, newParentId, newName));
    }

    private File moveImpl(final String sourceId, final String newParentId, final String newName) throws IOException {
        final File oldFile = m_driveService.files().get(sourceId).setFields("id, name, mimeType, parents")
                .setSupportsAllDrives(true).execute();

        final File file = new File();
        file.setName(newName);
        file.setMimeType(oldFile.getMimeType());

        final Update req = m_driveService.files().update(sourceId, file);
        if (newParentId != null) { // may be null in case of default drive.
            req.setAddParents(newParentId);
        }
        if (oldFile.getParents() != null) { // remove all old parents
            req.setRemoveParents(String.join(",", oldFile.getParents()));
        }

        return req.setFields(FILE_FIELDS).setSupportsAllDrives(true).execute();
    }
    /**
     * @param sourceId
     *            source file ID.
     * @param newParentId
     *            ID of new parent of moving file.
     * @param targetName
     *            name of target file.
     * @throws IOException
     */
    public void copy(final String sourceId, final String newParentId, final String targetName)
            throws IOException {
        doWithRetry(() -> {
            copyImpl(sourceId, newParentId, targetName);
            return null;
        });
    }

    private void copyImpl(final String sourceId, final String newParentId, final String targetName) throws IOException {
        final File file = new File();
        file.setName(targetName);

        if (newParentId != null) {
            List<String> parents = new LinkedList<>();
            parents.add(newParentId);
            file.setParents(parents);
        }

        m_driveService.files().copy(sourceId, file).setFields("mimeType").setSupportsAllDrives(true).execute();
    }

    private static String createNameAndIdQueryPart(final String name, final String additionalName) {
        StringBuilder sb = new StringBuilder();
        sb.append("name='").append(escapeSearchValue(name)).append("'");
        if (additionalName != null) {
            sb.insert(0, "(");
            sb.append(" or id='").append(escapeSearchValue(additionalName)).append("'");
            sb.append(")");
        }
        return sb.toString();
    }

    /**
     * @return drive service
     */
    public com.google.api.services.drive.Drive getDriveService() {
        return m_driveService;
    }

    private static <R> R doWithRetry(final IoRetryable<R> retryable) throws IOException {
        return RetryHelper.doWithRetryable(retryable);
    }
}
