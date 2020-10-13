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
package org.knime.ext.google.filehandling.cloudstorage.testing;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageClient;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFSConnection;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFileSystem;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStoragePath;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.testing.DefaultFSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.storage.model.StorageObject;

/**
 * Cloud storage test initializer.
 *
 * @author Alexander Bondaletov
 */
public class CloudStorageTestInitializer
        extends DefaultFSTestInitializer<CloudStoragePath, CloudStorageFileSystem> {

    private final CloudStorageClient m_client;

    private final ExecutorService m_threadPool = Executors.newCachedThreadPool();

    /**
     * Creates initializer.
     *
     * @param fsConnection
     *            fs connection.
     */
    public CloudStorageTestInitializer(final CloudStorageFSConnection fsConnection) {
        super(fsConnection);
        m_client = getFileSystem().getClient();
    }

    @Override
    public CloudStoragePath createFile(final String... pathComponents) throws IOException {
        return createFileWithContent("", pathComponents);
    }

    @Override
    public CloudStoragePath createFileWithContent(final String content, final String... pathComponents)
            throws IOException {

        final List<Future<Void>> futures = new LinkedList<>();

        final CloudStoragePath path = makePath(pathComponents);

        String key = "";
        for (int i = 1; i < path.getNameCount() - 1; i++) {
            key += path.getName(i).toString();

            if (getTestCaseScratchDir().toDirectoryPath().toString().startsWith(key)) {
                // we don't need to recreate the scratch dir structure every time, this is done
                // in
                // beforeTestCaseInternal().
                continue;
            }

            final String objectKey = key;
            futures.add(execAndRetry(() -> {
                m_client.insertObject(path.getBucketName(), objectKey, "");
                return null;
            }));
        }

        futures.add(execAndRetry(() -> {
            m_client.insertObject(path.getBucketName(), path.subpath(1, path.getNameCount()).toString(), content);
            return null;
        }));

        awaitFutures(futures);

        return path;
    }

    private static void awaitFutures(final List<Future<Void>> futures) {
        try {
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (ExecutionException | InterruptedException e) {
            ExceptionUtil.wrapAsIOException(e);
        }
    }

    @Override
    protected void beforeTestCaseInternal() throws IOException {
        final CloudStoragePath scratchDir = getTestCaseScratchDir().toDirectoryPath();

        awaitFutures(Collections.singletonList(execAndRetry(() -> {
            m_client.insertObject(scratchDir.getBucketName(), scratchDir.getBlobName(), "");
            return null;
        })));

    }

    @Override
    protected void afterTestCaseInternal() throws IOException {
        final CloudStoragePath scratchDir = getTestCaseScratchDir().toDirectoryPath();

        final List<Future<Void>> futures = new LinkedList<>();

        List<StorageObject> objects = m_client.listAllObjects(scratchDir.getBucketName(), scratchDir.getBlobName());
        if (objects != null) {
            for (StorageObject o : objects) {
                futures.add(execAndRetry(() -> {
                    m_client.deleteObject(scratchDir.getBucketName(), o.getName());
                    return null;
                }));
            }
        }

        awaitFutures(futures);
        getFileSystem().clearAttributesCache();
    }

    private Future<Void> execAndRetry(final IOESupplier<Void> request) throws IOException {
        return m_threadPool.submit(() -> {
            for (int i = 0; i < 3; i++) {
                try {
                    request.get();
                    return null;
                } catch (GoogleJsonResponseException ex) {
                    if (ex.getStatusCode() == 429) {// Rate limit exceeded
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ex1) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        throw ex;
                    }
                }
            }
            throw new IOException("Rate limit exceeded multiple times");
        });
    }
}
