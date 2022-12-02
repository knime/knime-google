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
 *   2020-04-22 (Alexander Bondaletov): created
 */
package org.knime.ext.google.filehandling.cloudstorage.node;

import java.time.Duration;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageConnectionConfig;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFileSystem;
import org.knime.filehandling.core.connections.meta.base.BaseFSConnectionConfig.BrowserRelativizationBehavior;
import org.knime.google.api.data.GoogleApiConnection;

/**
 * Node settings for for Google Cloud Storage Connector node.
 *
 * @author Alexander Bondaletov
 */
public class CloudStorageConnectorSettings {
    private static final String KEY_PROJECT_ID = "projectId";
    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";
    private static final String KEY_NORMALIZE_PATHS = "normalizePaths";
    private static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";
    private static final String KEY_READ_TIMEOUT = "readTimeout";
    private static final String KEY_BROWSER_PATH_RELATIVE = "browserPathRelativize";

    private SettingsModelString m_projectId;
    private SettingsModelString m_workingDirectory;
    private SettingsModelBoolean m_normalizePaths;
    private SettingsModelIntegerBounded m_connectionTimeout;
    private SettingsModelIntegerBounded m_readTimeout;
    private final SettingsModelBoolean m_browserPathRelative;

    /**
     * Creates new instance
     */
    public CloudStorageConnectorSettings() {
        m_projectId = new SettingsModelString(KEY_PROJECT_ID, "");
        m_workingDirectory = new SettingsModelString(KEY_WORKING_DIRECTORY, CloudStorageFileSystem.PATH_SEPARATOR);
        m_normalizePaths = new SettingsModelBoolean(KEY_NORMALIZE_PATHS, true);
        m_connectionTimeout = new SettingsModelIntegerBounded(KEY_CONNECTION_TIMEOUT, CloudStorageConnectionConfig.DEFAULT_TIMEOUT_SECONDS, 0,
                Integer.MAX_VALUE);
        m_readTimeout = new SettingsModelIntegerBounded(KEY_READ_TIMEOUT, CloudStorageConnectionConfig.DEFAULT_TIMEOUT_SECONDS, 0, Integer.MAX_VALUE);
        m_browserPathRelative = new SettingsModelBoolean(KEY_BROWSER_PATH_RELATIVE, false);
    }

    /**
     * Saves the settings in this instance to the given {@link NodeSettingsWO}
     *
     * @param settings
     *            Node settings.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_projectId.saveSettingsTo(settings);
        m_workingDirectory.saveSettingsTo(settings);
        m_normalizePaths.saveSettingsTo(settings);
        m_connectionTimeout.saveSettingsTo(settings);
        m_readTimeout.saveSettingsTo(settings);
        m_browserPathRelative.saveSettingsTo(settings);
    }

    /**
     * Validates the settings in a given {@link NodeSettingsRO}
     *
     * @param settings
     *            Node settings.
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_projectId.validateSettings(settings);
        m_workingDirectory.validateSettings(settings);
        m_normalizePaths.validateSettings(settings);
        m_connectionTimeout.validateSettings(settings);
        m_readTimeout.validateSettings(settings);

        if (settings.containsKey(KEY_BROWSER_PATH_RELATIVE)) {
            m_browserPathRelative.validateSettings(settings);
        }

        CloudStorageConnectorSettings temp = new CloudStorageConnectorSettings();
        temp.loadSettingsFrom(settings);
        temp.validate();
    }

    /**
     * Validates settings consistency for this instance.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (m_projectId.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Project ID is not configured");
        }

        if (m_workingDirectory.getStringValue().isEmpty()
                || !m_workingDirectory.getStringValue().startsWith(CloudStorageFileSystem.PATH_SEPARATOR)) {
            throw new InvalidSettingsException("Working directory must be set to an absolute path.");
        }
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO}
     *
     * @param settings
     *            Node settings.
     * @throws InvalidSettingsException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_projectId.loadSettingsFrom(settings);
        m_workingDirectory.loadSettingsFrom(settings);
        m_normalizePaths.loadSettingsFrom(settings);
        m_connectionTimeout.loadSettingsFrom(settings);
        m_readTimeout.loadSettingsFrom(settings);

        if (settings.containsKey(KEY_BROWSER_PATH_RELATIVE)) {
            m_browserPathRelative.loadSettingsFrom(settings);
        } else {
            m_browserPathRelative.setBooleanValue(false);
        }
    }

    /**
     * @return the projectId settings model
     */
    public SettingsModelString getProjectIdModel() {
        return m_projectId;
    }

    /**
     * @return the projectId
     */
    public String getProjectId() {
        return m_projectId.getStringValue();
    }

    /**
     * @return the workingDirectory model
     */
    public SettingsModelString getWorkingDirectoryModel() {
        return m_workingDirectory;
    }

    /**
     * @return the workingDirectory
     */
    public String getWorkingDirectory() {
        return m_workingDirectory.getStringValue();
    }

    /**
     * @return the normalizePaths model
     */
    public SettingsModelBoolean getNormalizePathsModel() {
        return m_normalizePaths;
    }

    /**
     * @return the normalizePaths
     */
    public boolean getNormalizePaths() {
        return m_normalizePaths.getBooleanValue();
    }

    /**
     * @return the connectionTimeout model
     */
    public SettingsModelIntegerBounded getConnectionTimeoutModel() {
        return m_connectionTimeout;
    }

    /**
     * @return the connectionTimeout
     */
    public int getConnectionTimeout() {
        return m_connectionTimeout.getIntValue();
    }

    /**
     * @return the readTimeout model
     */
    public SettingsModelIntegerBounded getReadTimeoutModel() {
        return m_readTimeout;
    }

    /**
     * @return the readTimeout
     */
    public int getReadTimeout() {
        return m_readTimeout.getIntValue();
    }

    /**
     * @return the browserPathRelative model
     */
    public SettingsModelBoolean getBrowserPathRelativeModel() {
        return m_browserPathRelative;
    }

    /**
     * @return the browser relativization behavior
     */
    public BrowserRelativizationBehavior getBrowserRelativizationBehavior() {
        if (m_browserPathRelative.getBooleanValue()) {
            return BrowserRelativizationBehavior.RELATIVE;
        } else {
            return BrowserRelativizationBehavior.ABSOLUTE;
        }
    }

    /**
     *
     * @param con
     * @return The FSConnectionConfig for Cloud Storage
     */
    public CloudStorageConnectionConfig toFSConnectionConfig(final GoogleApiConnection con) {
        return toFSConnectionConfig(con, getBrowserRelativizationBehavior());
    }

    /**
     * @param con
     *            The Google Api Connection.
     * @return The FSConnectionConfig to be used with Working directory chooser
     */
    public CloudStorageConnectionConfig toFSConnectionConfigForWorkdirChooser(final GoogleApiConnection con) {
        return toFSConnectionConfig(con, BrowserRelativizationBehavior.ABSOLUTE);
    }

    private CloudStorageConnectionConfig toFSConnectionConfig(final GoogleApiConnection con,
            final BrowserRelativizationBehavior relativizationBehavior) {
        CloudStorageConnectionConfig config = new CloudStorageConnectionConfig(getWorkingDirectory(),
                relativizationBehavior, con);
        config.setConnectionTimeOut(Duration.ofSeconds(getConnectionTimeout()));
        config.setReadTimeOut(Duration.ofSeconds(getReadTimeout()));
        config.setNormalizePaths(getNormalizePaths());
        config.setProjectId(getProjectId());
        return config;
    }
}
