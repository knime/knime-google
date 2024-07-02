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
 *   Nov 3, 2023 (bjoern): created
 */
package org.knime.google.api.nodes.authenticator;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.knime.google.api.nodes.util.GoogleApiUtil;

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;

/**
 * Abstract superclass to acquire {@link UserCredentials} either via interactive login or from an existing
 * {@link DataStore}. Subclasses can supply their own {@link DataStoreFactory} to handle persistence.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @since 5.2
 */
public abstract class AbstractUserCredentialStore {

    /**
     * User under which to store the credential in the {@link DataStore}.
     */
    protected static final String DEFAULT_KNIME_USER = "knimeGoogleUser";

    /**
     * OAuth2 client id and secret to use when acquiring/refresh an access token.
     */
    protected final GoogleClientSecrets m_clientSecrets;

    /**
     * OAuth2 scopes to request during interactive login.
     */
    protected final List<String> m_scopes;

    /**
     * Constructor.
     *
     * @param clientSecrets OAuth2 client id and secret to use when acquiring/refresh an access token.
     * @param scopes The OAuth2 scopes to request during interactive login.
     */
    protected AbstractUserCredentialStore(final GoogleClientSecrets clientSecrets, final List<String> scopes) {
        m_clientSecrets = Objects.requireNonNull(clientSecrets, "Google client secret must be provided");
        m_scopes = Objects.requireNonNull(scopes, "Scopes must be provided");
    }

    /**
     * First tries to load an existing {@link UserCredentials} from the underlying {@link DataStore}. If that fails, a
     * new interactive login (browser opens) is done.
     *
     * @return a restored or newly acquired {@link UserCredentials} instance.
     * @throws IOException
     */
    public UserCredentials loginInteractively() throws IOException {
        final var dataStoreFactory = createDataStoreFactory();
        final var flow = createAuthorizationFlow(dataStoreFactory);

        // first try to load existing user credentials
        var optCreds = loadUserCredentialsFromFlow(flow);

        // fall back to interactive login if nothing could be loaded
        if (optCreds.isEmpty()) {
            new CustomAuthorizationCodeInstalledApp(flow, new LocalServerReceiver())//
                .authorize(DEFAULT_KNIME_USER);
            optCreds = loadUserCredentialsFromFlow(flow);
        }

        return optCreds.orElseThrow(() -> new IOException("Failed to obtain credentials from interactive login"));
    }

    /**
     * Tries to load a previously stored {@link UserCredentials} instance from the underlying {@link DataStore}.
     *
     * @return a optional with a restored {@link UserCredentials} instance; empty if nothing could be restored.
     * @throws IOException
     */
    public Optional<UserCredentials> tryLoadExistingCredentials() throws IOException {
        final var dataStoreFactory = createDataStoreFactory();
        final var flow = createAuthorizationFlow(dataStoreFactory);

        // first try to load existing user credentials
        return loadUserCredentialsFromFlow(flow);
    }

    /**
     * Removes the (if any) previously stored from the underlying {@link DataStore} so that it cannot be restored anymore.
     */
    public abstract void clear();

    /**
     * Subclasses must implement this method to supply a {@link DataStoreFactory} that will be used to create a
     * {@link DataStore} that is used to persist/restore {@link UserCredentials}.
     *
     * @return a {@link DataStoreFactory}
     * @throws IOException
     */
    protected abstract DataStoreFactory createDataStoreFactory() throws IOException;

    /**
     * Creates a pre-configured {@link GoogleAuthorizationCodeFlow} that can be used for interactive login or restoring
     * a {@link UserCredentials} instance.
     *
     * @param dataStoreFactory
     * @return a new {@link GoogleAuthorizationCodeFlow}
     * @throws IOException
     */
    protected GoogleAuthorizationCodeFlow createAuthorizationFlow(final DataStoreFactory dataStoreFactory)
        throws IOException {

        return new GoogleAuthorizationCodeFlow.Builder(GoogleApiUtil.getHttpTransport(), //
            GoogleApiUtil.getJsonFactory(), //
            m_clientSecrets, //
            m_scopes)//
                .setDataStoreFactory(dataStoreFactory)//
                .setAccessType("offline")//
                .setApprovalPrompt("force")
                .build();
    }

    /**
     * Tries to restore an previously stored {@link UserCredentials} instance.
     *
     * @param flow
     * @return an optionally restored {@link UserCredentials} instance.
     * @throws IOException
     */
    protected Optional<UserCredentials> loadUserCredentialsFromFlow(final GoogleAuthorizationCodeFlow flow)
        throws IOException {

        final var restoredCredential = flow.loadCredential(DEFAULT_KNIME_USER);
        if (restoredCredential == null) {
            return Optional.empty();
        }

        final var expirationTime = Date.from(Instant.ofEpochMilli(restoredCredential.getExpirationTimeMilliseconds()));

        final var accessToken = AccessToken.newBuilder()//
            .setTokenValue(restoredCredential.getAccessToken())//
            .setExpirationTime(expirationTime)//
            .setScopes(m_scopes)//
            .build();

        final var toReturn = UserCredentials.newBuilder()//
            .setAccessToken(accessToken).setRefreshToken(restoredCredential.getRefreshToken())//
            .setClientId(m_clientSecrets.getDetails().getClientId())//
            .setClientSecret(m_clientSecrets.getDetails().getClientSecret())//
            .setHttpTransportFactory(GoogleApiUtil::getHttpTransport)//
            .build();

        return Optional.of(toReturn);
    }
}
