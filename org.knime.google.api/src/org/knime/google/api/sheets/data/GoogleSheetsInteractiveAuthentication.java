/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.knime.core.util.FileUtil;
import org.knime.google.api.sheets.nodes.connectorinteractive.GoogleSheetsInteractiveServiceProviderFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;

/**
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
public class GoogleSheetsInteractiveAuthentication {

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** The application name, for which the credentials are stored */
    public static final String APP_NAME = "KNIME-Google-Sheets-Interactive-Connector";

    /** The name of the credentials file created by the google API */
    public static final String STORAGE_CREDENTIAL = "StoredCredential";

    /** The scopes required for the {@link GoogleSheetsInteractiveAuthentication} */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_READONLY);

    /**
     * Returns a {@link Sheets} service for the given user from the given application name
     * and the path for the data store file. The authentication is verified.
     *
     * @param credentialPath the path for the data store file
     * @param user the user for which the credentials should be used
     * @return A sheets service authenticated for the given user, with credentials read from
     * the given location of the data store file
     * @throws IOException If there is a problem reading the credentials from the data storage file
     * @throws GeneralSecurityException If there is a problem testing the sheet service
     */
    public static Sheets getExistingAuthSheetService(final String credentialPath,
        final String user) throws IOException, GeneralSecurityException {
        Credential credential = existingAuthorization(credentialPath, user);
        Sheets service =
            getSheetService(credential);
        testService(service);
        return service;
    }

    /**
     * Tries to use pre-existing authentication. If the permissions for the app are revoked in the google settings,
     * the authentication tokens need to be removed before they can be renewed. This is handled.
     * The authentication is verified.
     *
     * Used in the dialog of the {@link GoogleSheetsInteractiveServiceProviderFactory} node.
     *
     * @param credentialPath The path to the data store file
     * @param user The user to be used for authentication
     * @return An authenticated sheet service.
     * @throws IOException If there is a problem reading the credentials from the data storage file
     * @throws GeneralSecurityException If there is a problem testing the sheet service
     */
    public static Sheets getAuthRenewedSheetsService(final String credentialPath,
        final String user) throws IOException, GeneralSecurityException {
        try{
                return getAuthSheetsService(credentialPath, user);
        } catch (IOException | GeneralSecurityException e) {
            getAuthorizationCodeFlow(credentialPath).getCredentialDataStore().delete(user);
            return getAuthSheetsService(credentialPath, user);
        }
    }


    /**
     * Creates an authorized credential object.
     * If the given user is not yet authenticated a pop-up will ask the user to authenticate.
     * The acquired authentication token will be stored in the given {@link File}.
     *
     * @return an authorized Credential object.
     * @throws IOException If there is a problem reading the credentials from the data storage file
     */
    private static Credential authorize(final String credentialPath, final String user)
        throws IOException {
        final GoogleAuthorizationCodeFlow flow = getAuthorizationCodeFlow(credentialPath);
        final Credential credential =
            new CustomAuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize(user);
        return credential;
    }

    /**
     * Returns credentials for the given user from the given data store file.
     *
     * @param credentialPath
     * @param user The user for which the credentials should be read from the data storage file
     * @return The credentials for the given user from the given data storage file
     * @throws IOException If there is a problem reading the credentials from the data storage file
     */
    private static Credential existingAuthorization(final String credentialPath, final String user) throws IOException {
        return getAuthorizationCodeFlow(credentialPath).loadCredential(user);
    }

    private static Sheets getSheetService(final Credential credential) {
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APP_NAME).build();
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
    private static Sheets getAuthSheetsService(final String credentialPath,
        final String user) throws IOException, GeneralSecurityException {
            final Credential credential = authorize(credentialPath, user);
            Sheets service = getSheetService(credential);
            testService(service);
            return service;
    }


    private static File getDataStoreFile(final String credentialPath) {
        return new File(credentialPath);
    }

    private static GoogleAuthorizationCodeFlow getAuthorizationCodeFlow(final String credentialPath) throws IOException {
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(getDataStoreFile(credentialPath));
        // Load client secrets.
        try (final InputStream in = GoogleSheetsConnection.class.getResourceAsStream("client_secret.json")) {
            final GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            return new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(dataStoreFactory).setAccessType("offline").build();
        } catch (IOException e) {
            throw e;
        }
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
     * @throws URISyntaxException If the byte string file cannot be written
     */
    public static String getTempCredentialPath(final String credentialByteString) throws IOException, URISyntaxException{
        File tempFolder = FileUtil.createTempDir("sheets");
        createTempFromByteFile(tempFolder, credentialByteString);
        return tempFolder.getPath();
    }

    /**
     * Decodes a given byte String to a given folder.
     *
     * @param tempFolder The temp folder that should be populated with the decoded byte string
     * @param credentialByteString The byte string that should be decoded to the given folder
     * @throws IOException If there is an error when writing the temp file
     * @throws URISyntaxException If the file cannot be decoded
     *
     */
    public static void createTempFromByteFile(final File tempFolder, final String credentialByteString) throws IOException, URISyntaxException {
        byte[] decodeBase64 = Base64.decodeBase64(credentialByteString);
        Files.write(new File(tempFolder, STORAGE_CREDENTIAL).toPath(), decodeBase64);
    }
}
