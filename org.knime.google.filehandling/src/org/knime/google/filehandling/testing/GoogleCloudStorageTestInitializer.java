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
package org.knime.google.filehandling.testing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.CheckedExceptionSupplier;
import org.knime.google.filehandling.connections.GoogleCloudStorageConnection;
import org.knime.google.filehandling.connections.GoogleCloudStorageFileSystem;
import org.knime.google.filehandling.connections.GoogleCloudStoragePath;
import org.knime.google.filehandling.util.GoogleCloudStorageClient;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.storage.model.StorageObject;

/**
 * Cloud storage test initializer.
 *
 * @author Alexander Bondaletov
 */
public class GoogleCloudStorageTestInitializer implements FSTestInitializer {

    private final String m_bucket;
    private final GoogleCloudStorageConnection m_fsConnection;
    private final GoogleCloudStorageFileSystem m_filesystem;
    private final GoogleCloudStorageClient m_client;
    private final String m_uniquePrefix;

    /**
     * Creates initializer.
     *
     * @param bucket
     *            bucket name.
     * @param fsConnection
     *            fs connection.
     */
    public GoogleCloudStorageTestInitializer(final String bucket, final GoogleCloudStorageConnection fsConnection) {
        m_bucket = bucket;
        m_fsConnection = fsConnection;
        m_filesystem = (GoogleCloudStorageFileSystem) fsConnection.getFileSystem();
        m_client = m_filesystem.getClient();
        m_uniquePrefix = UUID.randomUUID().toString();
    }

    @Override
    public FSConnection getFSConnection() {
        return m_fsConnection;
    }

    @Override
    public Path getRoot() {
        return m_filesystem.getPath("/", m_bucket, m_uniquePrefix + "/");
    }

    @Override
    public Path createFile(final String... pathComponents) throws IOException {
        return createFileWithContent("", pathComponents);
    }

    @Override
    public Path createFileWithContent(final String content, final String... pathComponents) throws IOException {
        Path absoulutePath = //
                Arrays //
                        .stream(pathComponents) //
                        .reduce( //
                                getRoot(), //
                                (path, pathComponent) -> path.resolve(pathComponent), //
                                (p1, p2) -> p1.resolve(p2) //
                        ); //

        final String key = absoulutePath.subpath(1, absoulutePath.getNameCount()).toString();
        execAndRetry(() -> {
            m_client.insertObject(m_bucket, key, content);
            return null;
        });
        return absoulutePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTestCase() throws IOException {
        GoogleCloudStoragePath root = (GoogleCloudStoragePath) getRoot();
        execAndRetry(() -> {
            m_client.insertObject(root.getBucketName(), root.getBlobName(), "");
            return null;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTestCase() throws IOException {
        List<StorageObject> objects = m_client.listAllObjects(m_bucket, m_uniquePrefix + "/");
        if (objects != null) {
            for (StorageObject o : objects) {
                execAndRetry(() -> {
                    m_client.deleteObject(m_bucket, o.getName());
                    return null;
                });
            }
        }
    }

    private static void execAndRetry(final CheckedExceptionSupplier<Void, IOException> request) throws IOException {
        try {
            request.apply();
        } catch (GoogleJsonResponseException ex) {
            if (ex.getStatusCode() == 429) {// Rate limit exceeded
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex1) {
                    Thread.currentThread().interrupt();
                }
                request.apply();
            } else {
                throw ex;
            }
        }
    }
}
