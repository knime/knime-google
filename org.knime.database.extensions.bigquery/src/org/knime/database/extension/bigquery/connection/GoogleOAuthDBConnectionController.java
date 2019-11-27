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
 */
package org.knime.database.extension.bigquery.connection;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.database.VariableContext;
import org.knime.database.attribute.AttributeValueRepository;
import org.knime.database.connection.UrlDBConnectionController;
import org.knime.database.extension.bigquery.BigQueryProject;
import org.knime.google.api.data.GoogleApiConnection;
import org.knime.google.api.data.GoogleApiConnection.OAuthClient;
import org.knime.google.api.data.GoogleApiConnection.OAuthCredentials;
import org.knime.google.api.data.GoogleApiConnection.ServiceAccountOAuthCredentials;
import org.knime.google.api.data.GoogleApiConnection.ServiceAccountOAuthCredentials.CredentialsFileType;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;

/**
 * Database connection management controller, based on Google OAuth access delegation information and the configuration
 * of the Simba JDBC Driver for Google BigQuery.
 *
 * @author Noemi Balassa
 */
public class GoogleOAuthDBConnectionController extends UrlDBConnectionController {

    private static final String CFG_KEY_PATH = "keyPath";

    private static final String CFG_KEY_TYPE = "keyType";

    private static final String CFG_OAUTH = "OAuth";

    private static final String CFG_OAUTH_TYPE = "oAuthType";

    private static final String CFG_PROJECT_ID = "projectId";

    private static final String CFG_REFRESH_TOKEN = "refreshToken";

    private static final String CFG_SERVICE_ACCOUNT_ID = "serviceAccountId";

    private static final String JDBC_PROPERTY_KEY_OAUTH_PRIVATE_KEY_PATH = "OAuthPvtKeyPath";

    private static final String JDBC_PROPERTY_KEY_OAUTH_SERVICE_ACCOUNT_EMAIL = "OAuthServiceAcctEmail";

