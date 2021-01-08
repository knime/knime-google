/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 19, 2014 ("Patrick Winter"): created
 */
package org.knime.google.api.analytics.nodes.connector;

import java.time.Duration;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.google.api.analytics.data.GoogleAnalyticsConnection;
import org.knime.google.api.data.GoogleApiConnection;

/**
 * Configuration of the GoogleAnalyticsConnector node.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class GoogleAnalyticsConnectorConfiguration {

    private static final String CFG_PROFILE_ID = "profile_id";

    /**
     * Default setting for the API connect timeout.
     */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Default setting for the API read timeout.
     */
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    private Optional<Duration> m_connectTimeout = Optional.empty();

    private Optional<Duration> m_readTimeout = Optional.empty();

    private String m_profileId = "";

    /**
     * @return the profileId
     */
    public String getProfileId() {
        return m_profileId;
    }

    /**
     * @param profileId the profileId to set
     */
    public void setProfileId(final String profileId) {
        m_profileId = profileId;
    }

    /**
     * @param settings The settings object to save in
     */
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_PROFILE_ID, m_profileId);
        m_connectTimeout.ifPresent(duration -> settings.addInt("connectTimeout", (int)duration.getSeconds()));
        m_readTimeout.ifPresent(duration -> settings.addInt("readTimeout", (int)duration.getSeconds()));
    }

    /**
     * @param settings The settings object to load from
     * @throws InvalidSettingsException If the settings are invalid
     */
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_profileId = settings.getString(CFG_PROFILE_ID);
        m_connectTimeout = getDurationFromSettings(settings, "connectTimeout");
        m_readTimeout = getDurationFromSettings(settings, "readTimeout");
    }

    /**
     * @param settings The settings object to load from
     */
    public void loadInDialog(final NodeSettingsRO settings) {
        m_profileId = settings.getString(CFG_PROFILE_ID, "");
        m_connectTimeout = getDurationFromSettings(settings, "connectTimeout");
        m_readTimeout = getDurationFromSettings(settings, "readTimeout");
    }

    private static Optional<Duration> getDurationFromSettings(final NodeSettingsRO settings, final String key) {
        try {
            return Optional.of(Duration.ofSeconds(Math.max(settings.getInt(key), 0)));
        } catch (InvalidSettingsException e) { // NOSONAR
            return Optional.empty();
        }
    }

    /**
     * Create a new {@link GoogleAnalyticsConnection} that uses the timeouts specified in this configuration.
     * @param googleApiConnection The GoogleApiConnection to use
     * @return GoogleAnalyticsConnection based on this configuration
     * @throws InvalidSettingsException If the current configuration is not valid
     */
    public GoogleAnalyticsConnection createGoogleAnalyticsConnection(final GoogleApiConnection googleApiConnection)
            throws InvalidSettingsException {
        if (m_profileId.isEmpty()) {
            throw new InvalidSettingsException("No profile ID selected");
        }
        Duration connectTimeout = m_connectTimeout.orElse(DEFAULT_CONNECT_TIMEOUT);
        Duration readTimeout = m_readTimeout.orElse(DEFAULT_READ_TIMEOUT);
        return new GoogleAnalyticsConnection(googleApiConnection, "KNIME-Google-Analytics-Connector", m_profileId,
            connectTimeout, readTimeout);
    }

    public Optional<Duration> getConnectTimeout() {
        return m_connectTimeout;
    }

    public Optional<Duration> getReadTimeout() {
        return m_readTimeout;
    }

    void setConnectTimeout(final Duration duration) {
        m_connectTimeout = Optional.of(duration);
    }

    void setReadTimeout(final Duration duration) {
        m_readTimeout = Optional.of(duration);
    }
}
