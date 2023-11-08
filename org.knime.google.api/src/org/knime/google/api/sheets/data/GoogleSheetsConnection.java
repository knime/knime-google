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
 *
 * History
 *   Aug 21, 2017 (oole): created
 */

package org.knime.google.api.sheets.data;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.credentials.base.CredentialRef;
import org.knime.credentials.base.CredentialRef.CredentialNotFoundException;
import org.knime.google.api.credential.CredentialRefSerializer;
import org.knime.google.api.credential.GoogleCredential;
import org.knime.google.api.nodes.util.GoogleApiUtil;
import org.knime.google.api.sheets.nodes.connectorinteractive.SettingsModelCredentialLocation.CredentialLocationType;

import com.google.api.services.analytics.Analytics;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;

/**
 * A connection to the Google Sheets API.
 *
 * Use the {@link Analytics} object returned by {@link #getSheetsService()} to communicate with the Google Analytics API.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public final class GoogleSheetsConnection {

    /** The application name, for which the credentials are stored */
    public static final String APP_NAME = "KNIME-Google-Sheets-Connector";

    private static final String CFG_INTERACTIVE = "interactiveAuth";

    private static final String CFG_CREDENTIALPATH = "credentialPath";

    private static final String CFG_USER = "user";

    private static final String CFG_STORED_CREDENTIAL = "storedCredential";

    private static final String CFG_IN_NODE_CREDENTIAL = "inNodeCredential";

    private static final String CFG_CREDENTIAL_LOCATION_TYPE = "credentialLocationType";

    private CredentialRef m_credentialRef;

    private Sheets m_sheets;

    private Drive m_drive;

    private boolean m_interactiveAuth;

    private String m_credentialPath;

    private String m_user;

    private String m_storedCredentials;

    private CredentialLocationType m_credentialLocationType;


    /**
     * Creates a new {@link GoogleSheetsConnection} using the given {@link CredentialRef}.
     *
     * @param credentialRef The {@link CredentialRef} to obtain the credential from.
     */
    public GoogleSheetsConnection(final CredentialRef credentialRef) {
        m_interactiveAuth = false;
        m_credentialRef = CheckUtils.checkArgumentNotNull(credentialRef);
    }

    /**
     * Creates a new {@link GoogleSheetsConnection} using O2Auth web authentication.
     * This creates a pop-up to confirm access to the necessary scopes.
     *
     * @param credentialPath The path on which the credentials for the interactive service provider are stored
     * @param user The user which should be used for the connection
     * @param credentialLocationType The credential location type
     * @throws IOException If the credential path cannot be read
     */
    public GoogleSheetsConnection(final String credentialPath, final String user,
        final CredentialLocationType credentialLocationType) throws IOException{

        m_interactiveAuth = true;
        m_user = user;
        m_credentialLocationType = credentialLocationType;
        if (m_credentialLocationType.equals(CredentialLocationType.DEFAULT)) {
            m_storedCredentials = GoogleSheetsInteractiveAuthentication.getByteStringFromFile(credentialPath);
        }
        m_credentialPath = credentialPath;
    }

    /**
     * Restores a {@link GoogleSheetsConnection} from a saved model (used by the framework).
     *
     * @param model The model containing the connection information
     * @throws InvalidSettingsException If the model was invalid
     */
    public GoogleSheetsConnection(final ModelContentRO model) throws InvalidSettingsException {
        m_interactiveAuth = model.getBoolean(CFG_INTERACTIVE);
        if (m_interactiveAuth) {
            try {
                m_credentialPath = model.getString(CFG_CREDENTIALPATH);
                m_user = model.getString(CFG_USER);
                m_storedCredentials = model.getString(CFG_STORED_CREDENTIAL);
                if (model.containsKey(CFG_IN_NODE_CREDENTIAL)) {
                    boolean inNodeCredentials = model.getBoolean(CFG_IN_NODE_CREDENTIAL);
                    m_credentialLocationType =
                        inNodeCredentials ? CredentialLocationType.DEFAULT : CredentialLocationType.CUSTOM;

                } else {
                    m_credentialLocationType =
                        CredentialLocationType.valueOf(model.getString(CFG_CREDENTIAL_LOCATION_TYPE));
                }

                if (m_credentialLocationType.equals(CredentialLocationType.DEFAULT)
                    && !((new File(m_credentialPath)).exists())) {
                    m_credentialPath = GoogleSheetsInteractiveAuthentication.getTempCredentialPath(m_storedCredentials);
                }
            } catch (IOException e) {
                throw new InvalidSettingsException(e);
            }
        } else {
            m_credentialRef = CredentialRefSerializer.loadRefWithLegacySupport(model);
        }
    }

    /**
     * Returns the {@link Sheets} object used to communicate with the Google Sheets API.
     *
     * @return The {@link Sheets} object used to communicate with the Google Sheets API
     * @throws IOException If the credentials cannot be read
     * @throws CredentialNotFoundException
     */
    public Sheets getSheetsService() throws IOException, CredentialNotFoundException {
        if (m_sheets == null) {
            if (m_interactiveAuth) {
                m_sheets = GoogleSheetsInteractiveAuthentication.
                        getExistingAuthSheetService(m_credentialLocationType, m_credentialPath, m_user);
            } else {
                final var creds = m_credentialRef.resolveCredential(GoogleCredential.class).getCredentials();
                final var credAdapter = new HttpCredentialsAdapter(creds);
                m_sheets = new Sheets.Builder(GoogleApiUtil.getHttpTransport(), GoogleApiUtil.getJsonFactory(),
                    credAdapter).setApplicationName(APP_NAME).build();
            }
        }
        return m_sheets;
    }

    /**
     * Get the Google Drive service associated with this Google Sheet connection.
     *
     * @return The Google Drive service associated with this Google Sheet connection
     * @throws IOException If the credentials cannot be read
     * @throws CredentialNotFoundException
     */
    public Drive getDriveService() throws IOException, CredentialNotFoundException {
        if (m_drive == null) {
            if (m_interactiveAuth) {
                m_drive = GoogleSheetsInteractiveAuthentication.
                        getExistingAuthDriveService(m_credentialLocationType, m_credentialPath, m_user);
            } else {
                final var creds = m_credentialRef.resolveCredential(GoogleCredential.class).getCredentials();
                final var credAdapter = new HttpCredentialsAdapter(creds);
                m_drive = new Drive.Builder(GoogleApiUtil.getHttpTransport(), GoogleApiUtil.getJsonFactory(),
                    credAdapter).setApplicationName(APP_NAME).build();
            }
        }
        return m_drive;
    }


    /**
     * @param model The model to save the current configuration in
     */
    public void save(final ModelContentWO model) {
        model.addBoolean(CFG_INTERACTIVE, m_interactiveAuth);
        if (!m_interactiveAuth) {
            CredentialRefSerializer.saveRef(m_credentialRef, model);
        } else {
            model.addString(CFG_USER, m_user);
            model.addString(CFG_CREDENTIALPATH, m_credentialPath);
            model.addString(CFG_CREDENTIAL_LOCATION_TYPE, m_credentialLocationType.name());
            model.addString(CFG_STORED_CREDENTIAL, m_storedCredentials);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GoogleSheetsConnection)) {
            return false;
        }
        GoogleSheetsConnection con = (GoogleSheetsConnection)obj;
        EqualsBuilder eb = new EqualsBuilder();
        eb.append(m_credentialRef, con.m_credentialRef);
        return eb.isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(m_credentialRef);
        return hcb.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (m_interactiveAuth) {
            sb.append("Interactive Authentication\n");
            sb.append("\n");
            sb.append("Application name:\n" + APP_NAME + "\n");

            sb.append("\n");

            switch (m_credentialLocationType) {
                case DEFAULT:
                    sb.append("Using in-node credentials");
                    break;
                case MEMORY:
                    sb.append("Using in-memory credentials\n");
                    break;
                case CUSTOM:
                    sb.append("Using custom credential location\n");
                    sb.append("User id: " + m_user + "\n");
                    sb.append("Credential path: " + m_credentialPath + "\n");
                    break;
            }
        }
        return sb.toString();
    }
}