    private static final String JDBC_PROPERTY_KEY_OAUTH_TYPE = "OAuthType";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GoogleOAuthDBConnectionController.class);

    private static final String MESSAGE_USER_OAUTH_SETTINGS_BEING_USED =
        "The value of the \"" + JDBC_PROPERTY_KEY_OAUTH_TYPE + "\" JDBC parameter has been explicitly specified."
            + " Only the user-provided settings are used for authentication.";

    private static final String OAUTH_TYPE_APPLICATION_DEFAULT_CREDENTIALS = "3";

    private static final String OAUTH_TYPE_PRE_GENERATED_TOKENS = "2";

    private static final String OAUTH_TYPE_SERVICE_ACCOUNT = "0";

    private static final String OAUTH_TYPE_USER = "1";

    private static final char[] PASSWORD = {'n', 'o', 't', 'a', 's', 'e', 'c', 'r', 'e', 't'};

    private static void addNonNull(final NodeSettingsWO settings, final String key, final String value) {
        requireNonNull(settings, "settings");
        if (value != null) {
            settings.addString(key, value);
        }
    }

    private static Optional<Credentials> createServiceAccountCredentials(final String keyPath,
        final String serviceAccountId) {
        CredentialsFileType keyType = null;
        final int lastDotIndex = keyPath == null ? -1 : keyPath.lastIndexOf('.');
        if (lastDotIndex > -1) {
            @SuppressWarnings("null") // The last condition ensures that keyPath is not null.
            final String fileExtension = keyPath.substring(lastDotIndex + 1);
            if (CredentialsFileType.JSON.name().equalsIgnoreCase(fileExtension)) {
                keyType = CredentialsFileType.JSON;
            } else if (CredentialsFileType.P12.name().equalsIgnoreCase(fileExtension)) {
                keyType = CredentialsFileType.P12;
            } else {
                LOGGER.error("The found file extension does not match any of the expected credentials files ("
                    + CredentialsFileType.JSON.name() + ", " + CredentialsFileType.P12.name() + "): \"" + fileExtension
                    + '"');
                return Optional.empty();
            }
        }
        if (keyType == null) {
            LOGGER.error("The credentials file type could not be determined.");
            return Optional.empty();
        }
        return createServiceAccountCredentials(keyPath, keyType, serviceAccountId);
    }

    private static Optional<Credentials> createServiceAccountCredentials(final String keyPath,
        final CredentialsFileType keyType, final String serviceAccountId) {
        try {
            switch (requireNonNull(keyType, "keyType")) {
                case JSON:
                    try (InputStream stream = Files.newInputStream(Paths.get(keyPath))) {
                        return Optional.of(ServiceAccountCredentials.fromStream(stream));
                    }
                case P12:
                    final KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    try (InputStream stream = Files.newInputStream(Paths.get(keyPath))) {
                        keyStore.load(stream, PASSWORD);
                    }
                    return Optional.of(ServiceAccountCredentials.newBuilder().setClientEmail(serviceAccountId)
                        .setPrivateKey((PrivateKey)keyStore.getKey("privatekey", PASSWORD)).build());
                default:
                    LOGGER
                        .error("The service account credentials could not be obtained. Unknown credentials file type: "
                            + keyType);
                    return Optional.empty();
            }
        } catch (GeneralSecurityException | IOException | RuntimeException exception) {
            LOGGER.error("The service account credentials could not be obtained.", exception);
            return Optional.empty();
        }
    }

    private static Optional<Credentials> createUserCredentials(final String refreshToken, final OAuthClient client) {
        return Optional.of(UserCredentials.newBuilder() // Forced line break.
            .setClientId(client.getId()) // Forced line break.
            .setClientSecret(client.getSecret()) // Forced line break.
            .setRefreshToken(refreshToken) // Forced line break.
            .build());
    }

    private static OAuthClient getOAuthClient() throws SQLException {
        try {
            return GoogleApiConnection.getOAuthClient();
        } catch (final IOException exception) {
            throw new SQLException(exception.getMessage(), exception);
        }
    }

    private static String emptyIfNull(final String string) {
        return string == null ? "" : string;
    }

    private volatile Optional<? extends Credentials> m_credentials = Optional.empty();

    private final String m_keyPath;

    private final CredentialsFileType m_keyType;

    private final String m_oAuthType;

    private volatile Optional<? extends BigQueryProject> m_project = Optional.empty();

    private final Optional<? extends BigQueryProject> m_projectDelegate = Optional.of(new BigQueryProject() {
        @Override
        public String getId() {
            return m_projectId;
        }
    });

    private final String m_projectId;

    private final String m_refreshToken;

    private final String m_serviceAccountId;

    /**
     * Constructs a {@link GoogleOAuthDBConnectionController} object.
     *
     * @param internalSettings the internal settings to load from.
     * @throws InvalidSettingsException if the settings are not valid.
     */
    public GoogleOAuthDBConnectionController(final NodeSettingsRO internalSettings) throws InvalidSettingsException {
        super(internalSettings);
        final NodeSettingsRO oAuthSettings = internalSettings.getNodeSettings(CFG_OAUTH);
        m_keyPath = oAuthSettings.getString(CFG_KEY_PATH, null);
        final String keyTypeString = oAuthSettings.getString(CFG_KEY_TYPE, null);
        if (keyTypeString == null) {
            m_keyType = m_keyPath == null ? null : CredentialsFileType.JSON;
        } else {
            try {
                m_keyType = CredentialsFileType.valueOf(keyTypeString);
            } catch (final IllegalArgumentException exception) {
                throw new InvalidSettingsException("Invalid credentials file type: \"" + keyTypeString + '"',
                    exception);
            }
        }
        m_oAuthType = oAuthSettings.getString(CFG_OAUTH_TYPE, null);
        m_projectId = oAuthSettings.getString(CFG_PROJECT_ID, null);
        m_refreshToken = oAuthSettings.getString(CFG_REFRESH_TOKEN, null);
        m_serviceAccountId = oAuthSettings.getString(CFG_SERVICE_ACCOUNT_ID, null);
    }

    /**
     * Constructs an {@link GoogleOAuthDBConnectionController} object.
     *
     * @param jdbcUrl the database connection URL as a {@link String}.
     * @param projectId the project ID.
     * @param googleApiConnection the Google API connection.
     * @throws NullPointerException if {@code jdbcUrl} is {@code null}.
     * @throws InvalidSettingsException if the received settings are not valid.
     */
    public GoogleOAuthDBConnectionController(final String jdbcUrl, final String projectId,
        final GoogleApiConnection googleApiConnection) throws InvalidSettingsException {
        super(jdbcUrl);
        m_projectId = requireNonNull(projectId, "projectId");
        if (googleApiConnection == null) {
            m_keyPath = null;
            m_keyType = null;
            m_oAuthType = OAUTH_TYPE_APPLICATION_DEFAULT_CREDENTIALS;
            m_refreshToken = null;
            m_serviceAccountId = null;
            return;
        }
        final OAuthCredentials oAuthCredentials = googleApiConnection.getOAuthCredentials()
            .orElseThrow(() -> new InvalidSettingsException("OAuth credentials are not available."));
        switch (oAuthCredentials.getType()) {
            case APPLICATION_DEFAULT:
                m_keyPath = null;
                m_keyType = null;
                m_oAuthType = OAUTH_TYPE_APPLICATION_DEFAULT_CREDENTIALS;
                m_refreshToken = null;
                m_serviceAccountId = null;
                break;
            case SERVICE_ACCOUNT:
                final ServiceAccountOAuthCredentials serviceAccountOAuthCredentials =
                    oAuthCredentials.asServiceAccountCredentials().get();
                final CredentialsFileType keyFileType = serviceAccountOAuthCredentials.getPrivateKeyType();
                switch (keyFileType) {
                    case JSON:
                    case P12:
                        break;
                    default:
                        throw new InvalidSettingsException(
                            "Only JSON and P12 credentials files are supported. The received file type: "
                                + keyFileType);
                }
                m_keyPath = serviceAccountOAuthCredentials.getPrivateKeyPath();
                m_keyType = keyFileType;
                m_oAuthType = OAUTH_TYPE_SERVICE_ACCOUNT;
                m_refreshToken = null;
                m_serviceAccountId = serviceAccountOAuthCredentials.getServiceAccountEmail();
                break;
            case USER_ACCOUNT:
                throw new InvalidSettingsException("User authentication is not allowed."
                    + " Please use the Google Authentication (API Key) node or application default credentials for"
                    + " authentication with a service account.");
            default:
                throw new InvalidSettingsException("OAuth credentials type: " + oAuthCredentials.getType());
        }
    }

    @Override
    @SuppressWarnings("unchecked") // Checked at run time.
    public <T> Optional<T> getController(final Class<? extends T> controllerType) {
        return requireNonNull(controllerType, "controllerType") == BigQueryProject.class ? (Optional<T>)m_project
            : controllerType == Credentials.class ? (Optional<T>)m_credentials : Optional.empty();
    }

    @Override
    public void saveInternalsTo(final NodeSettingsWO settings) {
        super.saveInternalsTo(settings);
        final NodeSettingsWO oAuthSettings = settings.addNodeSettings(CFG_OAUTH);
        addNonNull(oAuthSettings, CFG_KEY_PATH, m_keyPath);
        if (m_keyPath != null && m_keyType != null && m_keyType != CredentialsFileType.JSON) {
            oAuthSettings.addString(CFG_KEY_TYPE, m_keyType.name());
        }
        addNonNull(oAuthSettings, CFG_OAUTH_TYPE, m_oAuthType);
        addNonNull(oAuthSettings, CFG_PROJECT_ID, m_projectId);
        addNonNull(oAuthSettings, CFG_REFRESH_TOKEN, m_refreshToken);
        addNonNull(oAuthSettings, CFG_SERVICE_ACCOUNT_ID, m_serviceAccountId);
    }

    @Override
    protected Properties prepareJdbcProperties(final AttributeValueRepository attributeValues,
        final VariableContext variableContext, final ExecutionMonitor monitor)
        throws CanceledExecutionException, SQLException {
        if (m_projectId == null) {
            m_project = Optional.empty();
            throw new SQLException("Project ID is not available.");
        }
        m_project = m_projectDelegate;
        final Properties jdbcProperties =
            getDerivableJdbcProperties(attributeValues).getDerivedProperties(variableContext);
        final String userOAuthType = jdbcProperties.getProperty(JDBC_PROPERTY_KEY_OAUTH_TYPE);
        if (userOAuthType != null) {
            switch (userOAuthType) {
                case OAUTH_TYPE_APPLICATION_DEFAULT_CREDENTIALS:
                    if (m_credentials.isPresent()) {
                        m_credentials = Optional.empty();
                    }
                    break;
                case OAUTH_TYPE_PRE_GENERATED_TOKENS:
                    // This authentication method is disabled for security reasons:
                    //final OAuthClient client = getOAuthClient();
                    //jdbcProperties.setProperty("OAuthClientId", client.getId());
                    //jdbcProperties.setProperty("OAuthClientSecret", client.getSecret());
                    //LOGGER.warn("The KNIME client information is used for the OAuth process.");
                    //m_credentials = createUserCredentials(jdbcProperties.getProperty("OAuthRefreshToken"), client);
                    throw new SQLException(
                        "Directly supplied tokens cannot be used. User authentication is not allowed."
                            + " Please use the Google Authentication (API Key) node or application default credentials"
                            + " for authentication with a service account.");
                case OAUTH_TYPE_SERVICE_ACCOUNT:
                    m_credentials = createServiceAccountCredentials(
                        jdbcProperties.getProperty(JDBC_PROPERTY_KEY_OAUTH_PRIVATE_KEY_PATH),
                        jdbcProperties.getProperty(JDBC_PROPERTY_KEY_OAUTH_SERVICE_ACCOUNT_EMAIL));
                    break;
                case OAUTH_TYPE_USER:
                    throw new SQLException("The driver's interactive authentication process cannot be used."
                        + " Please use the Google Authentication node for user authentication.");
                default:
                    throw new SQLException("Unrecognized OAuth type setting: \"" + userOAuthType + '"');
            }
            LOGGER.info(MESSAGE_USER_OAUTH_SETTINGS_BEING_USED);
            return jdbcProperties;
        }
        final String oAuthType = m_oAuthType;
        if (oAuthType == null) {
            throw new SQLException("OAuth type is not available.");
        }
        switch (oAuthType) {
            case OAUTH_TYPE_APPLICATION_DEFAULT_CREDENTIALS:
                if (m_credentials.isPresent()) {
                    m_credentials = Optional.empty();
                }
                break;
            case OAUTH_TYPE_PRE_GENERATED_TOKENS:
                final String refreshToken = emptyIfNull(m_refreshToken);
                jdbcProperties.setProperty("OAuthRefreshToken", refreshToken);
                final OAuthClient client = getOAuthClient();
                jdbcProperties.setProperty("OAuthClientId", client.getId());
                jdbcProperties.setProperty("OAuthClientSecret", client.getSecret());
                m_credentials = createUserCredentials(refreshToken, client);
                break;
            case OAUTH_TYPE_SERVICE_ACCOUNT:
                final String serviceAccountId = emptyIfNull(m_serviceAccountId);
                jdbcProperties.setProperty(JDBC_PROPERTY_KEY_OAUTH_SERVICE_ACCOUNT_EMAIL, serviceAccountId);
                final String keyPath = emptyIfNull(m_keyPath);
                jdbcProperties.setProperty(JDBC_PROPERTY_KEY_OAUTH_PRIVATE_KEY_PATH, keyPath);
                m_credentials = createServiceAccountCredentials(keyPath, m_keyType, serviceAccountId);
                break;
            default:
                throw new SQLException("Unrecognized OAuth type setting: \"" + oAuthType + '"');
        }
        jdbcProperties.setProperty(JDBC_PROPERTY_KEY_OAUTH_TYPE, oAuthType);
        monitor.checkCanceled();
        return jdbcProperties;
    }
}
