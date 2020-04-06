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

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;
import org.knime.google.api.data.GoogleApiConnection;
import org.knime.google.filehandling.util.GoogleCloudStorageClient;

/**
 * Google Cloud Storage implementation of the {@link FileSystem} interface.
 *
 * @author Alexander Bondaletov
 */
public class GoogleCloudStorageFileSystem extends BaseFileSystem<GoogleCloudStoragePath> {
    private static final String PATH_SEPARATOR = "/";

    private final GoogleCloudStorageClient m_client;
    private final GoogleCloudStoragePath m_workingDirectory;

    /**
     * Constructs {@link GoogleCloudStorageFileSystem} for a given URI.
     *
     * @param provider
     *            the {@link GoogleCloudStorageFileSystemProvider}
     * @param uri
     *            the URI for the file system
     * @param apiConnection
     * @param cacheTTL
     */
    public GoogleCloudStorageFileSystem(final GoogleCloudStorageFileSystemProvider provider, final URI uri,
            final GoogleApiConnection apiConnection, final long cacheTTL) {
        super(provider, uri, "Google Cloud Storage File System", "Google Cloud Storage File System", cacheTTL,
                Choice.CONNECTED_FS, Optional.of(uri.getHost()));

        m_client = new GoogleCloudStorageClient(apiConnection, uri);
        m_workingDirectory = getPath(PATH_SEPARATOR);
    }

    /**
     * @return {@link GoogleCloudStorageClient} for this file system.
     */
    public GoogleCloudStorageClient getClient() {
        return m_client;
    }

    @Override
    protected void prepareClose() {
        // nothing to close
    }

    @Override
    public String getSchemeString() {
        return provider().getScheme();
    }

    @Override
    public String getHostString() {
        return null;
    }

    @Override
    public GoogleCloudStoragePath getPath(final String first, final String... more) {
        return new GoogleCloudStoragePath(this, first, more);
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(PATH_SEPARATOR));
    }

    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GoogleCloudStoragePath getWorkingDirectory() {
        return m_workingDirectory;
    }

}
