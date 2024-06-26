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
 *   Aug 21, 2017 (oole): created
 */
package org.knime.google.api.sheets.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.knime.core.util.FileUtil;
import org.knime.google.api.clientsecrets.ClientSecrets;
import org.knime.google.api.nodes.authenticator.CustomAuthorizationCodeInstalledApp;
import org.knime.google.api.nodes.util.GoogleApiUtil;
import org.knime.google.api.nodes.util.TimeoutHttpRequestInitializer;
import org.knime.google.api.sheets.nodes.connectorinteractive.GoogleSheetsInteractiveServiceProviderFactory;
import org.knime.google.api.sheets.nodes.connectorinteractive.SettingsModelCredentialLocation.CredentialLocationType;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;

/**
 * This class handles the interactive authentication for google sheets.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
@Deprecated
public class GoogleSheetsInteractiveAuthentication {

    /** The name of the credentials file created by the google API */
    public static final String STORAGE_CREDENTIAL = "StoredCredential";

    /** The scopes required for the {@link GoogleSheetsInteractiveAuthentication} */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_READONLY);



    /**
     * Returns a {@link Sheets} service for the given user from the given application name
     * and the path for the data store file. The authentication is verified.
     *
     * @param locationType The {@link CredentialLocationType} used for authentication
     * @param credentialPath the path for the data store file
     * @param user the user for which the credentials should be used
     * @param connectTimeout Connect timeout for HTTP connections.
     * @param readTimeout Read timeout for HTTP connections.
     * @return A sheets service authenticated for the given user, with credentials read from
     * the given location of the data store file
     * @throws IOException If there is a problem reading the credentials from the data storage file
     */
    public static Sheets getExistingAuthSheetService(final CredentialLocationType locationType,
        final String credentialPath, final String user, final Duration connectTimeout, final Duration readTimeout)
        throws IOException {

        final var credentialDataStoreFactory = getCredentialDataStoreFactory(locationType, credentialPath);
        final var credential =
            getAuthorizationCodeFlow(credentialDataStoreFactory, connectTimeout, readTimeout).loadCredential(user);
        final var service = getSheetService(credential, connectTimeout, readTimeout);
        testService(service);
        return service;
    }

    /**
     * Returns an authenticated Google Drive service. Given the credential path and the user.
     *
     * @param locationType The {@link CredentialLocationType} used for authentication
     * @param credentialPath The path to the credentials or {@link Null}.
     * @param user The user that should be used for authenticating with Google Drive
     * @param connectTimeout Connect timeout for HTTP connections.
     * @param readTimeout Read timeout for HTTP connections.
     * @return The google drive service
     * @throws IOException If the credential storage cannot be accesssed
     */
    public static Drive getExistingAuthDriveService(final CredentialLocationType locationType,
        final String credentialPath, final String user, final Duration connectTimeout, final Duration readTimeout)
        throws IOException {

        final var credentialDataStoreFactory = getCredentialDataStoreFactory(locationType, credentialPath);
        final var credential =
            getAuthorizationCodeFlow(credentialDataStoreFactory, connectTimeout, readTimeout).loadCredential(user);

        return new Drive.Builder(//
            GoogleApiUtil.getHttpTransport(), //
            GoogleApiUtil.getJsonFactory(), //
            new TimeoutHttpRequestInitializer(connectTimeout, readTimeout, credential))//
                .setApplicationName(GoogleSheetsConnection.APP_NAME)//
                .build();
    }

    /**
     * Tries to use pre-existing authentication. If the permissions for the app are revoked in the google settings,
     * the authentication tokens need to be removed before they can be renewed. This is handled.
     * The authentication is verified.
     *
     * Used in the dialog of the {@link GoogleSheetsInteractiveServiceProviderFactory} node.
     * @param locationType The {@link CredentialLocationType} used for authentication
     * @param credentialPath The path to the data store file
     * @param user The user to be used for authentication
     * @param connectTimeout Connect timeout for HTTP connections.
     * @param readTimeout Read timeout for HTTP connections.
     * @return An authenticated sheet service.
     * @throws IOException If there is a problem reading the credentials from the data storage file
     */
    public static Sheets getAuthRenewedSheetsService(final CredentialLocationType locationType,
        final String credentialPath, final String user, final Duration connectTimeout, final Duration readTimeout)
        throws IOException {

        final var credentialDataStoreFactory = getCredentialDataStoreFactory(locationType, credentialPath);
        try {
            return getAuthSheetsService(credentialDataStoreFactory, user, connectTimeout, readTimeout);
        } catch (IOException e) { // NOSONAR
            getAuthorizationCodeFlow(credentialDataStoreFactory, connectTimeout, readTimeout).getCredentialDataStore()
                .delete(user);
            return getAuthSheetsService(credentialDataStoreFactory, user, connectTimeout, readTimeout);
        }
    }

    /**
     * Creates an authorized credential object.
     * If the given user is not yet authenticated a pop-up will ask the user to authenticate.
     * The acquired authentication token will be stored in the given {@link File}.
     *
     * @param connectTimeout Connect timeout for HTTP connections.
     * @param readTimeout Read timeout for HTTP connections.
     * @return an authorized Credential object.
     * @throws IOException If there is a problem reading the credentials from the data storage file
     */
    private static Credential authorize(final DataStoreFactory credentialDataStore, final String user,
        final Duration connectTimeout, final Duration readTimeout) throws IOException {

        final GoogleAuthorizationCodeFlow flow =
            getAuthorizationCodeFlow(credentialDataStore, connectTimeout, readTimeout);
        return new CustomAuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(user);
    }

    private static Sheets getSheetService(final Credential credential, final Duration connectTimeout,
        final Duration readTimeout) {

        return new Sheets.Builder(//
                    GoogleApiUtil.getHttpTransport(), //
                    GoogleApiUtil.getJsonFactory(), //
                    new TimeoutHttpRequestInitializer(connectTimeout, readTimeout, credential))//
                .setApplicationName(GoogleSheetsConnection.APP_NAME).build();
    }

    /**
     * Build and return an authorized Sheets API client service.
     *
     * Used to do the interactive authorization
     *
     * @param credentialPath The path to where the credentials should be saved
     * @param user The user whose credentials should be used.
     * @param config whether the instance is for configuration
     * @return an authorized Sheets API client service
     * @throws IOException If there is a problem reading the credentials from the data storage file
     * @throws GeneralSecurityException If there is a problem testing the sheet service
     */
    private static Sheets getAuthSheetsService(final DataStoreFactory credentialDataStoreFactory, final String user,
        final Duration connectTimeout, final Duration readTimeout) throws IOException {

        final var credential = authorize(credentialDataStoreFactory, user, connectTimeout, readTimeout);
        final var service = getSheetService(credential, connectTimeout, readTimeout);
        testService(service);
        return service;
    }

    private static GoogleAuthorizationCodeFlow getAuthorizationCodeFlow(
        final DataStoreFactory credentialDataStoreFactory, final Duration connectTimeout, final Duration readTimeout)
        throws IOException {

        final var clientSecrets = ClientSecrets.loadDeprecatedDefaultClientSecrets();
        return new GoogleAuthorizationCodeFlow.Builder(//
            GoogleApiUtil.getHttpTransport(), //
            GoogleApiUtil.getJsonFactory(), //
            clientSecrets, SCOPES)//
                .setRequestInitializer(//
                    GoogleApiUtil.getHttpRequestInitializerWithTimeouts(connectTimeout, readTimeout))//
                .setDataStoreFactory(credentialDataStoreFactory)//
                .setAccessType("offline")//
                .build();
    }

    /**
     * Tests the Sheets Service by reading a public example spreadsheet
     *  to make sure that the credentials are still valid.
     *
     * @param service The sheet service to be tested
     * @throws IOException If the service is not properly authenticated
     */
    private static void testService(final Sheets service) throws IOException {
        Spreadsheet result = service.spreadsheets().get("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms").execute();
        if (result == null) {
            throw new IOException("Could not get the requested data");
        }
    }

    /**
     * Create a temporary file from a given byte string and returns it's location.

     * @param credentialByteString The byte string that should be written to a temporary folder.
     * @return The credential location
     * @throws IOException If temporary folder cannot be created
     */
    public static String getTempCredentialPath(final String credentialByteString) throws IOException {
        final var tempFolder = FileUtil.createTempDir("sheets");
        createTempFromByteFile(tempFolder, credentialByteString);
        return tempFolder.getPath();
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
        Files.write(new File(tempFolder, STORAGE_CREDENTIAL).toPath(), decodeBase64);
    }

    /**
     * Returns the encoded storage credential from the given path
     *
     * @param tempfolder The path of the Credentials
     * @return The base64 encoded file storage credential file from the given folder
     * @throws IOException If the path cannot be read
     */
    public static String getByteStringFromFile(final String tempfolder) throws IOException {
        return Base64.getEncoder().encodeToString(
            Files.readAllBytes(new File(tempfolder, STORAGE_CREDENTIAL).toPath()));
    }


    /**
     * Returns the {@link DataStoreFactory} corresponding to the given {@link CredentialLocationType}.
     *
     * @param locationType The {@link CredentialLocationType} for which the {@link DataStoreFactory} should be returned
     * @param credentialPath The credential path for appropriate credential location types
     * @return The {@link DataStoreFactory} corresponding to the given {@link CredentialLocationType} and path
     * @throws IOException If the {@link DataStoreFactory} cannot be created
     */
    private static DataStoreFactory getCredentialDataStoreFactory(final CredentialLocationType locationType,
        final String credentialPath) throws IOException {

        return switch (locationType) {
            case MEMORY -> MemoryDataStoreFactory.getDefaultInstance();
            case DEFAULT -> new FileDataStoreFactory(new File(credentialPath));
            case CUSTOM -> new FileDataStoreFactory(new File(credentialPath));
        };
    }
}
