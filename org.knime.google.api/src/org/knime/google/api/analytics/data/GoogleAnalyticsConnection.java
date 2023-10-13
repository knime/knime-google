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
 *   Mar 18, 2014 ("Patrick Winter"): created
 */

package org.knime.google.api.analytics.data;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.google.api.analytics.nodes.connector.GoogleAnalyticsConnectorConfiguration;
import org.knime.google.api.data.GoogleApiConnection;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.model.Account;
import com.google.api.services.analytics.model.Accounts;
import com.google.api.services.analytics.model.Profile;
import com.google.api.services.analytics.model.Profiles;
import com.google.api.services.analytics.model.Webproperties;
import com.google.api.services.analytics.model.Webproperty;
import com.google.auth.http.HttpCredentialsAdapter;

/**
 * A connection to the Google Analytics API.
 *
 * Use the {@link Analytics} object returned by {@link #getAnalytics()} to communicate with the Google Analytics API.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public final class GoogleAnalyticsConnection {

    private static final String ALL_WILDCARD = "~all";

    private static final String CFG_PROFILE_ID = "profileId";

    private static final String CFG_APPLICATION_NAME = "applicationName";

    private static final String CFG_CONNECT_TIMEOUT = "connectTimeout";

    private static final String CFG_READ_TIMEOUT = "readTimeout";

    private final Duration m_connectTimeout;

    private final Duration m_readTimeout;

    private GoogleApiConnection m_connection;

    private String m_profileId;

    private String m_applicationName;

    private Analytics m_analytics;

    /**
     * Retrieves all accounts with their web properties and their profiles from Google Analytics.
     *
     * @param connection Connection to the Google API
     * @param connectTimeout The connection timeout to use when communicating with the API.
     * @param readTimeout The read timeout to use when communicating with the API.
     * @return Map containing the hierarchical structure of accounts, web properties and profiles
     * @throws IOException If an error occurs while retrieving the data
     */
    public static Map<String, Map<String, Map<String, String>>> getAccountsWebpropertiesProfilesMap(
        final GoogleApiConnection connection, final Duration connectTimeout, final Duration readTimeout)
        throws IOException {
        Map<String, Map<String, Map<String, String>>> accountsMap =
                new TreeMap<String, Map<String, Map<String, String>>>();
        Map<String, String> accountIdToName = new HashMap<String, String>();
        Map<String, String> webpropertyIdToName = new HashMap<String, String>();
        final var credAdapter = new HttpCredentialsAdapter(connection.getCredentials());
        Analytics analytics = new Analytics.Builder(GoogleApiConnection.getHttpTransport(),
            GoogleApiConnection.getJsonFactory(), setHttpTimeout(credAdapter, connectTimeout, readTimeout))
                .setApplicationName("KNIME-Profiles-Scan").build();
        Accounts accounts = analytics.management().accounts().list().execute();
        Webproperties webproperties = analytics.management().webproperties().list(ALL_WILDCARD).execute();
        Profiles profiles = analytics.management().profiles().list(ALL_WILDCARD, ALL_WILDCARD).execute();
        for (Account account : accounts.getItems()) {
            accountIdToName.put(account.getId(), account.getName());
            accountsMap.put(account.getName(), new TreeMap<String, Map<String, String>>());
        }
        for (Webproperty webproperty : webproperties.getItems()) {
            webpropertyIdToName.put(webproperty.getId(), webproperty.getName());
            accountsMap.get(accountIdToName.get(webproperty.getAccountId())).put(webproperty.getName(), new TreeMap<String, String>());
        }
        for (Profile profile : profiles.getItems()) {
            accountsMap.get(accountIdToName.get(profile.getAccountId())).get(webpropertyIdToName.get(profile.getWebPropertyId())).put(profile.getName(), profile.getId());
        }
        return accountsMap;
    }

    /**
     * Creates a new {@link GoogleAnalyticsConnection} using the given {@link GoogleApiConnection}.
     *
     * @param connection The used GoogleApiConnection
     * @param applicationName Name of this application as it is shown to the Google API
     * @param profileId ID of the profile that will be used
     * @param connectTimeout The connection timeout to use when communicating with the API.
     * @param readTimeout The read timeout to use when communicating with the API.
     */
    public GoogleAnalyticsConnection(final GoogleApiConnection connection, final String applicationName,
        final String profileId, final Duration connectTimeout, final Duration readTimeout) {
        m_connection = connection;
        m_profileId = profileId;
        m_applicationName = applicationName;
        m_connectTimeout = connectTimeout;
        m_readTimeout = readTimeout;
        final var credAdapter = new HttpCredentialsAdapter(m_connection.getCredentials());
        HttpRequestInitializer credentials = setHttpTimeout(credAdapter, m_connectTimeout, m_readTimeout);
        m_analytics = new Analytics.Builder(GoogleApiConnection.getHttpTransport(),
            GoogleApiConnection.getJsonFactory(), credentials)
                .setApplicationName(m_applicationName).build();
    }

    /**
     * Restores a {@link GoogleAnalyticsConnection} from a saved model (used by the framework).
     *
     * @param model The model containing the connection information
     * @throws InvalidSettingsException If the model was invalid
     */
    public GoogleAnalyticsConnection(final ModelContentRO model) throws InvalidSettingsException {
        try {
            m_connection = new GoogleApiConnection(model);
            m_profileId = model.getString(CFG_PROFILE_ID);
            m_applicationName = model.getString(CFG_APPLICATION_NAME);
            m_connectTimeout = Duration.ofSeconds(model.getInt(CFG_CONNECT_TIMEOUT,
                (int)GoogleAnalyticsConnectorConfiguration.DEFAULT_CONNECT_TIMEOUT.getSeconds()));
            m_readTimeout = Duration.ofSeconds(model.getInt(CFG_READ_TIMEOUT,
                (int)GoogleAnalyticsConnectorConfiguration.DEFAULT_READ_TIMEOUT.getSeconds()));
            final var credAdapter = new HttpCredentialsAdapter(m_connection.getCredentials());
            m_analytics = new Analytics.Builder(GoogleApiConnection.getHttpTransport(),
                GoogleApiConnection.getJsonFactory(), setHttpTimeout(credAdapter, m_connectTimeout, m_readTimeout))
                    .setApplicationName(m_applicationName).build();
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidSettingsException(e);
        }
    }

    /**
     * Wraps the given requestInitalizer with another initializer that sets connect and read timeouts.
     * @param requestInitializer the initializer to wrap
     * @param connectTimeout the connection timeout. A value of {@code 0} means that an infinite timeout will be used.
     * @param readTimeout the read timeout. A value of {@code 0} means that an infinite timeout will be used.
     * @return
     */
    private static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer,
        final Duration connectTimeout, final Duration readTimeout) {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(final HttpRequest request) throws IOException {
                requestInitializer.initialize(request);
                request.setConnectTimeout((int)connectTimeout.toMillis());
                request.setReadTimeout((int)readTimeout.toMillis());
            }
        };
    }

    /**
     * @return The {@link Analytics} object used to communicate with the Google Analytics API
     */
    public Analytics getAnalytics() {
        return m_analytics;
    }

    /**
     * @return The profile ID that should be used
     */
    public String getProfileId() {
        return m_profileId;
    }

    /**
     * @param model The model to save the current configuration in
     */
    public void save(final ModelContentWO model) {
        m_connection.save(model);
        model.addString(CFG_PROFILE_ID, m_profileId);
        model.addString(CFG_APPLICATION_NAME, m_applicationName);
        model.addInt(CFG_CONNECT_TIMEOUT, (int)m_connectTimeout.getSeconds());
        model.addInt(CFG_READ_TIMEOUT, (int)m_readTimeout.getSeconds());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GoogleAnalyticsConnection)) {
            return false;
        }
        GoogleAnalyticsConnection con = (GoogleAnalyticsConnection)obj;
        EqualsBuilder eb = new EqualsBuilder();
        eb.append(m_connection, con.m_connection);
        eb.append(m_profileId, con.m_profileId);
        eb.append(m_applicationName, con.m_applicationName);
        return eb.isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(m_connection);
        hcb.append(m_profileId);
        hcb.append(m_applicationName);
        return hcb.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(m_connection.toString() + "\n");
        sb.append("Profile ID:\n" + m_profileId + "\n\n");
        sb.append("Application name:\n" + m_applicationName + "\n");
        return sb.toString();
    }

}
