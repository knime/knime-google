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
 *   Jun 12, 2018 (jtyler): created
 */
package org.knime.google.api.drive.filehandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.cloud.core.file.CloudRemoteFile;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.google.api.drive.util.GoogleDriveConnectionInformation;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.TeamDrive;
import com.google.api.services.drive.model.TeamDriveList;

/**
 * Implementation of {@link CloudRemoteFile} for Google Drive
 *
 * For Google Drive Containers either the Users Google Drive (called MyDrive) or team drives, A team drive called
 * exampleTeamDrive would be on the path /TeamDrvies/exampleTeamDrive, and would be represented by a container
 * internally.
 *
 * @author jtyler
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class GoogleDriveRemoteFile extends CloudRemoteFile<GoogleDriveConnection> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GoogleDriveRemoteFile.class);

    private static final String MY_DRIVE = "MyDrive";

    private static final String TEAM_DRIVES = "TeamDrives";

    private static final String DEFAULT_CONTAINER = "/" + MY_DRIVE + "/";

    private static final String TEAM_DRIVES_FOLDER = "/" + TEAM_DRIVES + "/";

    private static final String GOOGLE_MIME_TYPE = "application/vnd.google-apps";

    private static final String FOLDER = GOOGLE_MIME_TYPE + ".folder";

    private static final String FIELD_STRING = "files(id, name, kind, mimeType, modifiedTime, size, trashed, parents)";

    private GoogleDriveRemoteFileMetadata m_fileMetadata;

    /**
     * @param uri
     * @param connectionInformation
     * @param connectionMonitor
     * @throws Exception
     */
    protected GoogleDriveRemoteFile(final URI uri, final GoogleDriveConnectionInformation connectionInformation,
        final ConnectionMonitor<GoogleDriveConnection> connectionMonitor) throws Exception {
        super(uri, connectionInformation, connectionMonitor);
        m_fileMetadata = new GoogleDriveRemoteFileMetadata(uri);
        if (m_fileMetadata.getFileId() == null || m_fileMetadata.getFileId().isEmpty()) {
            // No metadata found in URI (Query params missing). Determine it via API
            m_fileMetadata = getMetadata();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBlobName() throws Exception {
        if (m_blobName == null) {
            if (isContainer()) {
                return null;
            }
            if (getContainerName().equals(MY_DRIVE)) {
                // Fix filenames with spaces in them.
                m_blobName = URLDecoder.decode(getFullPath().substring(DEFAULT_CONTAINER.length()), "UTF-8");
            } else {
                final int idx = StringUtils.ordinalIndexOf(getFullPath(), "/", 3);
                m_blobName = getFullPath().substring(idx + 1);
            }
        }
        return m_blobName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doesContainerExist(final String containerName) throws Exception {
        if (containerName.equals(MY_DRIVE) || containerName.equals(TEAM_DRIVES)) {
            return true;
        } else {
            try {
                // Return true if this is a valid Team Drive
                getTeamId(containerName);
                return true;
            } catch (final NoSuchElementException ex) {
                return false;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isContainer() throws Exception {
        if (m_isContainer == null) {
            if (getFullPath().equals(DEFAULT_CONTAINER) || getFullPath().equals(TEAM_DRIVES_FOLDER)) {
                m_isContainer = true;
            } else {
                final String[] elements = getFullPath().split("/");
                if (elements.length == 3 && elements[0].isEmpty() && elements[1].equals(TEAM_DRIVES)) {
                    m_isContainer = true;
                } else {
                    m_isContainer = false;
                }
            }
        }
        return m_isContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean doestBlobExist(final String containerName, final String blobName) throws Exception {
        // This can get called to verify a file was deleted successfully.
        // So lets reset the metadata
        if (m_fileMetadata == null) {
            m_fileMetadata = getMetadata();
        }
        if (m_fileMetadata.getFileId() != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected GoogleDriveRemoteFile[] listRootFiles() throws Exception {

        final GoogleDriveRemoteFile[] rootFiles = new GoogleDriveRemoteFile[2];

        // Create remote file for My Drive
        final URI uriMyDrive = new URI(getURI().getScheme(), getURI().getUserInfo(), getURI().getHost(),
            getURI().getPort(), DEFAULT_CONTAINER, getURI().getQuery(), getURI().getFragment());

        rootFiles[0] = new GoogleDriveRemoteFile(uriMyDrive,
            (GoogleDriveConnectionInformation)getConnectionInformation(), getConnectionMonitor());

        // Create remote file for Team Drives
        final URI uriTeamDrive = new URI(getURI().getScheme(), getURI().getUserInfo(), getURI().getHost(),
            getURI().getPort(), TEAM_DRIVES_FOLDER, getURI().getQuery(), getURI().getFragment());

        rootFiles[1] = new GoogleDriveRemoteFile(uriTeamDrive,
            (GoogleDriveConnectionInformation)getConnectionInformation(), getConnectionMonitor());

        return rootFiles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected GoogleDriveRemoteFile[] listDirectoryFiles() throws Exception {

        LOGGER.debug("Listing directory files for: " + getFullPath());
        final List<GoogleDriveRemoteFile> remoteFileList = new ArrayList<GoogleDriveRemoteFile>();

        String pageToken = null;

        if (getFullPath().equals(TEAM_DRIVES_FOLDER)) {
            do {
                // Loop while we still get a valid nextPageToken.
                // The pageToken is null if there is no next page.
                final TeamDriveList teamDrives = getService().teamdrives().list()
                        .setPageToken(pageToken)
                        .execute();

                for (final TeamDrive teamDrive : teamDrives.getTeamDrives()) {

                    final String name = teamDrive.getName();
                    if (name .contains("/")) {
                        LOGGER.warn("Skipping file because of character: " + getFullPath() + "'" + name + "'");
                        continue;
                    }
                    name.replace("'", "\\'");

                    final GoogleDriveRemoteFileMetadata metadata = new GoogleDriveRemoteFileMetadata();
                    // Use TeamDriveID as File ID for top drive roots
                    metadata.setFileId(teamDrive.getId());
                    metadata.setMimeType(FOLDER);
                    metadata.setTeamId(teamDrive.getId());

                    final URI teamURI = new URI(getURI().getScheme(), getURI().getUserInfo(), getURI().getHost(),
                        getURI().getPort(), TEAM_DRIVES_FOLDER + name + "/", metadata.toQueryString(),
                        getURI().getFragment());
                    LOGGER.debug("Team drive URI: " + teamURI);
                    remoteFileList.add(new GoogleDriveRemoteFile(teamURI,
                        (GoogleDriveConnectionInformation)getConnectionInformation(), getConnectionMonitor()));
                }
                pageToken = teamDrives.getNextPageToken();
            } while (pageToken != null);
        } else {
            do {
                // Loop while we still get a valid nextPageToken.
                // The pageToken is null if there is no next page.
                // Build File list (with support for team drives)
                // If a field is set, all necessary fields have to be set manually.
                com.google.api.services.drive.Drive.Files.List fileRequest = getService().files().list()
                        .setQ("'" + m_fileMetadata.getFileId() + "' in parents and trashed = false")
                        .setSpaces("drive")
                        .setFields("nextPageToken, " + FIELD_STRING)
                        .setPageToken(pageToken)
                        .setOrderBy("name");

                if (m_fileMetadata.fromTeamDrive()) {
                    fileRequest = fileRequest.setCorpora("teamDrive").setSupportsTeamDrives(true).setIncludeTeamDriveItems(true)
                        .setTeamDriveId(m_fileMetadata.getTeamId());
                }

                FileList fileList = fileRequest.execute();


                for (final File file : fileList.getFiles()) {

                    // Google Drive API only allows downloading of non Google file types.
                    // (meaning native Google Docs, Spreadsheets, etc. can not be directly downloaded via
                    // the API). Let's filter those files out, but keep folder references.
                    if (!file.getMimeType().contains(GOOGLE_MIME_TYPE) || file.getMimeType().equals(FOLDER)) {
                        final String folderPostFix = (file.getMimeType().equals(FOLDER)) ? "/" : "";

                        final String name = file.getName();
                        if (name.contains("/" )) {
                            LOGGER.warn("Skipping file because of character: " + getFullPath() + "'" + name + "'");
                            continue;
                        }
                        name.replace("'", "\\'");

                        final GoogleDriveRemoteFileMetadata metadata = new GoogleDriveRemoteFileMetadata();

                        metadata.setFileId(file.getId());
                        metadata.setMimeType(file.getMimeType());
                        if (!file.getMimeType().equals(FOLDER)) {
                            metadata.setFileSize(file.getSize());
                        }
                        metadata.setLastModified(file.getModifiedTime().getValue() / 1000);
                        metadata.setParents(file.getParents());

                        if (m_fileMetadata.fromTeamDrive()) {
                            metadata.setTeamId(m_fileMetadata.getTeamId());
                        }

                        final URI uri = new URI(getURI().getScheme(), getURI().getUserInfo(), getURI().getHost(), getURI().getPort(),
                            getFullPath() + name + folderPostFix, metadata.toQueryString(), getURI().getFragment());


                        LOGGER.debug("Google Drive Remote URI: " + uri.toString());
                        final GoogleDriveRemoteFile remoteFile = new GoogleDriveRemoteFile(uri,
                            (GoogleDriveConnectionInformation)getConnectionInformation(), getConnectionMonitor());
                        remoteFileList.add(remoteFile);
                    }
                }
                pageToken = fileList.getNextPageToken();
            } while (pageToken != null);
        }

        return remoteFileList.toArray(new GoogleDriveRemoteFile[remoteFileList.size()]);
    }

    /**
     * To get an idea of the notion of containers in Google Drive see {@link GoogleDriveRemoteFile}'s documentation.
     */
    @Override
    public String getContainerName() throws Exception {
        if (m_containerName == null) {
            if (getFullPath().equals("/")) {
                m_containerName = null;
            } else if (getFullPath().substring(0, DEFAULT_CONTAINER.length()).equals(DEFAULT_CONTAINER)) {
                m_containerName = MY_DRIVE;
            } else if (getFullPath().equals(TEAM_DRIVES_FOLDER)) {
                m_containerName = TEAM_DRIVES;
            } else {
                final String[] elements = getFullPath().split("/");
                if (elements.length < 3 || (elements[0] != null && !elements[0].isEmpty())) {
                    throw new InvalidSettingsException("Invalid path. Container could not be determined.");
                }
                m_containerName = elements[2];
            }
        }
        return m_containerName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long getBlobSize() throws Exception {
        return m_fileMetadata.getFileSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long getLastModified() throws Exception {
        return m_fileMetadata.getLastModified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean deleteContainer() throws Exception {
        throw new UnsupportedOperationException("Deleting Team Drives not supported.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean deleteDirectory() throws Exception {
        // Google Drive API will delete a folder and any subfolders or files by default
        return deleteBlob();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean deleteBlob() throws Exception {
        CheckUtils.checkState(m_fileMetadata.getFileId() != null, "File '%s' can't be deleted (does it exist)?",
            getFullPath());
        getService().files().delete(m_fileMetadata.getFileId()).setSupportsTeamDrives(true).execute();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean createContainer() throws Exception {
        throw new UnsupportedOperationException("Team Drive creation not supported.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean createDirectory(final String dirName) throws Exception {

        final File fileMetadata = new File();
        fileMetadata.setName(getName());
        fileMetadata.setParents(m_fileMetadata.getParents());
        fileMetadata.setMimeType(FOLDER);

        try {
            final File driveFile = getService().files().create(fileMetadata).setFields("id, parents, modifiedTime")
                .setSupportsTeamDrives(true).execute();

            LOGGER.debug("Creating new folder: " + getBlobName() + " , file id: " + driveFile.getId());

            // Set updated metadata (Parents and team ID have already been set)
            m_fileMetadata.setFileId(driveFile.getId());
            m_fileMetadata.setMimeType(FOLDER);
            m_fileMetadata.setLastModified(driveFile.getModifiedTime().getValue() / 1000);

            return true;
        } catch (final GoogleJsonResponseException ex) {
            throw new Exception(ex.getStatusMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected GoogleDriveConnection createConnection() {
        return new GoogleDriveConnection((GoogleDriveConnectionInformation)getConnectionInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getHadoopFilesystemURI() throws Exception {
        throw new UnsupportedOperationException("Hadoop file system not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream() throws Exception {
        if (!doestBlobExist(getContainerName(), getBlobName())) {
            throw new IOException("\"" + getFullPath() + "\" Does not exist.");
        }
        return getService().files().get(m_fileMetadata.getFileId()).executeMediaAsInputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        throw new UnsupportedOperationException("Output Streams not supported for writing to Google Drive.");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void write(final RemoteFile file, final ExecutionContext exec) throws Exception {
        try (final InputStream in = file.openInputStream()) {

            final File fileMetadata = new File();
            fileMetadata.setName(getName());
            fileMetadata.setParents(m_fileMetadata.getParents());

            // Media type is null. Google will determine the type based on file
            final InputStreamContent content = new InputStreamContent(null, in);

            final File driveFile = getService().files().create(fileMetadata, content)
                .setFields("id, parents, size, mimeType, modifiedTime").setSupportsTeamDrives(true).execute();

            LOGGER.debug("Creating new file: " + getBlobName() + " , file id: " + driveFile.getId());

            // Set updated metadata (Parents and team ID have already been set)
            m_fileMetadata.setFileId(driveFile.getId());
            m_fileMetadata.setMimeType(driveFile.getMimeType());
            m_fileMetadata.setFileSize(driveFile.getSize());
            m_fileMetadata.setLastModified(driveFile.getModifiedTime().getValue() / 1000);
        }
    }

    private String getTeamId(final String teamDriveName) throws Exception {
        if (teamDriveName.equals(MY_DRIVE)) {
            return null;
        }
        if (m_fileMetadata == null) {
            m_fileMetadata = getMetadata();
        }
        return m_fileMetadata.getTeamId();
    }

    private GoogleDriveRemoteFileMetadata getMetadata() throws Exception {

        final GoogleDriveRemoteFileMetadata metadata = new GoogleDriveRemoteFileMetadata();
        String teamId = null;

        // Handle top level folders (MyDrive and TeamDrives)
        if (getFullPath().equals("/")) {
            return metadata;
        }
        if (getFullPath().equals(DEFAULT_CONTAINER) || getFullPath().equals(TEAM_DRIVES_FOLDER)) {
            metadata.setFileId("root");
            return metadata;
        } else {
            final List<TeamDrive> teamDrives = getService().teamdrives().list().execute().getTeamDrives();
            for (final TeamDrive teamDrive : teamDrives) {
                if (teamDrive.getName().equals(getContainerName())) {
                    teamId = teamDrive.getId();
                }
            }
            if (teamId != null) {
                metadata.setTeamId(teamId);
                // Handle Team drive directory roots
                if (getFullPath().equals(TEAM_DRIVES_FOLDER + getContainerName() + "/")) {
                    // The file id for the team drive container is the team drive's id.
                    metadata.setFileId(teamId);
                    return metadata;
                }
            }
        }

        // Set root parent id (id of My Drive or Team Drive)
        String rootId;
        final Files driveFiles = getService().files();
        if (teamId == null) {
            rootId = driveFiles.get("root").setFields("id").execute().getId();
        } else {
            rootId = teamId;
        }

        final String[] pathElementStringArray = getBlobName().split("/");

        // Build Q-string (We only want to return relevant files/folders in the path to cut down on total
        // files returned). Ignore "trashed" files
        String qString = "trashed = false and (";
        for (int i = 0; i < pathElementStringArray.length; i++) {

            qString += "name = '" + pathElementStringArray[i].replace("'", "\\'") + "'";

            if (i < pathElementStringArray.length - 1) {
                qString += " or ";
            }
        }
        qString += ")";

        LOGGER.debug("Q-String: ( " + qString + " )");


        boolean parentsValidated = false;
        String parent = rootId;

        // Retrieve files that match names in qString
        // This could return file/folder name duplicates, so the next check the the parent is also in the path
        String pageToken = null;
        do {
            // Loop while we still get a valid nextPageToken.
            // The pageToken is null if there is no next page.
            // Build File list (with support for team drives)
            com.google.api.services.drive.Drive.Files.List fileRequest = getService().files().list()
                    .setQ(qString)
                    .setSpaces("drive")
                    .setFields("nextPageToken, " + FIELD_STRING)
                    .setPageToken(pageToken);

            if (metadata.fromTeamDrive()) {
                fileRequest = fileRequest.setCorpora("teamDrive").setSupportsTeamDrives(true).setIncludeTeamDriveItems(true)
                        .setTeamDriveId(metadata.getTeamId());
            }

            FileList fileList = fileRequest.execute();

            // Use file IDs and parent information to find the right file id by
            // iterating through each element in the path
            for (int i = 0; i < pathElementStringArray.length; i++) {

                boolean fileFound = false;

                for (final File file : fileList.getFiles()) {
                    // If name matches and has the correct parent
                    if (!file.getMimeType().contains(GOOGLE_MIME_TYPE) || file.getName().equals(pathElementStringArray[i])
                            && (!(file.getParents() != null) || file.getParents().contains(parent))) {
                        fileFound = true;
                        if (i == pathElementStringArray.length - 1) {
                            // Last element, this it the file we want
                            metadata.setFileId(file.getId());
                            metadata.setMimeType(file.getMimeType());
                            if (!file.getMimeType().equals(FOLDER)) {
                                metadata.setFileSize(file.getSize());
                            }
                            metadata.setLastModified(file.getModifiedTime().getValue() / 1000);
                            metadata.addParentId(parent);
                            return metadata;
                        } else {
                            // Haven't made it to end of path yet. Set this file ID as new parent
                            parent = file.getId();
                        }
                    }
                }

                // If the current file was found and we aren't to the end of the blob path,
                // then the parents are still valid.
                // We don't check this for the last file in the array because it could be a
                // new file being created
                if (fileFound) {
                    parentsValidated = true;
                } else if (i < pathElementStringArray.length - 1) {
                    parentsValidated = false;
                    break;
                }
            }
            pageToken = fileList.getNextPageToken();
        } while (pageToken != null);

        // In the case that the blob path only has a length of one, the parents are containers and have
        // already been validated. Override parentsValidated to true
        if (pathElementStringArray.length == 1) {
            parentsValidated = true;
        }

        // Set the last found location in the path as the parent.
        // This is needed during Google Drive file creation to ensure
        // the file is placed in the correct folder (i.e. its parent)
        if (parentsValidated) {
            metadata.addParentId(parent);
            return metadata;
        } else {
            return metadata;
        }
    }

    private Drive getService() throws Exception {
        return getOpenedConnection().getDriveService();
    }

}