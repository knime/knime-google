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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.google.api.data.GoogleApiConnection;

/**
 * File system provider for {@link GoogleCloudStorageFileSystem}.
 *
 * @author Alexander Bondaletov
 */
public class GoogleCloudStorageFileSystemProvider
        extends BaseFileSystemProvider<GoogleCloudStoragePath, GoogleCloudStorageFileSystem> {

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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected OutputStream newOutputStreamInternal(final GoogleCloudStoragePath path, final OpenOption... options)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Iterator<GoogleCloudStoragePath> createPathIterator(final GoogleCloudStoragePath dir,
            final Filter<? super Path> filter) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean exists(final GoogleCloudStoragePath path) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected BaseFileAttributes fetchAttributesInternal(final GoogleCloudStoragePath path, final Class<?> type)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void deleteInternal(final GoogleCloudStoragePath path) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void checkAccessInternal(final GoogleCloudStoragePath path, final AccessMode... modes) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void copyInternal(final GoogleCloudStoragePath source, final GoogleCloudStoragePath target,
            final CopyOption... options) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void createDirectory(final Path arg0, final FileAttribute<?>... arg1) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public FileStore getFileStore(final Path arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public boolean isHidden(final Path arg0) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void moveInternal(final GoogleCloudStoragePath source, final GoogleCloudStoragePath target,
            final CopyOption... options) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public SeekableByteChannel newByteChannelInternal(final Path arg0,
            final Set<? extends OpenOption> arg1,
            final FileAttribute<?>... arg2) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAttribute(final Path arg0, final String arg1, final Object arg2, final LinkOption... arg3)
            throws IOException {
        // TODO Auto-generated method stub

    }

}
