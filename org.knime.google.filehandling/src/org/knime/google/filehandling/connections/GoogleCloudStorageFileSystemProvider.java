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
package org.knime.google.filehandling.connections;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.google.api.data.GoogleApiConnection;
import org.knime.google.filehandling.util.GoogleCloudStorageClient;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.StorageObject;

/**
 * File system provider for {@link GoogleCloudStorageFileSystem}.
 *
 * @author Alexander Bondaletov
 */
public class GoogleCloudStorageFileSystemProvider
        extends BaseFileSystemProvider<GoogleCloudStoragePath, GoogleCloudStorageFileSystem> {
    @SuppressWarnings("unused")
    private static final NodeLogger LOGGER = NodeLogger.getLogger(GoogleCloudStorageFileSystemProvider.class);
    /**
     * Google Cloud Storage URI scheme.
     */
    public static final String SCHEME = "gs";

    private final GoogleApiConnection m_apiConnection;
    private final long m_cacheTTL;

    /**
     * Constructs a file system provider for {@link GoogleCloudStorageFileSystem}.
     *
     * @param apiConnection
     *            google api connection.
     * @param cacheTTL
     *            the timeToLive for the attributes cache.
     */
    public GoogleCloudStorageFileSystemProvider(final GoogleApiConnection apiConnection, final long cacheTTL) {
        this.m_apiConnection = apiConnection;
        this.m_cacheTTL = cacheTTL;
    }

    @Override
    protected GoogleCloudStorageFileSystem createFileSystem(final URI uri, final Map<String, ?> env)
            throws IOException {
        return new GoogleCloudStorageFileSystem(this, uri, m_apiConnection, m_cacheTTL);
    }

    @Override
    protected InputStream newInputStreamInternal(final GoogleCloudStoragePath path, final OpenOption... options)
            throws IOException {
        try {
            return path.getFileSystem().getClient().getObjectStream(path.getBucketName(), path.getBlobName());
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
                throw new NoSuchFileException(path.toString());
            }
            throw new IOException(e);
        }
    }

    @Override
    protected OutputStream newOutputStreamInternal(final GoogleCloudStoragePath path, final OpenOption... options)
            throws IOException {
        final Set<OpenOption> opts = new HashSet<>(Arrays.asList(options));
        return Channels.newOutputStream(newByteChannel(path, opts));
    }

    @Override
    protected Iterator<GoogleCloudStoragePath> createPathIterator(final GoogleCloudStoragePath dir,
            final Filter<? super Path> filter) throws IOException {
        return GoogleCloudStoragePathIterator.create(dir, filter);
    }

    @Override
    protected boolean exists(final GoogleCloudStoragePath path) throws IOException {
        if (path.getBucketName() == null) {
            // This is the fake root
            return true;
        }

        GoogleCloudStorageClient client = path.getFileSystem().getClient();
        boolean exists = false;

        if (path.getBlobName() != null) {// check if exact object exists
            exists = client.getObject(path.getBucketName(), path.getBlobName()) != null;
        }

        if (!exists) {// check if prefix and bucket exists
            // it is required to have trailing '/' at this point. Otherwise client.exists()
            // will return true for '/bucket/foo' even if only '/bucket/foobar' exists.
            String prefix = GoogleCloudStoragePath.ensureDirectoryPath(path.getBlobName());
            exists = client.exists(path.getBucketName(), prefix);
        }

        return exists;
    }

    @Override
    protected BaseFileAttributes fetchAttributesInternal(final GoogleCloudStoragePath path, final Class<?> type)
            throws IOException {
        FileTime createdAt = FileTime.fromMillis(0);
        FileTime modifiedAt = createdAt;
        long size = 0;
        boolean objectExists = false;

        if (path.getBucketName() != null) {
            GoogleCloudStorageClient client = path.getFileSystem().getClient();

            if (path.getBlobName() == null) {
                Bucket bucket = client.getBucket(path.getBucketName());
                createdAt = FileTime.fromMillis(bucket.getTimeCreated().getValue());
                modifiedAt = FileTime.fromMillis(bucket.getUpdated().getValue());
            } else {
                StorageObject object = client.getObject(path.getBucketName(), path.getBlobName());
                if (object != null) {
                    createdAt = FileTime.fromMillis(object.getTimeCreated().getValue());
                    modifiedAt = FileTime.fromMillis(object.getUpdated().getValue());
                    size = object.getSize().longValue();
                    objectExists = true;
                }
            }
        }
        return new BaseFileAttributes(!path.isDirectory() && objectExists, path, modifiedAt, modifiedAt, createdAt,
                size, false, false, null);

    }

    @Override
    protected void deleteInternal(final GoogleCloudStoragePath path) throws IOException {
        GoogleCloudStorageClient client = path.getFileSystem().getClient();
        String blobName = path.getBlobName();

        if (Files.isDirectory(path)) {
            blobName = GoogleCloudStoragePath.ensureDirectoryPath(blobName);
            if (client.isNotEmpty(path.getBucketName(), blobName)) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        if (path.getBlobName() != null) {
            client.deleteObject(path.getBucketName(), blobName);
        } else {
            client.deleteBucket(path.getBucketName());
        }

        // it is possible that parent directory(-s) only existed in a form of a prefix
        // and got deleted as a result of deleting the object
        if (!existsCached((GoogleCloudStoragePath) path.getParent())) {
            Files.createDirectories(path.getParent());
        }
    }

    @Override
    public void checkAccessInternal(final GoogleCloudStoragePath path, final AccessMode... modes) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void copyInternal(final GoogleCloudStoragePath source, final GoogleCloudStoragePath target,
            final CopyOption... options) throws IOException {
        GoogleCloudStorageClient client = source.getFileSystem().getClient();

        if (!Files.isDirectory(source)) {
            client.rewriteObject(source.getBucketName(), source.getBlobName(), target.getBucketName(),
                    target.getBlobName());
        } else {
            if (client.isNotEmpty(target.getBucketName(),
                    GoogleCloudStoragePath.ensureDirectoryPath(target.getBlobName()))) {
                throw new DirectoryNotEmptyException(
                        String.format("Target directory %s exists and is not empty", target.toString()));
            }
            createDirectory(target);
        }

    }

    @SuppressWarnings("resource")
    @Override
    protected void createDirectoryInternal(final GoogleCloudStoragePath path,
            final FileAttribute<?>... arg1)
            throws IOException {

        GoogleCloudStorageClient client = path.getFileSystem().getClient();

        if (path.getBlobName() != null) {
            String blob = GoogleCloudStoragePath.ensureDirectoryPath(path.getBlobName());
            client.insertObject(path.getBucketName(), blob, "");
        } else {
            client.insertBucket(path.getBucketName());
        }
    }

    @Override
    public FileStore getFileStore(final Path path) throws IOException {
        return path.getFileSystem().getFileStores().iterator().next();
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public boolean isHidden(final Path path) throws IOException {
        return false;
    }

    @Override
    protected void moveInternal(final GoogleCloudStoragePath source,
            final GoogleCloudStoragePath target,
            final CopyOption... options) throws IOException {
        GoogleCloudStorageClient client = source.getFileSystem().getClient();

        if (Files.isDirectory(target) && client.isNotEmpty(target.getBucketName(),
                GoogleCloudStoragePath.ensureDirectoryPath(target.getBlobName()))) {
            throw new DirectoryNotEmptyException(target.toString());
        }

        if (Files.isDirectory(source)) {
            String srcPath = GoogleCloudStoragePath.ensureDirectoryPath(source.getBlobName());
            List<StorageObject> list = client.listAllObjects(source.getBucketName(),
                    srcPath);
            if (list != null) {
                for (StorageObject so : list) {
                    String targetName = so.getName().replaceFirst(srcPath,
                            GoogleCloudStoragePath.ensureDirectoryPath(target.getBlobName()));
                    client.rewriteObject(so.getBucket(), so.getName(), target.getBucketName(), targetName);
                    delete(new GoogleCloudStoragePath(source.getFileSystem(), so.getBucket(), so.getName()));
                }
            }
        } else {
            client.rewriteObject(source.getBucketName(), source.getBlobName(), target.getBucketName(),
                    target.getBlobName());
            delete(source);
        }

    }

    @Override
    protected SeekableByteChannel newByteChannelInternal(
            final GoogleCloudStoragePath path,
            final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {
        return new GoogleCloudStorageSeekableByteChannel(path, options);
    }
}
