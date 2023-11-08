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
package org.knime.ext.google.filehandling.cloudstorage.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.NodeLogger;
import org.knime.google.api.nodes.util.GoogleApiUtil;

import com.google.api.client.http.HttpStatusCodes;
import com.google.api.gax.paging.Page;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobGetOption;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.Storage.BucketListOption;
import com.google.cloud.storage.Storage.CopyRequest;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;

/**
 * Class for keeping {@link Storage} instance alongside with project id. Wraps
 * {@link Storage} api call into more convenient methods.
 *
 * @author Alexander Bondaletov
 */
public class CloudStorageClient {
    @SuppressWarnings("unused")
    private static final NodeLogger LOGGER = NodeLogger.getLogger(CloudStorageClient.class);

    private static final int BUFFER_SIZE = 1024 * 1024 * 5; // 5 MiB

    private final Storage m_storage;

    /**
     * Constructs new instance for a given configuration (derived from host property
     * or the URI)
     *
     * @param config
     *            Connection configuration
     */
    public CloudStorageClient(final CloudStorageConnectionConfig config) {
        final var transportOptions = HttpTransportOptions.newBuilder()//
                .setHttpTransportFactory(GoogleApiUtil::getHttpTransport)
                .setConnectTimeout((int) config.getConnectionTimeOut().toMillis())
                .setReadTimeout((int) config.getReadTimeOut().toMillis()).build();

        m_storage = StorageOptions.newBuilder()
                .setProjectId(config.getProjectId())
                .setTransportOptions(transportOptions)
                .setCredentials(config.getCredentials())
                .build().getService();
    }

    /**
     * @param pageToken
     *            continuation token.
     * @return page of buckets
     * @throws IOException
     */
    public Page<Bucket> listBuckets(final String pageToken) throws IOException {
        final var options = new ArrayList<BucketListOption>();
        if (StringUtils.isNotBlank(pageToken)) {
            options.add(BucketListOption.pageToken(pageToken));
        }
        return handleAccessDenied(() -> m_storage.list(options.toArray(BucketListOption[]::new)));
    }

    /**
     * Returns list of blobs and prefixes for a given bucket and a given prefix.
     * Only items that are 'direct children' of the given directory (represented by
     * the prefix or the bucket name if prefix is null).
     *
     * @param bucket
     *            bucket name.
     * @param prefix
     *            (Optional) Separator-terminated blob name prefix
     * @param pageToken
     *            (Optional) Continuation token
     * @return Page of {@link Blob} instance.
     * @throws IOException
     */
    public Page<Blob> listBlobs(final String bucket, final String prefix, final String pageToken) throws IOException {
        final var options = new ArrayList<BlobListOption>();
        options.add(BlobListOption.delimiter(CloudStorageFileSystem.PATH_SEPARATOR));
        if (StringUtils.isNotBlank(prefix)) {
            options.add(BlobListOption.prefix(prefix));
        }
        if (StringUtils.isNotBlank(pageToken)) {
            options.add(BlobListOption.pageToken(pageToken));
        }
        return handleAccessDenied(() -> m_storage.list(bucket, options.toArray(BlobListOption[]::new)));
    }

    /**
     * List all blobs in the given bucket whose name is starts with a given prefix.
     * Separator is not used, meaning all the nested blobs will be returned.
     *
     * @param bucket
     *            the bucket name.
     * @param prefix
     *            (Optional) Separator-terminated blob name prefix
     * @return list of blobs
     * @throws IOException
     */
    public List<Blob> listAllBlobs(final String bucket, final String prefix) throws IOException {
        final var options = new ArrayList<BlobListOption>();
        if (StringUtils.isNotBlank(prefix)) {
            options.add(BlobListOption.prefix(prefix));
        }
        return handleAccessDenied(() -> {
            var resp = m_storage.list(bucket, options.toArray(BlobListOption[]::new));
            return resp.streamAll().collect(Collectors.toList());
        });
    }

    /**
     * Checks if the given bucket exists and if the blob with a given prefix exists
     * (when provided).
     *
     * @param bucket
     *            The bucket name.
     * @param prefix
     *            (Optional) Separator-terminated blob name prefix
     * @return true when the given buckets exists and blob with the given prefix
     *         exists or no prefix provided.
     * @throws IOException
     */
    public boolean exists(final String bucket, final String prefix) throws IOException {
        try {
            final var options = new ArrayList<BlobListOption>();
            options.add(BlobListOption.delimiter(CloudStorageFileSystem.PATH_SEPARATOR));
            options.add(BlobListOption.pageSize(1));
            if (StringUtils.isNotBlank(prefix)) {
                options.add(BlobListOption.prefix(prefix));
            }
            return handleAccessDenied(() -> {
                final var result = m_storage.list(bucket, options.toArray(BlobListOption[]::new));
                return StringUtils.isBlank(prefix) || (result.getValues().iterator().hasNext());
            });
        } catch (StorageException e) {
            if (e.getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
                // bucket does not exists
                return false;
            }
            throw e;
        }
    }

