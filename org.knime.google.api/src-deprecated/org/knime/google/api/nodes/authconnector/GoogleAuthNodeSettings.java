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

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;
import org.knime.google.api.clientsecrets.ClientSecrets;
import org.knime.google.api.nodes.authconnector.stores.FileUserCredentialsStore;
import org.knime.google.api.nodes.authconnector.stores.MemoryUserCredentialsStore;
import org.knime.google.api.nodes.authconnector.stores.StringSerializedCredentialStore;
import org.knime.google.api.nodes.authenticator.AbstractUserCredentialStore;
import org.knime.google.api.nodes.util.PathUtil;
import org.knime.google.api.scopes.KnimeGoogleAuthScope;
import org.knime.google.api.scopes.KnimeGoogleAuthScopeRegistry;

import com.google.auth.oauth2.UserCredentials;

/**
 * Settings for the GoogleAuthModel
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
final class GoogleAuthNodeSettings {

    private static final String CONFIG_NAME = "googleAuthentication";

    private static final String CREDENTIAL_LOCATION_TYPE = "credentialLocationType";

    private static final String CREDENTIAL_FILE_LOCATION = "credentialLocation";

    private static final String CREDENTIAL_IN_NODE = "credentialByteFile";

    private static final String KNIME_SCOPES = "knimeScopes";

    private static final String ALL_SCOPES = "allScopes";

    private static final String IS_AUTHENTICATED = "isAuthenticated";

    private static final String ACCESS_TOKEN_HASH = "accessTokenHash";

    private static final String USE_CUSTOM_CLIENT_ID = "useCustomClientId";

    private static final String CUSTOM_CLIENT_ID_FILE = "customClientIdFile";

    private final SettingsModelString m_credentialFilesystemLocation;

    private GoogleAuthLocationType m_type = GoogleAuthLocationType.MEMORY;

    private List<KnimeGoogleAuthScope> m_knimeGoogleAuthScopes = new ArrayList<>();

    private boolean m_useAllscopes = false;

    /**
     * If {@link #m_type} is {@link GoogleAuthLocationType#NODE} then this holds a serialized
     */
    private String m_serializedCredential;

    private boolean m_isAuthenticated;

    private int m_accessTokenHash = "".hashCode();

    private final SettingsModelBoolean m_useCustomClientId;

    private final SettingsModelString m_customClientIdFile;

    /**
     *
     */
    GoogleAuthNodeSettings() {
        m_credentialFilesystemLocation = new SettingsModelString(CREDENTIAL_FILE_LOCATION, "${user.home}/knime");
        m_useCustomClientId = new SettingsModelBoolean(USE_CUSTOM_CLIENT_ID, false);
        m_customClientIdFile = new SettingsModelString(CUSTOM_CLIENT_ID_FILE, "");
        // see AP-13792 -- Google Analytics needs to be disabled by default.
        m_knimeGoogleAuthScopes = KnimeGoogleAuthScopeRegistry.getInstance().getKnimeGoogleAuthScopes().stream()
            .filter(s -> StringUtils.containsAny(s.getAuthScopeName().toLowerCase(), "sheets", "drive"))
            .collect(Collectors.toList());

        m_useCustomClientId
            .addChangeListener(e -> m_customClientIdFile.setEnabled(m_useCustomClientId.getBooleanValue()));
    }

    /**
     * @return The {@link SettingsModelString} for the credential filesystem location
     */
    SettingsModelString getCredentialFilesystemLocationModel() {
        return m_credentialFilesystemLocation;
    }

    String getCredentialFilesystemLocation() {
        return m_credentialFilesystemLocation.getStringValue();
    }

    void clearSerializedCredential() {
        m_serializedCredential = null;
    }

    void saveSettingsTo(final NodeSettingsWO settings) {
        // save authentication
        if (getCredentialLocationType() != GoogleAuthLocationType.NODE) {
            clearSerializedCredential();
        }

        Config config = settings.addConfig(CONFIG_NAME);
        config.addString(CREDENTIAL_IN_NODE, m_serializedCredential);
        config.addString(CREDENTIAL_LOCATION_TYPE, m_type.name());
        KnimeGoogleAuthScopeRegistry.getInstance();
        config.addStringArray(KNIME_SCOPES,
            KnimeGoogleAuthScopeRegistry.getKnimeGoogleScopeIds(m_knimeGoogleAuthScopes));
        config.addBoolean(ALL_SCOPES, m_useAllscopes);
        config.addBoolean(IS_AUTHENTICATED, m_isAuthenticated);
        config.addInt(ACCESS_TOKEN_HASH, m_accessTokenHash);

        m_credentialFilesystemLocation.saveSettingsTo(settings);
        m_useCustomClientId.saveSettingsTo(settings);
        m_customClientIdFile.saveSettingsTo(settings);

    }

    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config = settings.getConfig(CONFIG_NAME);

        config.getString(CREDENTIAL_IN_NODE);
        config.getString(CREDENTIAL_LOCATION_TYPE);
        config.getStringArray(KNIME_SCOPES);
        config.getBoolean(ALL_SCOPES);
        config.getBoolean(IS_AUTHENTICATED);
        config.getInt(ACCESS_TOKEN_HASH);
        m_credentialFilesystemLocation.validateSettings(settings);

        if (settings.containsKey(USE_CUSTOM_CLIENT_ID)) {
            m_useCustomClientId.validateSettings(settings);
            m_customClientIdFile.validateSettings(settings);
        }
    }

    void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config = settings.getConfig(CONFIG_NAME);
        m_serializedCredential = config.getString(CREDENTIAL_IN_NODE);
        m_type = GoogleAuthLocationType.get(config.getString(CREDENTIAL_LOCATION_TYPE));
        m_knimeGoogleAuthScopes = KnimeGoogleAuthScopeRegistry.getInstance()
            .getScopesFromString(Arrays.asList(config.getStringArray(KNIME_SCOPES)));
        m_useAllscopes = config.getBoolean(ALL_SCOPES);
        m_isAuthenticated = config.getBoolean(IS_AUTHENTICATED);
        m_accessTokenHash = config.getInt(ACCESS_TOKEN_HASH);
        m_credentialFilesystemLocation.loadSettingsFrom(settings);

        if (settings.containsKey(USE_CUSTOM_CLIENT_ID)) {
            m_useCustomClientId.loadSettingsFrom(settings);
            m_customClientIdFile.loadSettingsFrom(settings);
        } else {
            m_useCustomClientId.setBooleanValue(false);
            m_customClientIdFile.setStringValue("");
        }
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
     * Returns the credential be stored in the node.
     *
     * @return The stored credential as a string of base64-encoded bytes
     */
    String getSerializedCredential() {
        return m_serializedCredential;
    }

    /**
     * Sets the credential be stored in the node.
     *
     * @param serializedCredential The credential as a string of base64-encoded bytes.
     */
    void setSerializedCredential(final String serializedCredential) {
        m_serializedCredential = serializedCredential;
    }

    void setAccessTokenHash(final int credentialHash) {
        m_accessTokenHash = credentialHash;
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
     * @return the useCustomClientId model
     */
    public SettingsModelBoolean getUseCustomClientIdModel() {
        return m_useCustomClientId;
    }

    /**
     * @return the customClientIdFile model
     */
    SettingsModelString getCustomClientIdFileModel() {
        return m_customClientIdFile;
    }

    String getCustomClientIdLocation() {
        return m_customClientIdFile.getStringValue();
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    void validate() throws InvalidSettingsException {
        if (m_useCustomClientId.getBooleanValue()) {
            if (StringUtils.isBlank(m_customClientIdFile.getStringValue())) {
                throw new InvalidSettingsException("Client ID file is not specified");
            }
        } else {
            final Optional<KnimeGoogleAuthScope> scope = getRelevantKnimeAuthScopes().stream() //
                .filter(KnimeGoogleAuthScope::isCustomClientIdRequired) //
                .findFirst();

            if (scope.isPresent()) {
                throw new InvalidSettingsException(
                    "Custom Client ID is required by the scope: " + scope.get().getAuthScopeName());
            }
        }
    }


    void validateOnConfigure() throws InvalidSettingsException {
        validate();

        if (!isAuthenticated()) {
            throw new InvalidSettingsException("Please authenticate using the node dialog.");
        }
    }

    void validateOnExecute() throws Exception { // NOSONAR
        // validate the credential storage location (if chosen)
        if (getCredentialLocationType() == GoogleAuthLocationType.FILESYSTEM) {
            final var localPath = PathUtil.resolveToLocalPath(getCredentialFilesystemLocation());
            if (!Files.isDirectory(localPath)) {
                throw new IOException("Cannot access folder with stored credentials: " + getCustomClientIdLocation());
            }
        }

        // validate the client secrets file (if chosen)
        if (m_useCustomClientId.getBooleanValue()) {
            final var localPath = PathUtil.resolveToLocalPath(getCustomClientIdLocation());
            if (!Files.isRegularFile(localPath)) {
                throw new IOException("Cannot access client secrets file: " + getCustomClientIdLocation());
            }
        }
    }

    UserCredentials getUserCredentials() throws Exception { // NOSONAR

        final var credentials = createUserCredentialsStore()//
            .tryLoadExistingCredentials()//
            .orElseThrow(
                () -> new IOException("Failed to retrieve credential. Please authenticate using the node dialog."));

        CheckUtils.checkSetting(credentials.getAccessToken().getTokenValue().hashCode() == m_accessTokenHash, //
            "Credentials changed. Please authenticate using the node dialog.");

        return credentials;
    }

    AbstractUserCredentialStore createUserCredentialsStore() throws IOException, InvalidSettingsException {

        final var clientSecrets = m_useCustomClientId.getBooleanValue() //
            ? ClientSecrets.loadClientSecrets(PathUtil.resolveToLocalPath(getCustomClientIdLocation()))//
            : ClientSecrets.loadDefaultClientSecrets();

        final var scopes = KnimeGoogleAuthScopeRegistry.getAuthScopes(getRelevantKnimeAuthScopes());

        return switch (m_type) {
            case MEMORY -> new MemoryUserCredentialsStore(clientSecrets, scopes);
            case FILESYSTEM -> new FileUserCredentialsStore(//
                PathUtil.resolveToLocalPath(getCredentialFilesystemLocation()),//
                clientSecrets,//
                scopes);
            case NODE -> new StringSerializedCredentialStore(m_serializedCredential, clientSecrets, scopes);
            default -> throw new IllegalStateException();
        };
    }
}
