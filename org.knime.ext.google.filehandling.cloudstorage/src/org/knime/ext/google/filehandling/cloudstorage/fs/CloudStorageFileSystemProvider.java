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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

import com.google.api.client.http.HttpStatusCodes;
import com.google.cloud.storage.StorageException;

/**
 * File system provider for {@link CloudStorageFileSystem}.
 *
 * @author Alexander Bondaletov
 */
class CloudStorageFileSystemProvider extends BaseFileSystemProvider<CloudStoragePath, CloudStorageFileSystem> {


    @SuppressWarnings("resource")
    @Override
    protected InputStream newInputStreamInternal(final CloudStoragePath path, final OpenOption... options)
            throws IOException {
        try {
            return getFileSystemInternal().getClient().getInputStream(path.getBucketName(), path.getBlobName());
        } catch (StorageException e) {
            if (e.getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
                throw new NoSuchFileException(path.toString());
            }
            throw new IOException(e);
        }
    }

    @SuppressWarnings("resource")
    @Override
    protected OutputStream newOutputStreamInternal(final CloudStoragePath path, final OpenOption... options)
            throws IOException {
        final Set<OpenOption> opts = new HashSet<>(Arrays.asList(options));
        return Channels.newOutputStream(newByteChannel(path, opts));
    }

    @Override
    protected Iterator<CloudStoragePath> createPathIterator(final CloudStoragePath dir,
            final Filter<? super Path> filter) throws IOException {
        return CloudStoragePathIteratorFactory.create(dir.toDirectoryPath(), filter);
    }

    @Override
    protected boolean exists(final CloudStoragePath path) throws IOException {
        if (path.getBucketName() == null) {
            // This is the fake root
            return true;
        }

        @SuppressWarnings("resource")
        CloudStorageClient client = getFileSystemInternal().getClient();
        boolean exists = false;

        if (path.getBlobName() != null) {// check if exact object exists
            exists = client.getBlob(path.getBucketName(), path.getBlobName()) != null;
        }

        if (!exists) {// check if prefix and bucket exists
            // it is required to have trailing '/' at this point. Otherwise client.exists()
            // will return true for '/bucket/foo' even if only '/bucket/foobar' exists.
            String prefix = path.toDirectoryPath().getBlobName();
            exists = client.exists(path.getBucketName(), prefix);
        }

        return exists;
    }

    @Override
    protected BaseFileAttributes fetchAttributesInternal(final CloudStoragePath path, final Class<?> type)
            throws IOException {
        FileTime createdAt = FileTime.fromMillis(0);
        FileTime modifiedAt = createdAt;
        long size = 0;
        boolean objectExists = false;

        if (path.getBucketName() != null) {
            @SuppressWarnings("resource")
            final var client = getFileSystemInternal().getClient();
            OffsetDateTime createTimeOffsetDateTime = null;
            OffsetDateTime updateTimeOffsetDateTime = null;
            if (path.getBlobName() == null) {
                final var bucket = client.getBucket(path.getBucketName());
                createTimeOffsetDateTime = bucket.getCreateTimeOffsetDateTime();
                updateTimeOffsetDateTime = bucket.getUpdateTimeOffsetDateTime();
            } else {
                final var blob = client.getBlob(path.getBucketName(), path.getBlobName());
                if (blob != null) {
                    createTimeOffsetDateTime = blob.getCreateTimeOffsetDateTime();
                    updateTimeOffsetDateTime = blob.getUpdateTimeOffsetDateTime();
                    size = blob.getSize();
                    objectExists = true;
                }
            }
            if (createTimeOffsetDateTime != null) {
                createdAt = FileTime.from(createTimeOffsetDateTime.toInstant());
            }
            if (updateTimeOffsetDateTime != null) {
                modifiedAt = FileTime.from(updateTimeOffsetDateTime.toInstant());
            }
        }
        return new BaseFileAttributes(!path.isDirectory() && objectExists, path, modifiedAt, modifiedAt, createdAt,
                size, false, false, null);

    }

    @Override
    protected void deleteInternal(final CloudStoragePath path) throws IOException {
        @SuppressWarnings("resource")
        CloudStorageClient client = getFileSystemInternal().getClient();
        String blobName = path.getBlobName();

        if (isDirectory(path)) {
            blobName = path.toDirectoryPath().getBlobName();
            if (client.isNotEmpty(path.getBucketName(), blobName)) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        if (path.getBlobName() != null) {
            client.deleteBlob(path.getBucketName(), blobName);
        } else {
            client.deleteBucket(path.getBucketName());
        }
    }

    @Override
    public void checkAccessInternal(final CloudStoragePath path, final AccessMode... modes) throws IOException {
        // nothing to do here
    }

    @Override
    public void copyInternal(final CloudStoragePath source, final CloudStoragePath target, final CopyOption... options)
            throws IOException {
        @SuppressWarnings("resource")
        CloudStorageClient client = getFileSystemInternal().getClient();

        if (!isDirectory(source)) {
            client.copyBlob(source.getBucketName(), source.getBlobName(), target.getBucketName(),
                    target.getBlobName());
        } else {

            if (client.isNotEmpty(target.getBucketName(), target.toDirectoryPath().getBlobName())) {
                throw new DirectoryNotEmptyException(
                        String.format("Target directory %s exists and is not empty", target.toString()));
            }
            createDirectory(target);
        }

    }

    private boolean isDirectory(final CloudStoragePath path) throws IOException {
        return readAttributes(path, BasicFileAttributes.class).isDirectory();
    }

    @SuppressWarnings("resource")
    @Override
    protected void createDirectoryInternal(final CloudStoragePath path, final FileAttribute<?>... arg1)
            throws IOException {

        CloudStorageClient client = getFileSystemInternal().getClient();

        final CloudStoragePath dirPath = path.toDirectoryPath();
        if (path.getBlobName() != null) {
            client.insertBlob(dirPath.getBucketName(), dirPath.getBlobName(), "");
        } else {
            client.insertBucket(dirPath.getBucketName());
        }
    }

    @Override
    protected SeekableByteChannel newByteChannelInternal(final CloudStoragePath path,
            final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
        return new CloudStorageSeekableByteChannel(path, options);
    }
}