    /**
     *
     * @param bucket
     *            The bucket name.
     * @param prefix
     *            (Optional) Separator-terminated prefix representing a directory.
     * @return <code>true</code> if the directory represented by the bucket name and
     *         prefix exists and not empty. Returns <code>false</code> otherwise.
     * @throws IOException
     */
    public boolean isNotEmpty(final String bucket, final String prefix) throws IOException {
        try {
            final var options = new ArrayList<BlobListOption>();
            options.add(BlobListOption.delimiter(CloudStorageFileSystem.PATH_SEPARATOR));
            options.add(BlobListOption.pageSize(2));
            if (StringUtils.isNotBlank(prefix)) {
                options.add(BlobListOption.prefix(prefix));
            }

            return handleAccessDenied(() -> {
                final var result = m_storage.list(bucket, options.toArray(BlobListOption[]::new));
                return result.streamValues().anyMatch(b -> !b.getName().equals(prefix));
            });
        } catch (StorageException e) {
            if (e.getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND && StringUtils.isBlank(prefix)) {
                return false;
            }
            throw e;
        }
    }

    /**
     * @param bucket
     *            the bucket name
     * @return the {@link Bucket} blob.
     * @throws IOException
     */
    public Bucket getBucket(final String bucket) throws IOException {
        return handleAccessDenied(() -> m_storage.get(bucket));
    }

    /**
     * @param bucket
     *            the bucket name.
     * @param blobName
     *            the blob name.
     * @return the {@link Blob}.
     * @throws IOException
     */
    public Blob getBlob(final String bucket, final String blobName) throws IOException {
        try {
            return handleAccessDenied(() -> m_storage.get(bucket, blobName));
        } catch (StorageException ex) {
            if (ex.getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
                return null;
            }
            throw ex;
        }
    }

    /**
     * Returns the {@link InputStream} for a given blob data.
     *
     * @param bucket
     *            Bucket name.
     * @param blobName
     *            Blob name.
     * @return Input stream.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public InputStream getInputStream(final String bucket, final String blobName) throws IOException {
        return handleAccessDenied(() -> {
            final var blob = m_storage.get(bucket, blobName, BlobGetOption.shouldReturnRawInputStream(true));
            return Channels.newInputStream(blob.reader());
        });
    }

    /**
     * Creates new buckets with a given name.
     *
     * @param bucket
     *            The bucket name.
     * @throws IOException
     */
    public void insertBucket(final String bucket) throws IOException {
        handleAccessDenied(() -> m_storage.create(BucketInfo.of(bucket)));
    }

    /**
     * Uploads the file to storage.
     *
     * @param bucket
     *            Target bucket name.
     * @param blobName
     *            Target blob name.
     * @param file
     *            File to upload.
     * @throws IOException
     */
    public void insertBlob(final String bucket, final String blobName, final Path file) throws IOException {
        final var blobInfo = buildBlobInfo(bucket, blobName);
        handleAccessDenied(() -> {
            insertBlob(blobInfo, file.toFile());
            return true;
        });
    }

    private static BlobInfo buildBlobInfo(final String bucket, final String blobName) {
        final var blobId = BlobId.of(bucket, blobName);
        return BlobInfo.newBuilder(blobId).build();
    }

    private void insertBlob(final BlobInfo blobInfo, final File file) throws IOException {
        try (final var out = m_storage.writer(blobInfo);
                final var in = new FileInputStream(file).getChannel()) {
            final var buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            while (in.read(buffer) > 0) {
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
        }
    }

    /**
     * Creates an blob with the provided content.
     *
     * @param bucket
     *            Target bucket name.
     * @param blobName
     *            Target blob name.
     * @param content
     *            Target blob content.
     * @throws IOException
     */
    public void insertBlob(final String bucket, final String blobName, final String content) throws IOException {
        final var blobInfo = buildBlobInfo(bucket, blobName);
        handleAccessDenied(() -> m_storage.create(blobInfo, content.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Deletes empty bucket.
     *
     * @param bucket
     *            The bucket name.
     * @throws IOException
     */
    public void deleteBucket(final String bucket) throws IOException {
        handleAccessDenied(() -> m_storage.delete(bucket));
    }

    /**
     * Deletes the given blob.
     *
     * @param bucket
     *            The bucket name.
     * @param blobName
     *            The blob name.
     * @throws IOException
     */
    public void deleteBlob(final String bucket, final String blobName) throws IOException {
        handleAccessDenied(() -> m_storage.delete(bucket, blobName));
    }

    /**
     * Performs a copy from a source blob to a destination blob.
     *
     * @param srcBucket
     *            Source bucket name.
     * @param srcBlobName
     *            Source blob name.
     * @param dstBucket
     *            Destination bucket name.
     * @param dstBlobName
     *            Destination blob name.
     * @throws IOException
     */
    public void copyBlob(final String srcBucket, final String srcBlobName, final String dstBucket,
            final String dstBlobName) throws IOException {
        final var sourceBlobId = BlobId.of(srcBucket, srcBlobName);
        final var destBlobId = BlobId.of(dstBucket, dstBlobName);
        final var request = CopyRequest.newBuilder().setSource(sourceBlobId).setTarget(destBlobId).build();
        handleAccessDenied(() -> {
            final var copyWriter = m_storage.copy(request);
            while (!copyWriter.isDone()) {
                copyWriter.copyChunk();
            }
            return true;
        });
    }

    private static <T> T handleAccessDenied(final IOSupplier<T> r) throws IOException {
        try {
            return r.getWithException();
        } catch (StorageException ex) {
            if (ex.getCode() == HttpStatusCodes.STATUS_CODE_FORBIDDEN) {
                final var ade = new AccessDeniedException(ex.getMessage());
                ade.initCause(ex);
                throw ade;
            }
            throw ex;
        }
    }

    private interface IOSupplier<T> {
        abstract T getWithException() throws IOException;
    }
}
