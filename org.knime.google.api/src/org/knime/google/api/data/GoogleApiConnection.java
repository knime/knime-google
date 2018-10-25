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
 *   Mar 18, 2014 ("Patrick Winter"): created
 */

package org.knime.google.api.data;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.google.api.nodes.authconnector.auth.GoogleAuthLocationType;
import org.knime.google.api.nodes.authconnector.auth.GoogleAuthentication;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScope;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScopeRegistry;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

/**
 * Object that represents a connection to the Google API.
 *
 * Use the {@link GoogleCredential} object returned by {@link #getCredential()} to build objects for a specific Google
 * API.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public final class GoogleApiConnection {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private static final String CFG_SERVICE_ACCOUNT_EMAIL = "service_account_email";

    private static final String CFG_KEY_FILE_LOCATION = "key_file_location";

    private static final String CFG_SCOPES = "scopes";

    private static final String CFG_CREDENTIAL_LOCATION_TYPE = "credentialLocationType";

    private static final String CFG_CREDENTIAL_LOCATION = "credentialLocation";

    private static final String CFG_STORED_CREDENTIAL = "storedCredential";

    private static final String CFG_KNIME_SCOPES = "knimeScopes";

    private Credential m_credential;

    private String m_serviceAccountEmail;

    private String m_keyFileLocation;

    private GoogleAuthLocationType m_credentialLocationType;

    private String m_credentialLocation;

    private String m_storedCredential;

    private String[] m_scopes;

    private List<KnimeGoogleAuthScope> m_knimeGoogleScopes;

    /**
     * Creates a new {@link GoogleApiConnection}.
     *
     * @param serviceAccountEmail Email address of the service account
     * @param keyFileLocation Location of the P12 key file
     * @param scopes Scopes that will be used
     * @throws GeneralSecurityException If there was a problem with the key file
     * @throws IOException If the key file was not accessible
     */
    public GoogleApiConnection(final String serviceAccountEmail, final String keyFileLocation, final String... scopes)
        throws GeneralSecurityException, IOException {
        m_serviceAccountEmail = serviceAccountEmail;
        m_keyFileLocation = keyFileLocation;
        m_scopes = scopes;
        m_credential = new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT).setJsonFactory(JSON_FACTORY)
            .setServiceAccountId(serviceAccountEmail).setServiceAccountScopes(Arrays.asList(scopes))
            .setServiceAccountPrivateKeyFromP12File(new File(keyFileLocation)).build();
        m_credentialLocationType = null;
    }

    /**
     * Creates a new {@link GoogleApiConnection}
     *
     * @param locationType The credential location type
     * @param credentialLocation
     * @param knimeGoogleScopes The scopes for the credentials.
     * @throws IOException If the credentials cannot be read from the credentiallocationType
     */
    public GoogleApiConnection(final GoogleAuthLocationType locationType, final String credentialLocation,
        final List<KnimeGoogleAuthScope> knimeGoogleScopes) throws IOException {
        m_scopes = KnimeGoogleAuthScopeRegistry.getAuthScopes(knimeGoogleScopes)
            .toArray(new String[knimeGoogleScopes.size()]);
        m_credentialLocationType = locationType;
        m_credentialLocation = credentialLocation;
        if (m_credentialLocationType.equals(GoogleAuthLocationType.NODE)) {
            m_storedCredential = GoogleAuthentication.getByteStringFromFile(credentialLocation);
        }
        m_knimeGoogleScopes = knimeGoogleScopes;
        m_credential = GoogleAuthentication.getNoAuthCredential(m_credentialLocationType, m_credentialLocation,
            m_knimeGoogleScopes);

    }

    /**
     * Private constructor used to load connection.
     *
     * @param locationType The credential location type
     * @param serviceAccountEmail The service account email if appropriate
     * @param keyFileLocation The key file location if appropriate
     * @param scopes The scopes used for the credentials
     * @throws GeneralSecurityException If there was a problem with the key file
     * @throws IOException If the key file or the credential location is not accessible
     */
    private GoogleApiConnection(final GoogleAuthLocationType locationType, final String credentialLocation,
        final String storedCredential, final String serviceAccountEmail, final String keyFileLocation,
        final List<KnimeGoogleAuthScope> knimeAuthScopes, final String... scopes)
        throws GeneralSecurityException, IOException {
        m_serviceAccountEmail = serviceAccountEmail;
        m_keyFileLocation = keyFileLocation;
        m_scopes = scopes;
        m_credentialLocationType = locationType;
        m_credentialLocation = credentialLocation;
        m_storedCredential = storedCredential;
        m_knimeGoogleScopes = knimeAuthScopes;
        if (locationType == null) {
            m_credential = new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT).setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(serviceAccountEmail).setServiceAccountScopes(Arrays.asList(scopes))
                .setServiceAccountPrivateKeyFromP12File(new File(keyFileLocation)).build();
        } else {
            if (m_credentialLocationType.equals(GoogleAuthLocationType.NODE)
                && !(new File(m_credentialLocation).exists())) {
                m_credentialLocation = GoogleAuthentication.getTempCredentialPath(m_storedCredential);
            }
            m_credential = GoogleAuthentication.getNoAuthCredential(m_credentialLocationType, m_credentialLocation,
                m_knimeGoogleScopes);
        }
    }

    /**
     * Restores a {@link GoogleApiConnection} from a saved model (used by the framework).
     *
     * @param model The model containing the connection information
     * @throws GeneralSecurityException If there was a problem with the key file
     * @throws IOException If the key file was not accessible
     * @throws InvalidSettingsException If the model was invalid
     */
    public GoogleApiConnection(final ModelContentRO model)
        throws GeneralSecurityException, IOException, InvalidSettingsException {
        this(
            model.containsKey(CFG_CREDENTIAL_LOCATION_TYPE)
                ? GoogleAuthLocationType.valueOf(model.getString(CFG_CREDENTIAL_LOCATION_TYPE)) : null,
            model.getString(CFG_CREDENTIAL_LOCATION), model.getString(CFG_STORED_CREDENTIAL),
            model.getString(CFG_SERVICE_ACCOUNT_EMAIL), model.getString(CFG_KEY_FILE_LOCATION),
            model.containsKey(CFG_KNIME_SCOPES) ?
            KnimeGoogleAuthScopeRegistry.getInstance().getScopesFromString(
                Arrays.asList(model.getStringArray(CFG_KNIME_SCOPES))) : null,
            model.getStringArray(CFG_SCOPES));
    }

    /**
     * @return The {@link HttpTransport} instance to be used to build Google API objects
     */
    public static HttpTransport getHttpTransport() {
        return HTTP_TRANSPORT;
    }

    /**
     * @return The {@link JsonFactory} instance to be used to build Google API objects
     */
    public static JsonFactory getJsonFactory() {
        return JSON_FACTORY;
    }

    /**
     * @return The {@link GoogleCredential} object used to build specific Google API objects
     */
    public Credential getCredential() {
        return m_credential;
    }

    /**
     * @param model The model to save the current configuration in
     */
    public void save(final ModelContentWO model) {
        model.addString(CFG_SERVICE_ACCOUNT_EMAIL, m_serviceAccountEmail);
        model.addString(CFG_KEY_FILE_LOCATION, m_keyFileLocation);
        model.addStringArray(CFG_SCOPES, m_scopes);
        if (m_credentialLocationType != null) {
            model.addString(CFG_CREDENTIAL_LOCATION_TYPE, m_credentialLocationType.getActionCommand());
        }
        model.addString(CFG_CREDENTIAL_LOCATION, m_credentialLocation);
        model.addString(CFG_STORED_CREDENTIAL, m_storedCredential);
        if (m_knimeGoogleScopes != null) {
            model.addStringArray(CFG_KNIME_SCOPES,
                KnimeGoogleAuthScopeRegistry.getKnimeGoogleScopeIds(m_knimeGoogleScopes));
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
        if (!(obj instanceof GoogleApiConnection)) {
            return false;
        }
        GoogleApiConnection con = (GoogleApiConnection)obj;
        EqualsBuilder eb = new EqualsBuilder();
        eb.append(m_serviceAccountEmail, con.m_serviceAccountEmail);
        eb.append(m_keyFileLocation, con.m_keyFileLocation);
        eb.append(m_scopes, con.m_scopes);
        eb.append(m_credentialLocationType, con.m_credentialLocationType);
        eb.append(m_credentialLocation, con.m_credentialLocation);
        eb.append(m_storedCredential, con.m_storedCredential);
        eb.append(m_knimeGoogleScopes, con.m_knimeGoogleScopes);
        return eb.isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(m_serviceAccountEmail);
        hcb.append(m_keyFileLocation);
        hcb.append(m_scopes);
        hcb.append(m_credentialLocationType.getActionCommand());
        hcb.append(m_credentialLocation);
        hcb.append(m_storedCredential);
        hcb.append(m_knimeGoogleScopes);
        return hcb.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (m_credentialLocationType == null) {
            sb.append("Service account email address:\n" + m_serviceAccountEmail + "\n\n");
            sb.append("Key file:\n" + m_keyFileLocation + "\n\n");
        } else {
            sb.append("Google OAuth 2.0 Authentication \n\n");
            sb.append("Authentication Keys: " + m_credentialLocationType.getText() + "\n\n");
            switch (m_credentialLocationType) {
                case MEMORY:
                    //
                    break;
                case NODE:
                    break;
                default:
                    sb.append("Location: " + m_credentialLocation + "\n\n");
            }

        }
        sb.append("Scopes:\n");
        for (String scope : m_scopes) {
            sb.append(scope + "\n");
        }
        return sb.toString();
    }

}
