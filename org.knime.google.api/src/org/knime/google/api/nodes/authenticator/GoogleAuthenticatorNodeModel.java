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
 *   Aug 6, 2023 (Alexander Bondaletov, Redfield SE): created
 */
package org.knime.google.api.nodes.authenticator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.credentials.base.Credential;
import org.knime.credentials.base.CredentialCache;
import org.knime.credentials.base.node.AuthenticatorNodeModel;
import org.knime.credentials.base.oauth.api.AccessTokenCredential;

import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.http.GenericUrl;

/**
 * The Google Authenticator node. Performs OAuth authentication to selected Google services and produces
 * {@link AccessTokenCredential}.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class GoogleAuthenticatorNodeModel extends AuthenticatorNodeModel<GoogleAuthenticatorSettings> {

    private static final String LOGIN_FIRST_ERROR = "Please use the configuration dialog to log in first.";

    private GoogleCredentialHolder m_tokenHolder;

    /**
     * @param configuration The node configuration.
     */
    protected GoogleAuthenticatorNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, GoogleAuthenticatorSettings.class);
    }

    @Override
    protected void validateOnConfigure(final PortObjectSpec[] inSpecs, final GoogleAuthenticatorSettings settings)
        throws InvalidSettingsException {
        settings.validate();

        if (settings.m_tokenCacheKey == null) {
            throw new InvalidSettingsException(LOGIN_FIRST_ERROR);
        } else {
            m_tokenHolder = CredentialCache.<GoogleCredentialHolder> get(settings.m_tokenCacheKey)//
                .orElseThrow(() -> new InvalidSettingsException(LOGIN_FIRST_ERROR));
        }
    }

    @Override
    protected Credential createCredential(final PortObject[] inObjects, final ExecutionContext exec,
        final GoogleAuthenticatorSettings settings) throws Exception {
        return fromGoogleToken(m_tokenHolder.m_token);
    }

    private Credential fromGoogleToken(final com.google.api.client.auth.oauth2.Credential token) {
        var accessToken = token.getAccessToken();
        var refreshToken = token.getRefreshToken();
        var expiresAfter = Optional.ofNullable(token.getExpiresInSeconds())//
            .map(secs -> Instant.now().plusSeconds(secs))//
            .orElse(null);
        var tokenType = "Bearer";

        return new AccessTokenCredential(accessToken, //
            refreshToken, //
            expiresAfter, //
            tokenType, //
            createTokenRefresher());

    }

    @SuppressWarnings("unchecked")
    private <T extends Credential> Function<String, T> createTokenRefresher() {
        var transport = m_tokenHolder.m_token.getTransport();
        var jsonFactory = m_tokenHolder.m_token.getJsonFactory();
        var tokenServerEncodedUrl = m_tokenHolder.m_token.getTokenServerEncodedUrl();
        var clientAuthentication = m_tokenHolder.m_token.getClientAuthentication();
        var requestInitializer = m_tokenHolder.m_token.getRequestInitializer();
        var accessMethod = m_tokenHolder.m_token.getMethod();

        return refreshToken -> {//NOSONAR
            var request =
                new RefreshTokenRequest(transport, jsonFactory, new GenericUrl(tokenServerEncodedUrl), refreshToken)
                    .setClientAuthentication(clientAuthentication) //
                    .setRequestInitializer(requestInitializer);

            try {
                var response = request.execute();
                var googleToken = new com.google.api.client.auth.oauth2.Credential(accessMethod);
                googleToken.setFromTokenResponse(response);
                return (T)fromGoogleToken(googleToken);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    @Override
    protected void onDisposeInternal() {
        // dispose of the google token that was retrieved interactively in the node
        // dialog
        if (m_tokenHolder != null) {
            CredentialCache.delete(m_tokenHolder.m_cacheKey);
            m_tokenHolder = null;
        }
    }
}
