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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.FileUtil;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.credentials.base.Credential;
import org.knime.credentials.base.CredentialCache;
import org.knime.credentials.base.GenericTokenHolder;
import org.knime.credentials.base.node.AuthenticatorNodeModel;
import org.knime.credentials.base.oauth.api.AccessTokenCredential;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScopeRegistry;

import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

/**
 * The Google Authenticator node. Performs OAuth authentication to selected Google services and produces
 * {@link AccessTokenCredential}.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class GoogleAuthenticatorNodeModel extends AuthenticatorNodeModel<GoogleAuthenticatorSettings> {

    private static final String LOGIN_FIRST_ERROR = "Please use the configuration dialog to log in first.";

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static final JsonFactory JSON_FACTORY = new GsonFactory();

    private GenericTokenHolder<com.google.api.client.auth.oauth2.Credential> m_tokenHolder;

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

        if (settings.m_authType == GoogleAuthenticatorSettings.AuthType.INTERACTIVE) {
            if (settings.m_tokenCacheKey == null) {
                throw new InvalidSettingsException(LOGIN_FIRST_ERROR);
            } else {
                m_tokenHolder = CredentialCache.<GenericTokenHolder<com.google.api.client.auth.oauth2.Credential>> get(
                    settings.m_tokenCacheKey)//
                    .orElseThrow(() -> new InvalidSettingsException(LOGIN_FIRST_ERROR));
            }
        } else {
            // we have an access token from a previous interactive login -> remove it
            if (m_tokenHolder != null) {
                CredentialCache.delete(m_tokenHolder.getCacheKey());
                m_tokenHolder = null;
            }
        }
    }

    @Override
    protected Credential createCredential(final PortObject[] inObjects, final ExecutionContext exec,
        final GoogleAuthenticatorSettings settings) throws Exception {
        switch (settings.m_authType) {
            case INTERACTIVE:
                return fromGoogleToken(m_tokenHolder.getToken());
            case API_KEY:
                return fromGoogleToken(getTokenUsingAPIKey(settings));
            default:
                throw new IllegalArgumentException("Usupported auth type: " + settings.m_authType);
        }
    }

    private Credential fromGoogleToken(final com.google.api.client.auth.oauth2.Credential token) {
        var accessToken = token.getAccessToken();
        var expiresAfter = Optional.ofNullable(token.getExpiresInSeconds())//
            .map(secs -> Instant.now().plusSeconds(secs))//
            .orElse(null);
        var tokenType = "Bearer";

        return new AccessTokenCredential(accessToken, //
            expiresAfter, //
            tokenType, //
            createTokenRefresher(token));

    }

    @SuppressWarnings("unchecked")
    private <T extends Credential> Supplier<T>
        createTokenRefresher(final com.google.api.client.auth.oauth2.Credential token) {
        var refreshToken = token.getRefreshToken();
        var transport = token.getTransport();
        var jsonFactory = token.getJsonFactory();
        var tokenServerEncodedUrl = token.getTokenServerEncodedUrl();
        var clientAuthentication = token.getClientAuthentication();
        var requestInitializer = token.getRequestInitializer();
        var accessMethod = token.getMethod();

        return () -> {//NOSONAR
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

    private static com.google.api.client.auth.oauth2.Credential
        getTokenUsingAPIKey(final GoogleAuthenticatorSettings settings) throws InvalidSettingsException {

        final var apiKeysettings = settings.m_apiKeySettings;
        final var scopes = KnimeGoogleAuthScopeRegistry.getAuthScopes(settings.getSelectedScopes());

        com.google.api.client.auth.oauth2.Credential credential;

        if (apiKeysettings.m_apiKeyType == APIKeySettings.APIKeyType.JSON) {
            final var path = resolveKeyFilePath(apiKeysettings.m_jsonFile);
            credential = getTokenlUsingJSONKey(path, scopes);
        } else {
            final var path = resolveKeyFilePath(apiKeysettings.m_p12File);
            credential = getTokenUsingP12Key(apiKeysettings.m_serviceAccountEmail, path, scopes);
        }
        try {
            // get access token by refreshing token
            credential.refreshToken();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return credential;
    }

    private static String resolveKeyFilePath(final String keyFilePath) throws InvalidSettingsException {
        String result = keyFilePath;
        try {
            final var keyFileURL = FileUtil.toURL(result);
            final var path = FileUtil.resolveToPath(keyFileURL);
            if (path != null) {
                result = path.toString();
            } else {
                String simpleFileBaseName = FilenameUtils.getBaseName(keyFileURL.getFile()); // /foo/bar.key -> 'bar'
                String fileExtension = FilenameUtils.getExtension(keyFileURL.getFile());     // /foo/bar.key -> 'key'
                final var keyFileTemp = FileUtil.createTempFile(simpleFileBaseName, fileExtension);
                try (InputStream in = FileUtil.openStreamWithTimeout(keyFileURL)) {
                    FileUtils.copyInputStreamToFile(in, keyFileTemp);
                }
                result = keyFileTemp.getAbsolutePath();
            }
        } catch (URISyntaxException | InvalidPathException | IOException e) {
            throw new InvalidSettingsException(e);
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private static com.google.api.client.auth.oauth2.Credential getTokenUsingP12Key(final String serviceAccountEmail,
        final String filePath, final List<String> scopes) throws InvalidSettingsException {
        try {
            return new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT).setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId(serviceAccountEmail).setServiceAccountScopes(scopes)
                    .setServiceAccountPrivateKeyFromP12File(new File(filePath)).build();
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidSettingsException(e);
        }
    }

    @SuppressWarnings("deprecation")
    private static com.google.api.client.auth.oauth2.Credential getTokenlUsingJSONKey(final String filePath,
        final List<String> scopes) throws InvalidSettingsException {
        try (final var inputStream = new FileInputStream(filePath)) {
            return GoogleCredential.fromStream(inputStream, HTTP_TRANSPORT, JSON_FACTORY).createScoped(scopes);
        } catch (IOException e) {
            throw new InvalidSettingsException(e);
        }
    }

    @Override
    protected void onDisposeInternal() {
        // dispose of the google token that was retrieved interactively in the node
        // dialog
        if (m_tokenHolder != null) {
            CredentialCache.delete(m_tokenHolder.getCacheKey());
            m_tokenHolder = null;
        }
    }
}
