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
 *   2020-03-26 (Alexander Bondaletov): created
 */
package org.knime.ext.google.filehandling.cloudstorage.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.knime.filehandling.core.connections.base.PagedPathIterator;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;

/**
 * Class to iterate through the files and folders in the path
 *
 * @author Alexander Bondaletov
 */
final class CloudStoragePathIteratorFactory {

    private CloudStoragePathIteratorFactory() {
    }


    /**
     * Creates iterator instance.
     *
     * @param path
     *            path to iterate.
     * @param filter
     *            {@link Filter} instance.
     * @return an iterator over paths
     * @throws IOException
     */
    public static Iterator<CloudStoragePath> create(
            final CloudStoragePath path,
            final Filter<? super Path> filter) throws IOException {
        if (path.getNameCount() == 0) {
            return new BucketIterator(path, filter);
        } else {
            return new BlobIterator(path, filter);
        }
    }

    private static final class BucketIterator extends PagedPathIterator<CloudStoragePath> {

        private String m_nextPageToken = null;

        private BucketIterator(final CloudStoragePath path, final Filter<? super Path> filter)
                throws IOException {
            super(path, filter);
            setFirstPage(loadNextPage());
        }

        @Override
        protected boolean hasNextPage() {
            return StringUtils.isNotBlank(m_nextPageToken);
        }

        @Override
        protected Iterator<CloudStoragePath> loadNextPage() throws IOException {
            @SuppressWarnings("resource")
            final Page<Bucket> page = m_path.getFileSystem().getClient().listBuckets(m_nextPageToken);
            m_nextPageToken = page.getNextPageToken();
            final var hasElements = page.getValues().iterator().hasNext();

            if (hasElements) {
                return page.streamValues().map(this::createPath).iterator();
            } else {
                return null;
            }
        }

        @SuppressWarnings("resource")
        private CloudStoragePath createPath(final Bucket bucket) {
            final CloudStorageFileSystem fs = m_path.getFileSystem();

            final CloudStoragePath path = fs.getPath(fs.getSeparator() + bucket.getName(), fs.getSeparator());
            var createdAt = FileTime.fromMillis(0);
            var modifiedAt = createdAt;
            if (bucket.getCreateTimeOffsetDateTime() != null) {
                createdAt = FileTime.from(bucket.getCreateTimeOffsetDateTime().toInstant());
            }
            if (bucket.getUpdateTimeOffsetDateTime() != null) {
                modifiedAt = FileTime.from(bucket.getUpdateTimeOffsetDateTime().toInstant());
            }
            final BaseFileAttributes attrs = new BaseFileAttributes(false, //
                    path, //
                    modifiedAt, //
                    modifiedAt, //
                    createdAt, //
                    0, //
                    false, //
                    false, //
                    null);
            fs.addToAttributeCache(path, attrs);

            return path;
        }
    }

    private static final class BlobIterator extends PagedPathIterator<CloudStoragePath> {

        private String m_nextPageToken;

        private BlobIterator(final CloudStoragePath path, final Filter<? super Path> filter) throws IOException {
            super(path, filter);
            setFirstPage(loadNextPage());
        }

        @Override
        protected boolean hasNextPage() {
            return StringUtils.isNotBlank(m_nextPageToken);
        }

        @SuppressWarnings("resource")
        @Override
        protected Iterator<CloudStoragePath> loadNextPage() throws IOException {
            final CloudStorageFileSystem fs = m_path.getFileSystem();
            final String prefix = m_path.getBlobName();

            Page<Blob> blobs = fs.getClient().listBlobs(m_path.getBucketName(), prefix, m_nextPageToken);
            final List<CloudStoragePath> paths = new ArrayList<>();

            blobs.streamValues() //
                    .filter(obj -> !Objects.equals(prefix, obj.getName())) //
                    .map(this::createPath) //
                    .forEach(paths::add); // NOSONAR we want a mutable list

            m_nextPageToken = blobs.getNextPageToken();

            return paths.iterator();
        }

        @SuppressWarnings("resource")
        private CloudStoragePath createPath(final Blob blob) {
            final CloudStorageFileSystem fs = m_path.getFileSystem();
            CloudStoragePath path = new CloudStoragePath(fs, m_path.getBucketName(), blob.getName());
            var createdAt = FileTime.fromMillis(0);
            var modifiedAt = createdAt;
            if (blob.getCreateTimeOffsetDateTime() != null) {
                createdAt = FileTime.from(blob.getCreateTimeOffsetDateTime().toInstant());
            }
            if (blob.getUpdateTimeOffsetDateTime() != null) {
                modifiedAt = FileTime.from(blob.getUpdateTimeOffsetDateTime().toInstant());
            }
            BaseFileAttributes attrs = new BaseFileAttributes(!path.isDirectory(), path, modifiedAt, modifiedAt,
                    createdAt, blob.getSize(), false, false, null);
            fs.addToAttributeCache(path, attrs);

            return path;
        }

    }
}
