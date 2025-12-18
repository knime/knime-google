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
 *   Jan 7, 2026 (magnus): created
 */
package org.knime.google.api.sheets.nodes.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.google.api.sheets.data.GoogleSheetsConnection;

import com.google.api.services.drive.model.FileList;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * Abstract node model class for Google Sheet nodes that cache the list of spreadsheets for a given connection.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
public abstract class GoogleSpreadSheetCacheNodeModel extends NodeModel {

    /**
     * Constructor for node models with custom in- and out-port types.
     *
     * @param inPortTypes
     * @param outPortTypes
     */
    protected GoogleSpreadSheetCacheNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    record SpreadSheetCacheKey(GoogleSheetsConnection sheetsConnection) {
    }

    private LoadingCache<SpreadSheetCacheKey, List<com.google.api.services.drive.model.File>> m_spreadSheetsCache =
        CacheBuilder.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES).weakKeys()
            .build(new CacheLoader<SpreadSheetCacheKey, List<com.google.api.services.drive.model.File>>() {

                @Override
                public List<com.google.api.services.drive.model.File> load(final SpreadSheetCacheKey key) {
                    try {
                        final List<com.google.api.services.drive.model.File> spreadsheets = new ArrayList<>();
                        final com.google.api.services.drive.Drive.Files.List request =
                            key.sheetsConnection.getDriveService().files().list()
                                .setQ("mimeType='application/vnd.google-apps.spreadsheet'");

                        do {
                            final FileList execute = request.execute();
                            spreadsheets.addAll(execute.getFiles());
                        } while (request.getPageToken() != null && request.getPageToken().length() > 0);

                        return spreadsheets;
                    } catch (IOException | NoSuchCredentialException e) {
                        throw ExceptionUtils.asRuntimeException(e);
                    }
                }

                @Override
                public ListenableFuture<List<com.google.api.services.drive.model.File>> reload(
                    final SpreadSheetCacheKey key, final List<com.google.api.services.drive.model.File> oldValue)
                    throws Exception {
                    return Futures.immediateFuture(load(key));
                }

            });

    /**
     * Retrieves the spreadsheets cache entry.
     *
     * @param key the google sheets connection
     * @return the spreadsheets cache entry
     * @throws ExecutionException if the cache could not be retrieved
     */
    protected List<com.google.api.services.drive.model.File> getSpreadSheetsEntry(final GoogleSheetsConnection key)
        throws ExecutionException {
        return m_spreadSheetsCache.get(new SpreadSheetCacheKey(key));
    }

    /**
     * Computes the spreadsheets cache if necessary and returns the entry.
     *
     * @param key the google sheet connection
     * @return the spreadsheets cache entry
     * @throws ExecutionException if the cache could not be computed
     * @throws UncheckedExecutionException if an unchecked exception occurred during cache computation
     * @throws ExecutionError if an error occurred during cache computation
     */
    protected List<com.google.api.services.drive.model.File> computeSpreadSheetsEntry(final GoogleSheetsConnection key)
        throws ExecutionException, UncheckedExecutionException, ExecutionError {
        final var cacheKey = new SpreadSheetCacheKey(key);
        m_spreadSheetsCache.refresh(cacheKey);
        return m_spreadSheetsCache.get(cacheKey);
    }

}
