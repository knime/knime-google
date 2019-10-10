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
 *   Oct 8, 2018 (oole): created
 */
package org.knime.google.api.nodes.authconnector.auth;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Base64;
import java.util.List;

import org.knime.core.util.FileUtil;
import org.knime.google.api.analytics.nodes.connector.GoogleAnalyticsConnectorFactory;
import org.knime.google.api.data.GoogleApiConnection.OAuthClient;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScope;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScopeRegistry;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;

/**
 * Class that handles the authentication with Google Services.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class GoogleAuthentication {

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** The name of the client secret JSON file **/
    private static final String CLIENT_SECRET = "knime_client_secret-ap_3_7_0.json";

    private static final String DEFAULT_KNIME_USER = "knimeGoogleUser";

    private static volatile OAuthClient m_oAuthClient;

    /**
     * Gets the KNIME OAuth client application information.
     *
     * @return an {@link OAuthClient} object.
     * @throws IOException if an I/O error occurs.
     */
    public static OAuthClient getOAuthClient() throws IOException {
        OAuthClient client = m_oAuthClient;
        if (client == null) {
            synchronized (GoogleAuthentication.class) {
                client = m_oAuthClient;
                if (client == null) {
                    client = new OAuthClient(loadClientSecrets().getDetails());
                    m_oAuthClient = client;
                }
            }
        }
        return client;
    }

    /**
     * If the user is already authenticated the corresponding credentials will be return, otherwise an authorization
     * pop-up will open which asks the user to authenticate with the provided scopes.
     *
     * @param type The {@link GoogleAuthLocationType} used to store the credentials
     * @param credentialPath The path to the credentials
     * @param scopes The scopes which should be asked for authorization
     * @return The authenticated credentials
     * @throws IOException
     */
    public static Credential getCredential(final GoogleAuthLocationType type, final String credentialPath,
        final List<KnimeGoogleAuthScope> scopes) throws IOException {
        DataStoreFactory credentialDataStoreFactory = getCredentialDataStoreFactory(type, credentialPath);
        final GoogleAuthorizationCodeFlow flow = getAuthorizationCodeFlow(credentialDataStoreFactory, scopes);
        Credential credential = flow.loadCredential(DEFAULT_KNIME_USER);
        if (credential == null) {
            credential =
                new CustomAuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(DEFAULT_KNIME_USER);
        }
        return credential;
    }

    /**
     * Returns credentials for an already authenticated user, or NULL if there is no authentication present.
     *
     * @param type The {@link GoogleAuthLocationType}
     * @param credentialPath The credentials path
     * @param scopes The scopes that should be used with the credentials
     * @return authorized credentials if they are available
     * @throws IOException
     */
    public static Credential getNoAuthCredential(final GoogleAuthLocationType type, final String credentialPath,
        final List<KnimeGoogleAuthScope> scopes) throws IOException {
        DataStoreFactory credentialDataStoreFactory = getCredentialDataStoreFactory(type, credentialPath);
        final GoogleAuthorizationCodeFlow flow = getAuthorizationCodeFlow(credentialDataStoreFactory, scopes);
        Credential credential = flow.loadCredential(DEFAULT_KNIME_USER);
        return credential;
    }

    /**
     * Returns the {@link DataStoreFactory} corresponding to the given {@link GoogleAuthLocationType}.
     *
     * @param locationType The {@link GoogleAuthLocationType} for which the {@link DataStoreFactory} should be returned
     * @param credentialPath The credential path for appropriate credential location types
     * @return The {@link DataStoreFactory} corresponding to the given {@link GoogleAuthLocationType} and path
     * @throws IOException If the {@link DataStoreFactory} cannot be created
     */
    private static DataStoreFactory getCredentialDataStoreFactory(final GoogleAuthLocationType locationType,
        final String credentialPath) throws IOException {
        DataStoreFactory credentialDataStoreFactory = null;
        switch (locationType) {
            case MEMORY:
                credentialDataStoreFactory = MemoryDataStoreFactory.getDefaultInstance();
                break;
            case NODE:
                credentialDataStoreFactory = new FileDataStoreFactory(new File(credentialPath));
                break;
            case FILESYSTEM:
                try {
                    credentialDataStoreFactory = new FileDataStoreFactory(
                        new File(FileUtil.resolveToPath(FileUtil.toURL(credentialPath)).toString()));
                } catch (InvalidPathException | URISyntaxException e) {
                    throw new IOException("Credential path could not be processed");
                }
                break;
        }
        return credentialDataStoreFactory;
    }

    /**
     * Builds the {@link GoogleAuthorizationCodeFlow}, which is responsible for open the authorization pop-up.
     *
     *
     * @param credentialDataStoreFactory The {@link DataStoreFactory} that should be used
     * @param scopes The scopes for which the flow should request authorization
     * @return Returns the {@link GoogleAnalyticsConnectorFactory} for the given {@link DataStoreFactory} and scopes
     * @throws IOException If there is a problem accessing the {@link DataStoreFactory}
     */
    private static GoogleAuthorizationCodeFlow getAuthorizationCodeFlow(
        final DataStoreFactory credentialDataStoreFactory, final List<KnimeGoogleAuthScope> knimeScopes)
        throws IOException {
        final GoogleClientSecrets clientSecrets = loadClientSecrets();
        KnimeGoogleAuthScopeRegistry.getInstance();
        final List<String> scopes = KnimeGoogleAuthScopeRegistry.getAuthScopes(knimeScopes);
        return new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes)
            .setDataStoreFactory(credentialDataStoreFactory).setAccessType("offline").build();
    }

    /**
     * Removes the credentials from the given {@link GoogleAuthLocationType}.
     *
     * @param type The {@link GoogleAuthLocationType} for which the credentials should be removed
     * @param credentialPath The path to the credentials that should be removed
     * @throws IOException If the credential location cannot be accessed
     */
    public static void removeCredential(final GoogleAuthLocationType type, final String credentialPath)
        throws IOException {
        DataStoreFactory credentialDataStoreFactory = getCredentialDataStoreFactory(type, credentialPath);
        credentialDataStoreFactory.getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID).delete(DEFAULT_KNIME_USER);
    }

    /**
     * Decodes a given byte String to a given folder.
     *
     * @param tempFolder The temp folder that should be populated with the decoded byte string
     * @param credentialByteString The byte string that should be decoded to the given folder
     * @throws IOException If there is an error when writing the temp file
     */
    public static void createTempFromByteFile(final File tempFolder, final String credentialByteString)
        throws IOException {
        byte[] decodeBase64 = Base64.getDecoder().decode(credentialByteString);
        Files.write(new File(tempFolder, StoredCredential.DEFAULT_DATA_STORE_ID).toPath(), decodeBase64);
    }

    /**
     * Create a temporary file from a given byte string and returns it's location.
     *
     * @param credentialByteString The byte string that should be written to a temporary folder.
     * @return The credential location
     * @throws IOException If temporary folder cannot be created
     */
    public static String getTempCredentialPath(final String credentialByteString) throws IOException {
        File tempFolder = FileUtil.createTempDir("sheets");
        createTempFromByteFile(tempFolder, credentialByteString);
        return tempFolder.getPath();
    }

    /**
     * Returns the encoded storage credential from the given path
     *
     * @param tempfolder The path of the Credentials
     * @return The base64 encoded file storage credential file from the given folder
     * @throws IOException If the path cannot be read
     */
    public static String getByteStringFromFile(final String tempfolder) throws IOException {
        return Base64.getEncoder()
            .encodeToString(Files.readAllBytes(new File(tempfolder, StoredCredential.DEFAULT_DATA_STORE_ID).toPath()));
    }

    private static GoogleClientSecrets loadClientSecrets() throws IOException {
        try (final InputStream in = GoogleAuthentication.class.getResourceAsStream(CLIENT_SECRET)) {
            return GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        }
    }

}
