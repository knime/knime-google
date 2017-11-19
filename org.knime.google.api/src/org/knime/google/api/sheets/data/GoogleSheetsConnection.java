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
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 21, 2017 (oole): created
 */

package org.knime.google.api.sheets.data;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.google.api.data.GoogleApiConnection;

import com.google.api.services.analytics.Analytics;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;

/**
 * A connection to the Google Sheets API.
 *
 * Use the {@link Analytics} object returned by {@link #getSheetsService()} to communicate with the Google Analytics API.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public final class GoogleSheetsConnection {

    private static final String CFG_APPLICATION_NAME = "applicationName";

    private static final String CFG_INTERACTIVE = "interactiveAuth";

    private static final String CFG_CREDENTIALPATH = "credentialPath";

    private static final String CFG_USER = "user";

    private static final String CFG_STORED_CREDENTIAL = "storedCredential";

    private static final String CFG_IN_NODE_CREDENTIAL = "inNodeCredential";

    private GoogleApiConnection m_connection;

    private String m_applicationName;

    private Sheets m_sheets;

    private Drive m_drive;

    private boolean m_interactiveAuth;

    private String m_credentialPath;

    private String m_user;

    private String m_storedCredentials;

    private boolean m_inNodeCredentials;


    /**
     * Creates a new {@link GoogleSheetsConnection} using the given {@link GoogleApiConnection}.
     *
     * @param connection The used GoogleApiConnectionx
     * @param applicationName Name of this application as it is shown to the Google API
     */
    public GoogleSheetsConnection(final GoogleApiConnection connection, final String applicationName) {
        this(connection, applicationName, null, null, false, false, null);
        m_sheets =
                new Sheets.Builder(m_connection.getHttpTransport(), m_connection.getJsonFactory(),
                        m_connection.getCredential()).setApplicationName(m_applicationName).build();
    }

    /**
     * Creates a new {@link GoogleSheetsConnection} using O2Auth web authentication.
     * This creates a pop-up to confirm access to the necessary scopes.
     *z
     * @param applicationName Name of this application as it is shown to the Google API
     * @param credentialPath The path on which the credentials for the interactive service provider are stored
     * @param user The user which should be used for the connection
     * @param storedCredential
     * @param inNodeCredentials
     * @throws IOException
     * @throws GeneralSecurityException
     * @throws URISyntaxException
     */
    public GoogleSheetsConnection(final String applicationName, final String credentialPath, final String user,
        final String storedCredential, final boolean inNodeCredentials)
                throws IOException, GeneralSecurityException, URISyntaxException {
        this(null, applicationName, user, storedCredential, inNodeCredentials, true, credentialPath);
        if (m_inNodeCredentials) {
            m_credentialPath = GoogleSheetsInteractiveAuthentication.getTempCredentialPath(m_storedCredentials);
        }
        m_sheets = GoogleSheetsInteractiveAuthentication.getExistingAuthSheetService(m_credentialPath, m_user);
    }

    private GoogleSheetsConnection(final GoogleApiConnection connection,
        final String applicationName, final String user, final String storedCredentials,
        final boolean inNodeCredentials, final boolean interactiveAuth, final String credentialPath){
        m_connection = connection;
        m_applicationName = applicationName;
        m_user = user;
        m_interactiveAuth = interactiveAuth;
        m_storedCredentials = storedCredentials;
        m_inNodeCredentials = inNodeCredentials;
        m_credentialPath = credentialPath;
    }




    /**
     * Restores a {@link GoogleSheetsConnection} from a saved model (used by the framework).
     *
     * @param model The model containing the connection information
     * @throws InvalidSettingsException If the model was invalid
     */
    public GoogleSheetsConnection(final ModelContentRO model) throws InvalidSettingsException {
        try {
            m_interactiveAuth = model.getBoolean(CFG_INTERACTIVE);
            m_applicationName = model.getString(CFG_APPLICATION_NAME);
            m_credentialPath = model.getString(CFG_CREDENTIALPATH);
            m_user = model.getString(CFG_USER);
            m_storedCredentials = model.getString(CFG_STORED_CREDENTIAL);
            m_inNodeCredentials = model.getBoolean(CFG_IN_NODE_CREDENTIAL);

            if (m_interactiveAuth){
                if (m_inNodeCredentials) {
                    m_credentialPath = GoogleSheetsInteractiveAuthentication.getTempCredentialPath(m_storedCredentials);
                }
                m_sheets = GoogleSheetsInteractiveAuthentication.getExistingAuthSheetService(m_credentialPath, m_user);
            } else {
                m_connection = new GoogleApiConnection(model);
                m_sheets = new Sheets.Builder(m_connection.getHttpTransport(), m_connection.getJsonFactory(),
                    m_connection.getCredential()).setApplicationName(m_applicationName).build();
            }
        } catch (GeneralSecurityException | IOException | URISyntaxException e) {
            throw new InvalidSettingsException(e);
        }
    }

    /**
     * @return The {@link Analytics} object used to communicate with the Google Analytics API
     */
    public Sheets getSheetsService() {
        return m_sheets;
    }

    /**
     * Get the Google Drive service associated with this Google Sheet connection.
     *
     * @return The Google Drive service associated with this Google Sheet connection
     * @throws IOException
     */
    public Drive getDriveService() throws IOException {
        if (m_drive == null) {
            if (m_interactiveAuth) {
                m_drive = GoogleSheetsInteractiveAuthentication.getExistingAuthDriveService(m_credentialPath, m_user);
            } else {
                m_drive = new Drive.Builder(m_connection.getHttpTransport(), m_connection.getJsonFactory(),
                    m_connection.getCredential()).setApplicationName(m_applicationName).build();
            }
        }
        return m_drive;
    }


    /**
     * @param model The model to save the current configuration in
     */
    public void save(final ModelContentWO model) {
        model.addBoolean(CFG_INTERACTIVE, m_interactiveAuth);
        model.addString(CFG_CREDENTIALPATH, m_credentialPath);
        model.addString(CFG_USER, m_user);
        if (!m_interactiveAuth) {
            m_connection.save(model);
        }
        model.addString(CFG_STORED_CREDENTIAL, m_storedCredentials);
        model.addBoolean(CFG_IN_NODE_CREDENTIAL, m_inNodeCredentials);
        model.addString(CFG_APPLICATION_NAME, m_applicationName);
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
        eb.append(m_connection, con.m_connection);
        eb.append(m_applicationName, con.m_applicationName);
        return eb.isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(m_connection);
        hcb.append(m_applicationName);
        return hcb.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!m_interactiveAuth) {
            sb.append(m_connection.toString() + "\n");
        } else {
            sb.append("Interactive Authentication\n");
        }
        sb.append("\n");
        sb.append("Application name:\n" + m_applicationName + "\n");

        sb.append("\n");
        if (!m_inNodeCredentials) {
            sb.append("   User id: " + m_user + "\n");
            sb.append("   Credential path: " + m_credentialPath + "\n");
        } else {
            sb.append("   Using in-node credentials");
        }
        return sb.toString();
    }
}
