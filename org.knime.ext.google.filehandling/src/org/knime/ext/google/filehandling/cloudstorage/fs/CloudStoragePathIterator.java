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
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;

/**
 * Class to iterate through the files and folders in the path
 *
 * @author Alexander Bondaletov
 */
public abstract class CloudStoragePathIterator implements Iterator<CloudStoragePath> {

    /**
     * Path object
     */
    protected final CloudStoragePath m_path;
    /**
     * File system
     */
    protected final CloudStorageFileSystem m_fs;
    /**
     * Storage client
     */
    protected final CloudStorageClient m_client;

    private final Filter<? super Path> m_filter;

    private boolean initialized;
    private CloudStoragePath m_nextPath;
    private String m_nextPageToken;

    /**
     * Creates iterator instance.
     *
     * @param path
     *            path to iterate.
     * @param filter
     *            {@link Filter} instance.
     * @return {@link CloudStoragePathIterator} instance.
     * @throws IOException
     */
    public static CloudStoragePathIterator create(final CloudStoragePath path,
            final Filter<? super Path> filter) throws IOException {
        if (path.getNameCount() == 0) {
            return new BucketIterator(path, filter);
        } else {
            return new BlobIterator(path, filter);
        }
    }

    /**
     * Creates iterator instance.
     *
     * @param path
     *            path to iterate.
     * @param filter
     *            {@link Filter} instance.
     * @throws IOException
     */
    protected CloudStoragePathIterator(final CloudStoragePath path, final Filter<? super Path> filter) {
        m_path = path;
        m_fs = path.getFileSystem();
        m_client = m_fs.getClient();
        m_filter = filter;

        initialized = false;
    }

    /**
     * Performs initialization by fetching the first path. Cannot be called in the
     * base class constructor, so has to be called in the derived classes
     * constructors.
     *
     * @throws IOException
     */
    protected void init() throws IOException {
        m_nextPath = getNextFilteredPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_nextPath != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudStoragePath next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        CloudStoragePath current = m_nextPath;
        try {
            m_nextPath = getNextFilteredPath();
        } catch (IOException ex) {
            throw new DirectoryIteratorException(ex);
        }
        return current;
    }

    private CloudStoragePath getNextFilteredPath() throws IOException {
        CloudStoragePath next = getNextPath();
        while (next != null) {
            if (m_filter.accept(next)) {
                return next;
            }
            next = getNextPath();
        }
        return null;
    }

    private CloudStoragePath getNextPath() throws IOException {
        CloudStoragePath path = getNextPathFromCurrentPage();

        if (path == null && (m_nextPageToken != null || !initialized)) {
            m_nextPageToken = loadNextPage(m_nextPageToken);
            path = getNextPathFromCurrentPage();
            initialized = true;
        }

        return path;
    }

    /**
     * Loads next page corresponding to provided token. The first page is loaded if
     * token is null.
     *
     * @param token
     *            Continuation token.
     * @return Continuation token of the next page (if available).
     * @throws IOException
     */
    protected abstract String loadNextPage(String token) throws IOException;

    /**
     * @return Next path entry from the currently loaded page. <code>null</code> if
     *         the end of the page is reached.
     */
    protected abstract CloudStoragePath getNextPathFromCurrentPage();

    private static class BucketIterator extends CloudStoragePathIterator {

        private Iterator<Bucket> m_bucketsIter;

        private BucketIterator(final CloudStoragePath path, final Filter<? super Path> filter)
                throws IOException {
            super(path, filter);
            init();
        }

        /**
         * {@inheritDoc}
         *
         * @throws IOException
         */
        @Override
        protected String loadNextPage(final String token) throws IOException {
            Buckets buckets = m_client.listBuckets(token);
            if (buckets.getItems() != null) {
                m_bucketsIter = buckets.getItems().iterator();
            }
            return buckets.getNextPageToken();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected CloudStoragePath getNextPathFromCurrentPage() {
            if (m_bucketsIter != null && m_bucketsIter.hasNext()) {
                return createPath(m_bucketsIter.next());
            }
            return null;
        }

        private CloudStoragePath createPath(final Bucket bucket) {
            CloudStoragePath path = m_fs.getPath(m_fs.getSeparator() + bucket.getName(), m_fs.getSeparator());

            FileTime createdAt = FileTime.fromMillis(bucket.getTimeCreated().getValue());
            FileTime modifiedAt = FileTime.fromMillis(bucket.getUpdated().getValue());

            BaseFileAttributes attrs = new BaseFileAttributes(false, path, modifiedAt, modifiedAt, createdAt, 0, false,
                    false, null);
            m_fs.addToAttributeCache(path, attrs);

            return path;
        }
    }

    private static class BlobIterator extends CloudStoragePathIterator {

        private String m_prefix;
        private Iterator<String> m_prefixes;
        private Iterator<StorageObject> m_objects;

        private BlobIterator(final CloudStoragePath path, final Filter<? super Path> filter) throws IOException {
            super(path, filter);
            m_prefix = m_path.getBlobName();
            init();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String loadNextPage(final String token) throws IOException {
            Objects objects = m_client.listObjects(m_path.getBucketName(), m_prefix, token);
            if (objects.getPrefixes() != null) {
                m_prefixes = objects.getPrefixes().iterator();
            }
            if (objects.getItems() != null) {
                m_objects = objects.getItems().iterator();
            }
            return objects.getNextPageToken();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected CloudStoragePath getNextPathFromCurrentPage() {
            if (m_prefixes != null && m_prefixes.hasNext()) {
                return createPath(m_prefixes.next());
            }

            StorageObject obj = getNextObject();
            if (obj != null) {
                return createPath(obj);
            }
            return null;
        }

        private StorageObject getNextObject() {
            if (m_objects != null && m_objects.hasNext()) {
                StorageObject obj = m_objects.next();

                if (m_prefix != null && m_prefix.equals(obj.getName())) {
                    // response could include an object whose name is exactly matches provided
                    // prefix which is effectively a parent directory and should be skipped
                    return getNextObject();
                } else {
                    return obj;
                }
            }
            return null;
        }

        private CloudStoragePath createPath(final String prefix) {
            CloudStoragePath path = new CloudStoragePath(m_fs, m_path.getBucketName(), prefix);

            FileTime time = FileTime.fromMillis(0);
            BaseFileAttributes attrs = new BaseFileAttributes(false, path, time, time, time, 0, false, false, null);
            m_fs.addToAttributeCache(path, attrs);

            return path;
        }

        private CloudStoragePath createPath(final StorageObject object) {
            CloudStoragePath path = new CloudStoragePath(m_fs, m_path.getBucketName(), object.getName());

            FileTime createdAt = FileTime.fromMillis(object.getTimeCreated().getValue());
            FileTime modifiedAt = FileTime.fromMillis(object.getUpdated().getValue());

            BaseFileAttributes attrs = new BaseFileAttributes(!path.isDirectory(), path, modifiedAt, modifiedAt,
                    createdAt, object.getSize().longValue(), false, false, null);
            m_fs.addToAttributeCache(path, attrs);

            return path;
        }
    }
}
