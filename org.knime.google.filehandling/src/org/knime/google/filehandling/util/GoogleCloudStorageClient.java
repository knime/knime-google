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
package org.knime.google.filehandling.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import org.knime.core.node.NodeLogger;
import org.knime.google.api.data.GoogleApiConnection;
import org.knime.google.filehandling.connections.GoogleCloudStorageFileSystem;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.Storage.Objects.Rewrite;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.RewriteResponse;
import com.google.api.services.storage.model.StorageObject;

/**
 * Class for keeping {@link Storage} instance alongside with project id. Wraps
 * {@link Storage} api call into more convenient methods.
 *
 * @author Alexander Bondaletov
 */
public class GoogleCloudStorageClient {
    @SuppressWarnings("unused")
    private static final NodeLogger LOGGER = NodeLogger.getLogger(GoogleCloudStorageClient.class);
    private static final String APP_NAME = "KNIME-Google-Cloud-Storage-Connector";

    private final String m_projectId;
    private final Storage m_storage;

    /**
     * Constructs new instance for a given api connection and project id (derived
     * from host property or the URI)
     *
     * @param apiConnection
     *            google api connection.
     * @param uri
     *            file system URI.
     */
    public GoogleCloudStorageClient(final GoogleApiConnection apiConnection, final URI uri) {
        this.m_projectId = uri.getHost();
        m_storage = new Storage.Builder(GoogleApiConnection.getHttpTransport(), GoogleApiConnection.getJsonFactory(),
                apiConnection.getCredential()).setApplicationName(APP_NAME).build();
    }

    /**
     * @param pageToken
     *            continuation token.
     * @return list of buckets
     * @throws IOException
     */
    public Buckets listBuckets(final String pageToken) throws IOException {
        Storage.Buckets.List req = m_storage.buckets().list(m_projectId);
        if (pageToken != null) {
            req.setPageToken(pageToken);
        }
        return req.execute();
    }

    /**
     * Returns list of objects and prefixes for a given bucket and a given prefix.
     * Only items that are 'direct children' of the given directory (represented by
     * the prefix or the bucket name if prefix is null).
     *
     * @param bucket
     *            bucket name.
     * @param prefix
     *            (Optional) Separator-terminated object name prefix
     * @param pageToken
     *            (Optional) Continuation token
     * @return {@link Objects} instance.
     * @throws IOException
     */
    public Objects listObjects(final String bucket, final String prefix, final String pageToken) throws IOException {
        Storage.Objects.List req = m_storage.objects().list(bucket)
                .setDelimiter(GoogleCloudStorageFileSystem.PATH_SEPARATOR);

        if (prefix != null && !prefix.isEmpty()) {
            req.setPrefix(prefix);
        }

        if (pageToken != null) {
            req.setPageToken(pageToken);
        }

        return req.execute();
    }

    /**
     * List all objects in the given bucket whose name is starts with a given
     * prefix. Separator is not used, meaning all the nested object will be
     * returned.
     *
     * @param bucket
     *            the bucket name.
     * @param prefix
     *            (Optional) Separator-terminated object name prefix
     * @return list of objects
     * @throws IOException
     */
    public List<StorageObject> listAllObjects(final String bucket, final String prefix) throws IOException {
        Storage.Objects.List req = m_storage.objects().list(bucket);
        if (prefix != null && !prefix.isEmpty()) {
            req.setPrefix(prefix);
        }

        Objects resp = req.execute();
        List<StorageObject> result = resp.getItems();

        while (resp.getNextPageToken() != null) {
            resp = req.setPageToken(resp.getNextPageToken()).execute();
            result.addAll(resp.getItems());
        }

        return result;
    }

