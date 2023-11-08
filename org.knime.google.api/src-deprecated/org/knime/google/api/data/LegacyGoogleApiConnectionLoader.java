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
 *   Nov 6, 2023 (bjoern): created
 */
package org.knime.google.api.data;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.credentials.base.Credential;
import org.knime.google.api.analytics.data.GoogleAnalyticsConnectionPortObjectSpec;
import org.knime.google.api.clientsecrets.ClientSecrets;
import org.knime.google.api.credential.GoogleCredential;
import org.knime.google.api.nodes.authconnector.GoogleAuthLocationType;
import org.knime.google.api.nodes.authconnector.stores.FileUserCredentialsStore;
import org.knime.google.api.nodes.authconnector.stores.StringSerializedCredentialStore;
import org.knime.google.api.nodes.util.PathUtil;
import org.knime.google.api.nodes.util.ServiceAccountCredentialsUtil;
import org.knime.google.api.scopes.KnimeGoogleAuthScopeRegistry;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

/**
 * Prior to AP 5.2 there was a class called GoogleApiConnection which saved a whole lot of confidential information from
 * the Google Authenticator nodes. It was saved/restored with {@link GoogleApiConnectionPortObjectSpec} and other
 * downstream ports, such as the {@link GoogleAnalyticsConnectionPortObjectSpec}.
 *
 * This class can load the persisted settings of a GoogleApiConnection and return the resulting
 * {@link GoogleCredentials}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
@SuppressWarnings("deprecation")
public final class LegacyGoogleApiConnectionLoader {

    private static final String CFG_SERVICE_ACCOUNT_EMAIL = "service_account_email";

    private static final String CFG_KEY_FILE_LOCATION = "key_file_location";

    private static final String CFG_SCOPES = "scopes";

    private static final String CFG_CREDENTIAL_LOCATION_TYPE = "credentialLocationType";

    private static final String CFG_CREDENTIAL_LOCATION = "credentialLocation";

    private static final String CFG_STORED_CREDENTIAL = "storedCredential";

    private static final String CFG_KNIME_SCOPES = "knimeScopes";

    private static final String CFG_CLIENT_ID_FILE = "clientIdFile";

    private LegacyGoogleApiConnectionLoader() {
    }

    static boolean canRestoreCredential(final ConfigRO config) {
        // this seems to be the common setting across all variations of a saved
        // GoogleApiConnection
        return config.containsKey(CFG_CREDENTIAL_LOCATION);
    }

    /**
     * Loads the persisted settings of a GoogleApiConnection and creates a {@link Credential} instance from it.
     *
     * @param model {@link ConfigRO} from which to load from.
     * @return an optional with a {@link GoogleCredential} inside, if the GoogleApiConnection settings could be loaded;
     *         empty otherwise.
     * @throws InvalidSettingsException
     */
    public static Optional<GoogleCredential> restoreAsCredential(final ConfigRO model) throws InvalidSettingsException {

        if (!canRestoreCredential(model)) {
            return Optional.empty();
        }

        try {
            final Optional<GoogleCredentials> googleCreds;

            if (model.containsKey(CFG_CREDENTIAL_LOCATION_TYPE)) {
                googleCreds = tryRestoreUserCredentials(model);
            } else {
                googleCreds = Optional.of(restoreServiceAccountCredentials(model));
            }

            return googleCreds.map(GoogleCredential::new);
        } catch (IOException e) {
            throw new InvalidSettingsException("Failed to restore legacy Google API Connection", e);
        }
    }

    private static ServiceAccountCredentials restoreServiceAccountCredentials(final ConfigRO config)
        throws IOException, InvalidSettingsException {

        final var email = config.getString(CFG_SERVICE_ACCOUNT_EMAIL);
        final var keyFilePath = PathUtil.resolveToLocalPath(config.getString(CFG_KEY_FILE_LOCATION));
        final var scopes = config.getStringArray(CFG_SCOPES);

        return (ServiceAccountCredentials)ServiceAccountCredentialsUtil.loadFromP12(email, keyFilePath).createScoped(scopes);

    }

    private static Optional<GoogleCredentials> tryRestoreUserCredentials(final ConfigRO model)
        throws InvalidSettingsException, IOException {

        final var locationType = GoogleAuthLocationType.valueOf(model.getString(CFG_CREDENTIAL_LOCATION_TYPE));
        final var scopes = loadScopes(model);

        final var clientSecrets = model.getString(CFG_CLIENT_ID_FILE, null) != null //
            ? ClientSecrets.loadClientSecrets(PathUtil.resolveToLocalPath(model.getString(CFG_CLIENT_ID_FILE, null)))//
            : ClientSecrets.loadDefaultClientSecrets();

        final var optUserCreds = switch (locationType) {
            case NODE -> new StringSerializedCredentialStore(model.getString(CFG_STORED_CREDENTIAL), //
                clientSecrets, //
                scopes).tryLoadExistingCredentials();
            case FILESYSTEM -> new FileUserCredentialsStore(Paths.get(model.getString(CFG_CREDENTIAL_LOCATION)), //
                clientSecrets, //
                scopes).tryLoadExistingCredentials();
            default -> Optional.empty();
        };

        return optUserCreds.map(GoogleCredentials.class::cast);
    }

    private static List<String> loadScopes(final ConfigRO model) throws InvalidSettingsException {
        // GoogleApiConnection saved both an array of plain string scopes, as well as an array
        // of KnimeGoogleAuthScope values. Since the mapping of KnimeGoogleAuthScope to actual
        // scopes can change over time), we preferably restore the plain string scopes
        if (model.containsKey(CFG_SCOPES)) {
            return Arrays.asList(model.getStringArray(CFG_SCOPES));
        } else if (model.containsKey(CFG_KNIME_SCOPES)) {
            return KnimeGoogleAuthScopeRegistry.getInstance()//
                .getScopesFromString(Arrays.asList(model.getStringArray(CFG_KNIME_SCOPES)))//
                .stream()//
                .flatMap(knimeScope -> knimeScope.getAuthScopes().stream())//
                .distinct()//
                .toList();
        } else {
            throw new InvalidSettingsException("Could not determine scopes");
        }
    }
}
