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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.util.FileSystemBrowser;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.filechooser.NioFileSystemBrowser;
import org.knime.google.api.data.GoogleApiConnection;
import org.knime.google.filehandling.nodes.connection.GoogleCloudStorageConnectionSettings;

/**
 * Google Cloud Storage implementation of the {@link FSConnection} interface.
 *
 * @author Alexander Bondaletov
 */
public class GoogleCloudStorageFSConnection implements FSConnection {

    private final static long CACHE_TTL_MILLIS = 6000;

    private final GoogleCloudStorageFileSystem m_filesystem;


    /**
     * Creates new {@link GoogleCloudStorageFSConnection} for a given api connection
     * and project.
     *
     * @param apiConnection
     *            google api connection
     * @param settings
     *            Connection settings.
     * @throws IOException
     */
    public GoogleCloudStorageFSConnection(final GoogleApiConnection apiConnection,
            final GoogleCloudStorageConnectionSettings settings)
            throws IOException {

        final GoogleCloudStorageFileSystemProvider provider = new GoogleCloudStorageFileSystemProvider();

        final URI uri;
        try {
            uri = new URI(GoogleCloudStorageFileSystemProvider.SCHEME, settings.getProjectId(), null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Illegal project id: " + settings.getProjectId(), ex);
        }

        final Map<String, Object> env = new HashMap<>();
        env.put(GoogleCloudStorageFileSystemProvider.KEY_API_CONNECTION, apiConnection);
        env.put(GoogleCloudStorageFileSystemProvider.KEY_GCS_CONNECTION_SETTINGS, settings);
        env.put(GoogleCloudStorageFileSystemProvider.KEY_CACHE_TTL_MILLIS, CACHE_TTL_MILLIS);
        m_filesystem = provider.getOrCreateFileSystem(uri, env);
    }

    @Override
    public FSFileSystem<?> getFileSystem() {
        return m_filesystem;
    }

    @Override
    public FileSystemBrowser getFileSystemBrowser() {
        return new NioFileSystemBrowser(this);
    }
}