    /**
     * Checks if the given bucket exists and if the object with a given prefix
     * exists (when provided).
     *
     * @param bucket
     *            The bucket name.
     * @param prefix
     *            (Optional) Separator-terminated object name prefix
     * @return true when the given buckets exists and object with the given prefix
     *         exists or no prefix provided.
     * @throws IOException
     */
    public boolean exists(final String bucket, final String prefix) throws IOException {
        try {
            Objects objects = m_storage.objects().list(bucket).setDelimiter(GoogleCloudStorageFileSystem.PATH_SEPARATOR)
                    .setPrefix(prefix).setMaxResults(1L).execute();

            return prefix == null || (objects.getItems() != null && !objects.getItems().isEmpty())
                    || (objects.getPrefixes() != null && !objects.getPrefixes().isEmpty());
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
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
            Objects objects = m_storage.objects().list(bucket).setDelimiter(GoogleCloudStorageFileSystem.PATH_SEPARATOR)
                    .setPrefix(prefix).setMaxResults(2L).execute();

            if (objects.getPrefixes() != null && !objects.getPrefixes().isEmpty()) {
                return true;
            }

            if (objects.getItems() != null) {
                return objects.getItems().stream().anyMatch(o -> !o.getName().equals(prefix));
            }

        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND && prefix == null) {
                return false;
            }
            throw e;
        }
        return false;
    }

    /**
     * @param bucket
     *            the bucket name
     * @return the {@link Bucket} object.
     * @throws IOException
     */
    public Bucket getBucket(final String bucket) throws IOException {
        return m_storage.buckets().get(bucket).execute();
    }

    /**
     * @param bucket
     *            the bucket name.
     * @param object
     *            the object name.
     * @return the {@link StorageObject}.
     * @throws IOException
     */
    public StorageObject getObject(final String bucket, final String object) throws IOException {
        try {
            return m_storage.objects().get(bucket, object).execute();
        } catch (GoogleJsonResponseException ex) {
            if (ex.getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
                return null;
            }
            throw ex;
        }
    }

    /**
     * Returns the {@link InputStream} for a given object data.
     *
     * @param bucket
     *            Bucket name.
     * @param object
     *            Object name.
     * @return Input stream.
     * @throws IOException
     */
    public InputStream getObjectStream(final String bucket, final String object) throws IOException {
        return m_storage.objects().get(bucket, object).setAlt("media").executeAsInputStream();
    }

    /**
     * Creates new buckets with a given name.
     *
     * @param bucket
     *            The bucket name.
     * @throws IOException
     */
    public void insertBucket(final String bucket) throws IOException {
        Bucket b = new Bucket().setName(bucket);
        m_storage.buckets().insert(m_projectId, b).execute();
    }

    /**
     * Uploads the file to storage.
     *
     * @param bucket
     *            Target bucket name.
     * @param object
     *            Target object name.
     * @param file
     *            File to upload.
     * @throws IOException
     */
    public void insertObject(final String bucket, final String object, final Path file) throws IOException {
        insertObject(bucket, object, new FileContent(null, file.toFile()));
    }

    /**
     * Creates an object with the provided content.
     *
     * @param bucket
     *            Target bucket name.
     * @param object
     *            Target object name.
     * @param content
     *            Target object content.
     * @throws IOException
     */
    public void insertObject(final String bucket, final String object, final String content) throws IOException {
        insertObject(bucket, object, new InputStreamContent(null, new ByteArrayInputStream(content.getBytes())));
    }

    private void insertObject(final String bucket, final String object, final AbstractInputStreamContent content)
            throws IOException {
        StorageObject obj = new StorageObject().setName(object);
        m_storage.objects().insert(bucket, obj, content).execute();
    }

    /**
     * Deletes empty bucket.
     *
     * @param bucket
     *            The bucket name.
     * @throws IOException
     */
    public void deleteBucket(final String bucket) throws IOException {
        m_storage.buckets().delete(bucket).execute();
    }

    /**
     * Deletes the given object.
     *
     * @param bucket
     *            The bucket name.
     * @param object
     *            The object name.
     * @throws IOException
     */
    public void deleteObject(final String bucket, final String object) throws IOException {
        m_storage.objects().delete(bucket, object).execute();
    }

    /**
     * Performs a copy from a source object to a destination object.
     *
     * @param srcBucket
     *            Source bucket name.
     * @param srcObject
     *            Source object name.
     * @param dstBucket
     *            Destination bucket name.
     * @param dstObject
     *            Destination object name.
     * @throws IOException
     */
    public void rewriteObject(final String srcBucket, final String srcObject, final String dstBucket,
            final String dstObject) throws IOException {
        Rewrite rewrite = m_storage.objects().rewrite(srcBucket, srcObject, dstBucket, dstObject, new StorageObject());
        RewriteResponse response = rewrite.execute();
        while (!Boolean.TRUE.equals(response.getDone())) {
            response = rewrite.setRewriteToken(response.getRewriteToken()).execute();
        }
    }

}
