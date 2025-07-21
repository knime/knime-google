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
 */
package org.knime.google.cloud.storage.filehandler;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.cloud.core.file.CloudRemoteFile;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.knime.google.cloud.storage.util.GoogleCloudStorageConnectionInformation;

import com.google.cloud.BatchResult;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobField;
import com.google.cloud.storage.Storage.BlobGetOption;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageException;

/**
 * Implementation of {@link CloudRemoteFile} for Google Cloud Storage.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
@Deprecated
public class GoogleCSRemoteFile extends CloudRemoteFile<GoogleCSConnection> {

    private static final int MAX_BATCH_SIZE = 800; // max 1000

    private static final int MAX_PAGES = 1000;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GoogleCSRemoteFile.class);

    /**
     * Default constructor.
     *
     * @param uri URI of the storage object
     * @param connectionInformation connection informations to use
     * @param connectionMonitor connection monitor to use
     * @deprecated
     */
    @Deprecated
    public GoogleCSRemoteFile(final URI uri, final GoogleCloudStorageConnectionInformation connectionInformation,
        final ConnectionMonitor<GoogleCSConnection> connectionMonitor) {
        this(uri, connectionInformation, connectionMonitor, null);
    }

    /**
     * Default constructor with cache initialized from given {@link Blob}.
     *
     * @param uri URI of the storage object
     * @param connectionInformation connection informations to use
     * @param connectionMonitor connection monitor to use
     * @param object storage object to initialize cache
     * @deprecated
     */
    @Deprecated
    public GoogleCSRemoteFile(final URI uri, final GoogleCloudStorageConnectionInformation connectionInformation,
        final ConnectionMonitor<GoogleCSConnection> connectionMonitor, final Blob object) {
        super(uri, connectionInformation, connectionMonitor);
        CheckUtils.checkArgumentNotNull(connectionInformation, "Connection Information mus not be null");
        if (object != null) {
            m_exists = true;
            m_containerName = object.getBucket();
            m_blobName = object.getName();
            if (object.getUpdateTimeOffsetDateTime() != null) {
                m_lastModified = object.getUpdateTimeOffsetDateTime().toInstant().toEpochMilli();
            }
            m_size = object.getSize();
        }
    }

    private Storage getClient() throws Exception {
        return getOpenedConnection().getClient();
    }

    private String getProjectId() {
        return ((GoogleCloudStorageConnectionInformation)getConnectionInformation()).getProject();
    }

    @Override
    protected GoogleCSConnection createConnection() {
        return new GoogleCSConnection((GoogleCloudStorageConnectionInformation)getConnectionInformation());
    }

    @Override
    protected boolean doesContainerExist(final String bucketName) throws Exception {
        try {
            return getClient().get(bucketName) != null;
        } catch (final StorageException ex) {
            if (ex.getCode() == 404) {
                return false;
            } else if (ex.getCode() == 403) {
                throw new RemoteFile.AccessControlException(ex);
            } else {
                throw ex;
            }
        }
    }

    @Override
    protected boolean doestBlobExist(final String bucketName, final String objectName) throws Exception {
        try {
            if (objectName.endsWith(DELIMITER)) {
                final var options = new ArrayList<BlobListOption>();
                options.add(BlobListOption.delimiter(DELIMITER));
                options.add(BlobListOption.prefix(objectName));
                options.add(BlobListOption.fields(BlobField.NAME));
                options.add(BlobListOption.pageSize(1));
                final var result = getClient().list(bucketName, options.toArray(BlobListOption[]::new));
                return result.getValues().iterator().hasNext();
            } else {
                return getClient().get(bucketName, objectName, BlobGetOption.fields(BlobField.NAME)) != null;
            }
        } catch (final StorageException ex) {
            if (ex.getCode() == 404) {
                return false;
            } else if (ex.getCode() == 403) {
                throw new RemoteFile.AccessControlException(ex);
            } else {
                throw ex;
            }
        }
    }

    @Override
    protected GoogleCSRemoteFile[] listRootFiles() throws Exception {
        final ArrayList<GoogleCSRemoteFile> files = new ArrayList<>();
        final var options = new ArrayList<BlobListOption>();
        options.add(BlobListOption.fields(BlobField.NAME));

        var result = getClient().list(getProjectId(), options.toArray(BlobListOption[]::new));
        for (var page = 1; page < MAX_PAGES; page++) {
            for (final var blob : result.getValues()) {
                files.add(getRemoteFile(blob.getName(), null, null));
            }
            if (page + 1 == MAX_PAGES) {
                LOGGER.warn("Max pages count (" + MAX_PAGES + ") in bucket listing reached, ignoring other pages.");
            }

            if (StringUtils.isNotBlank(result.getNextPageToken())) {
                options.clear();
                options.add(BlobListOption.fields(BlobField.NAME));
                options.add(BlobListOption.pageToken(result.getNextPageToken()));
                result = getClient().list(getProjectId(), options.toArray(BlobListOption[]::new));
            } else {
                break;
            }
        }

        return files.toArray(new GoogleCSRemoteFile[0]);
    }

    private GoogleCSRemoteFile getRemoteFile(final String bucketName, final String objectName, final Blob obj)
        throws URISyntaxException {
        final String path = createContainerPath(bucketName) + (objectName != null ? objectName : "");
        final URI uri = new URI(getURI().getScheme(), getURI().getUserInfo(), getURI().getHost(), -1, path, null, null);
        return new GoogleCSRemoteFile(uri, (GoogleCloudStorageConnectionInformation)getConnectionInformation(),
            getConnectionMonitor(), obj);
    }

    @Override
    protected GoogleCSRemoteFile[] listDirectoryFiles() throws Exception {
        final String bucketName = getContainerName();
        final ArrayList<GoogleCSRemoteFile> files = new ArrayList<>();
        final var prefix = getBlobName();

        final Function<String, BlobListOption[]> optionsProvider = nextPageToken -> { //NOSONAR
            final var options = new ArrayList<BlobListOption>();
            options.add(BlobListOption.delimiter(DELIMITER));
            options.add(BlobListOption.fields(BlobField.BUCKET, BlobField.NAME,
                BlobField.SIZE, BlobField.UPDATED));
            if (StringUtils.isNotBlank(prefix)) {
                options.add(BlobListOption.prefix(prefix));
            }
            if (StringUtils.isNotBlank(nextPageToken)) {
                options.add(BlobListOption.pageToken(nextPageToken));
            }
            return options.toArray(BlobListOption[]::new);
        };

        var result = getClient().list(bucketName, optionsProvider.apply(null));

        for (var page = 1; page < MAX_PAGES; page++) {
            for (final var blob : result.getValues()) {
                final var name = blob.getName();
                if (!name.equals(prefix)) {
                    if (name.endsWith(DELIMITER)) {
                        files.add(getRemoteFile(bucketName, name, null));
                    } else {
                        files.add(getRemoteFile(bucketName, name, blob));
                    }
                }
            }
            if (page + 1 == MAX_PAGES) {
                LOGGER.warn(
                    "Max pages count (" + MAX_PAGES + ") in directory files listing reached, ignoring other pages.");
            }

            if (!StringUtils.isBlank(result.getNextPageToken())) {
                result = getClient().list(bucketName, optionsProvider.apply(result.getNextPageToken()));
            } else {
                break;
            }
        }
        return files.toArray(new GoogleCSRemoteFile[0]);
    }

    /**
     * Fetch meta data and initialize cache.
     */
    private void fetchMetaData() throws Exception {
        try {
            final var options = new ArrayList<BlobGetOption>();
            options.add(BlobGetOption.fields(BlobField.BUCKET, BlobField.NAME,
                BlobField.SIZE, BlobField.UPDATED));

            final var blob = getClient().get(getContainerName(), getBlobName(), options.toArray(BlobGetOption[]::new));
            m_size = blob.getSize();
            if (blob.getUpdateTimeOffsetDateTime() != null) {
                m_lastModified = blob.getUpdateTimeOffsetDateTime().toInstant().toEpochMilli();
            }
            m_exists = true;

        } catch (final StorageException ex) {
            m_size = m_lastModified = null;
            if (ex.getCode() == 404) {
                m_exists = false;
            } else if (ex.getCode() == 403) {
                m_exists = null;
                throw new RemoteFile.AccessControlException(ex);
            } else {
                m_exists = null;
                throw ex;
            }
        }
    }

    @Override
    protected long getBlobSize() throws Exception {
        if (m_size == null) {
            fetchMetaData();
        }
        return m_size;
    }

    @Override
    protected long getLastModified() throws Exception {
        if (m_lastModified == null) {
            fetchMetaData();
        }
        return m_lastModified;
    }

    @Override
    protected boolean deleteContainer() throws Exception {
        getClient().delete(getContainerName());
        return true;
    }

    @Override
    protected boolean deleteDirectory() throws Exception {
        final var bucket = getContainerName();
        final var batch = getClient().batch();
        final var queue = new LinkedBlockingQueue<GoogleCSRemoteFile>();
        queue.add(this);
        while (!queue.isEmpty()) {
            final GoogleCSRemoteFile file = queue.poll();
            if (file.isDirectory()) {
                queue.addAll(Arrays.asList(file.listDirectoryFiles()));
            }
            batch.delete(bucket, file.getBlobName()).notify(new BatchResult.Callback<Boolean, StorageException>() {

                @Override
                public void success(final Boolean result) {
                    // nothing to do
                }

                @Override
                public void error(final StorageException e) {
                    LOGGER.warn("Failure in batch delete: " + e.getMessage());
                }
            });
        }
        batch.submit();
        return true;
    }

    @Override
    protected boolean deleteBlob() throws Exception {
        getClient().delete(getContainerName(), getBlobName());
        return true;
    }

    @Override
    protected boolean createContainer() throws Exception {
        getClient().create(BucketInfo.of(getContainerName()));
        return true;
    }

    @Override
    protected boolean createDirectory(final String dirName) throws Exception {
        final var blobId = BlobId.of(getContainerName(), getBlobName());
        final var blobInfo = BlobInfo.newBuilder(blobId).build();
        getClient().create(blobInfo, new byte[0]);
        return true;
    }

    @Override
    public InputStream openInputStream() throws Exception {
        final var blob = getClient().get(getContainerName(), getBlobName(),
            BlobGetOption.shouldReturnRawInputStream(true));
        return Channels.newInputStream(blob.reader());
    }

    /**
     * Upload a object using a temporary file.
     */
    @Override
    public OutputStream openOutputStream() throws Exception {
        final File tmpFile = FileUtil.createTempFile("gcs-" + getName(), "");
        resetCache();
        return new UploadOutputStream(getClient(), getContainerName(), getBlobName(), tmpFile);
    }

    /**
     * Generate a signed public URL with expiration time.
     *
     * @param expirationSeconds URL expiration time in seconds from now
     * @return signed URL
     * @throws Exception
     * @deprecated
     */
    @Deprecated
    public String getSignedURL(final long expirationSeconds) throws Exception {
        return getConnection().getSigningURL(expirationSeconds, getContainerName(), getBlobName());
    }

    /**
     * @return Spark compatible cluster URL (gs://<containername>/<path>)
     */
    @Override
    public URI getHadoopFilesystemURI() throws Exception {
        final String scheme = "gs";
        final String container = getContainerName();
        final String blobName = DELIMITER + Optional.ofNullable(getBlobName()).orElseGet(() -> "");
        return new URI(scheme, null, container, -1, blobName, null, null);
    }
}
