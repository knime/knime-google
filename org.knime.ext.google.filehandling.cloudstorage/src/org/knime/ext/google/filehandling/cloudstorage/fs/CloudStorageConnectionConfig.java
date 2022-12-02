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
 *   2020-10-28 (Vyacheslav Soldatov): created
 */
package org.knime.ext.google.filehandling.cloudstorage.fs;

import java.time.Duration;

import org.knime.filehandling.core.connections.meta.base.BaseFSConnectionConfig;
import org.knime.google.api.data.GoogleApiConnection;

/**
 * Google Cloud Storage connection configuration implementation.
 *
 * @author Moditha Hewasinghage
 */
public class CloudStorageConnectionConfig extends BaseFSConnectionConfig {

    /**
     * Default timeout for connecting and connecting (in seconds).
     */
    public static final int DEFAULT_TIMEOUT_SECONDS = 20;

    private String m_projectId;
    private boolean m_normalizePaths;
    private Duration m_connectionTimeOut;
    private Duration m_readTimeOut;

    private final GoogleApiConnection m_apiConnection;

    /**
     *
     * @param workingDirectory
     * @param relativizationBehavior
     *            The browser relativization behavior.
     * @param apiConnection
     */
    public CloudStorageConnectionConfig(final String workingDirectory,
            final BrowserRelativizationBehavior relativizationBehavior, final GoogleApiConnection apiConnection) {
        super(workingDirectory, true, relativizationBehavior);
        m_apiConnection = apiConnection;
    }

    /**
     * @return connection time out.
     */
    public Duration getConnectionTimeOut() {
        return m_connectionTimeOut;
    }

    /**
     * @param connectionTimeOut
     *            connection time out.
     */
    public void setConnectionTimeOut(final Duration connectionTimeOut) {
        this.m_connectionTimeOut = connectionTimeOut;
    }

    /**
     * @return connection read time out.
     */
    public Duration getReadTimeOut() {
        return m_readTimeOut;
    }

    /**
     * @param readTimeOut
     *            connection read time out.
     */
    public void setReadTimeOut(final Duration readTimeOut) {
        this.m_readTimeOut = readTimeOut;
    }

    /**
     * @return the projectId
     */
    public String getProjectId() {
        return m_projectId;
    }

    /**
     * @param projectId
     *            the projectId to set
     */
    public void setProjectId(final String projectId) {
        m_projectId = projectId;
    }

    /**
     * @return the normalizePaths
     */
    public boolean isNormalizePaths() {
        return m_normalizePaths;
    }

    /**
     * @param normalizePaths
     *            the normalizePaths to set
     */
    public void setNormalizePaths(final boolean normalizePaths) {
        m_normalizePaths = normalizePaths;
    }

    /**
     * @return the apiConnection
     */
    public GoogleApiConnection getApiConnection() {
        return m_apiConnection;
    }

}
