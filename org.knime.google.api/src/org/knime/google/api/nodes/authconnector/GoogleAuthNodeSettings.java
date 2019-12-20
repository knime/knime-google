/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   Oct 2, 2018 (oole): created
 */
package org.knime.google.api.nodes.authconnector;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.FileUtil;
import org.knime.google.api.data.GoogleApiConnection;
import org.knime.google.api.nodes.authconnector.auth.GoogleAuthLocationType;
import org.knime.google.api.nodes.authconnector.auth.GoogleAuthentication;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScope;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScopeRegistry;

import com.google.api.client.auth.oauth2.Credential;

/**
 * Settings for the GoogleAuthModel
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
final class GoogleAuthNodeSettings {

    private static final String CONFIG_NAME = "googleAuthentication";

    private static final String CREDENTIAL_LOCATION = "credentialLocation";

    private static final String CREDENTIAL_BYTE_FILE = "credentialByteFile";

    private static final String CREDENTIAL_LOCATION_TYPE = "credentialLocationType";

    private static final String KNIME_SCOPES = "knimeScopes";

    private static final String ALL_SCOPES = "allScopes";

    private static final String IS_AUTHENTICATED = "isAuthenticated";

    private static final String ACCESS_TOKEN_HASH = "accessTokenHash";

    private final SettingsModelString m_credentialFileLocation;

    private GoogleAuthLocationType m_type = GoogleAuthLocationType.MEMORY;

    private List<KnimeGoogleAuthScope> m_knimeGoogleAuthScopes = new ArrayList<>();

    private boolean m_useAllscopes = true;

    private String m_credentialTempFolder;

    private String m_storedCredential;

    private boolean m_isAuthenticated;

    private int m_accessTokenHash = "".hashCode();

    /**
     *
     */
    GoogleAuthNodeSettings() {
        m_credentialFileLocation = new SettingsModelString(CREDENTIAL_LOCATION, "${user.home}/knime");
    }

    /**
     * @return The {@link SettingsModelString} for the credential location
     */
    SettingsModelString getCredentialFileLocationModel() {
        return m_credentialFileLocation;
    }

    /**
     * Returns the credential location.
     *
     * @return The credential location
     * @throws InvalidSettingsException
     */
    String getCredentialLocation() throws InvalidSettingsException {
        String path = null;
        switch (getCredentialLocationType()) {
            case NODE:
                File credentialFolder = null;
                try {
                    credentialFolder = FileUtil.createTempDir("sheets");
                    m_credentialTempFolder = credentialFolder.getPath();

                    // if there area alreay in-node credentials, restore them now.
                    if (m_storedCredential != null) {
                        GoogleAuthentication.createTempFromByteFile(credentialFolder, m_storedCredential);
                    }
                    path = credentialFolder.getPath();
                } catch (IOException e) {
                    throw new InvalidSettingsException("Could not create temporary Credentials file");
                }
                break;
            case FILESYSTEM:
                path = m_credentialFileLocation.getStringValue();
                String resolvedPropertiesValue = StrSubstitutor.replaceSystemProperties(path);

                // Check the path
                StringBuilder errorMessageBuilder = new StringBuilder();
                errorMessageBuilder.append("Not a valid path: \"").append(resolvedPropertiesValue).append("\"");
                if (!Objects.equals(path, resolvedPropertiesValue)) {
                    errorMessageBuilder.append(" (substituted from \"").append(path).append("\")");
                }
                String errorMessage = errorMessageBuilder.toString();
                Path realPath;
                try {
                    realPath = FileUtil.resolveToPath(FileUtil.toURL(resolvedPropertiesValue));
                    if (realPath == null) {
                        throw new InvalidSettingsException(errorMessage);
                    }
                } catch (IOException | URISyntaxException e) {
                    throw new InvalidSettingsException(errorMessage, e);
                }

                path = resolvedPropertiesValue;
            case MEMORY:
                break;
            default:
                break;

        }
        return path;
    }

    void removeInNodeCredentials() {
        m_storedCredential = null;
    }

    void saveSettingsTo(final NodeSettingsWO settings) {
        // save authentication
        if (!getCredentialLocationType().equals(GoogleAuthLocationType.NODE)) {
            removeInNodeCredentials();
        }
        Config config = settings.addConfig(CONFIG_NAME);
        config.addString(CREDENTIAL_BYTE_FILE, m_storedCredential);
        config.addString(CREDENTIAL_LOCATION_TYPE, m_type.name());
        KnimeGoogleAuthScopeRegistry.getInstance();
        config.addStringArray(KNIME_SCOPES,
            KnimeGoogleAuthScopeRegistry.getKnimeGoogleScopeIds(m_knimeGoogleAuthScopes));
        config.addBoolean(ALL_SCOPES, m_useAllscopes);
        config.addBoolean(IS_AUTHENTICATED, m_isAuthenticated);
        config.addInt(ACCESS_TOKEN_HASH, m_accessTokenHash);

        m_credentialFileLocation.saveSettingsTo(settings);

    }

    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config = settings.getConfig(CONFIG_NAME);

        config.getString(CREDENTIAL_BYTE_FILE);
        config.getString(CREDENTIAL_LOCATION_TYPE);
        config.getStringArray(KNIME_SCOPES);
        config.getBoolean(ALL_SCOPES);
        config.getBoolean(IS_AUTHENTICATED);
        config.getInt(ACCESS_TOKEN_HASH);
        m_credentialFileLocation.validateSettings(settings);
    }

    void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config = settings.getConfig(CONFIG_NAME);
        m_storedCredential = config.getString(CREDENTIAL_BYTE_FILE);
        m_type = GoogleAuthLocationType.get(config.getString(CREDENTIAL_LOCATION_TYPE));
        m_knimeGoogleAuthScopes = KnimeGoogleAuthScopeRegistry.getInstance()
            .getScopesFromString(Arrays.asList(config.getStringArray(KNIME_SCOPES)));
        m_useAllscopes = config.getBoolean(ALL_SCOPES);
        m_isAuthenticated = config.getBoolean(IS_AUTHENTICATED);
        m_accessTokenHash = config.getInt(ACCESS_TOKEN_HASH);
        m_credentialFileLocation.loadSettingsFrom(settings);
    }

    GoogleAuthLocationType getCredentialLocationType() {
        return m_type;
    }

    List<KnimeGoogleAuthScope> getSelectedKnimeAuthScopes() {
        return m_knimeGoogleAuthScopes;
    }

    List<KnimeGoogleAuthScope> getRelevantKnimeAuthScopes() {
        if (m_useAllscopes) {
            return KnimeGoogleAuthScopeRegistry.getInstance().getOAuthEnabledKnimeGoogleAuthScopes();
        } else {
            return getSelectedKnimeAuthScopes();
        }
    }

    void setKnimeGoogleAuthScopes(final List<KnimeGoogleAuthScope> authScopes) {
        m_knimeGoogleAuthScopes = authScopes;
    }

    void setCredentialLocationType(final GoogleAuthLocationType type) {
        m_type = type;
    }

    /**
     * Returns the StoredCredential file as a byte string.
     *
     * @return The stored credential as a byte string
     */
    String getEncodedStoredCredential() {
        return m_storedCredential;
    }

    /**
     * @throws IOException
     * @throws URISyntaxException
     */
    void setByteString() throws IOException, URISyntaxException {
        if (m_type.equals(GoogleAuthLocationType.NODE)) {
            m_storedCredential = GoogleAuthentication.getByteStringFromFile(m_credentialTempFolder);
        } else {
            removeInNodeCredentials();
        }
    }

    void setAccessTokenHash(final int credentialHash) {
        m_accessTokenHash = credentialHash;
    }

    /**
     * Has to be loaded from the node dialog
     *
     * @param settings The node settings
     * @param specs The input specs
     * @throws NotConfigurableException
     */
    void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            loadValidatedSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            // okay
        }
    }

    void setAllScopes(final boolean selectAllScopes) {
        m_useAllscopes = selectAllScopes;
    }

    Boolean useAllScopes() {
        return m_useAllscopes;
    }

    void setIsAuthenticated(final Boolean isAuthenticated) {
        m_isAuthenticated = isAuthenticated;
    }

    boolean isAuthenticated() {
        return m_isAuthenticated;
    }

    /**
     * Creates the API connection object, if possible. It will create an empty Optional if the key file lives on a
     * remote server (either KNIME server or http/ftp/scp) and the argument flag is false (use case: during node
     * configuration we shouldn't do expensive I/O).
     *
     * @param isDuringExecute The flag as describe above.
     * @return The spec of this node, containing the GoogleApiConnection for the current configuration, if possible
     * @throws InvalidSettingsException If the current configuration is not valid
     */
    Optional<GoogleApiConnection> createGoogleApiConnection(final boolean isDuringExecute)
        throws InvalidSettingsException {
        try {
            String credentialLocation = getCredentialLocation();
            if (getCredentialLocationType().equals(GoogleAuthLocationType.NODE)
                && !(new File(credentialLocation).exists())) {
                credentialLocation = GoogleAuthentication.getTempCredentialPath(m_storedCredential);
            }
            if (!isDuringExecute) {
                Credential noAuthCredential = GoogleAuthentication.getNoAuthCredential(getCredentialLocationType(), getCredentialLocation(), getRelevantKnimeAuthScopes());
                if (noAuthCredential == null) {
                    throw new InvalidSettingsException("No valid credentials found. Please authenticate using the node dialog.");
                }
                // If the accessTokenHash changes it means that the credentials got overwritten or exchanged,
                // so they should be verified using the node dialog.
                if (noAuthCredential.getAccessToken().hashCode() != m_accessTokenHash) {
                    throw new InvalidSettingsException("Credentials changed. Please authenticate using the node dialog.");
                }
                return Optional.empty();
            }

            return Optional.of(
                new GoogleApiConnection(getCredentialLocationType(), credentialLocation, getRelevantKnimeAuthScopes()));
        } catch (IOException e) {
            throw new InvalidSettingsException(e);
        }
    }
}
