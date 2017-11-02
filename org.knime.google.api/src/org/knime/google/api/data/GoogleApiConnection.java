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
 *   Mar 18, 2014 ("Patrick Winter"): created
 */

package org.knime.google.api.data;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

/**
 * Object that represents a connection to the Google API.
 *
 * Use the {@link GoogleCredential} object returned by {@link #getCredential()} to build objects for a specific Google API.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public final class GoogleApiConnection {

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private static final String CFG_SERVICE_ACCOUNT_EMAIL = "service_account_email";

    private static final String CFG_KEY_FILE_LOCATION = "key_file_location";

    private static final String CFG_SCOPES = "scopes";

    private GoogleCredential m_credential;

    private String m_serviceAccountEmail;

    private String m_keyFileLocation;

    private String[] m_scopes;

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
        m_credential =
                new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT).setJsonFactory(JSON_FACTORY)
                        .setServiceAccountId(serviceAccountEmail).setServiceAccountScopes(Arrays.asList(scopes))
                        .setServiceAccountPrivateKeyFromP12File(new File(keyFileLocation)).build();
    }

    /**
     * Restores a {@link GoogleApiConnection} from a saved model (used by the framework).
     *
     * @param model The model containing the connection information
     * @throws GeneralSecurityException If there was a problem with the key file
     * @throws IOException If the key file was not accessible
     * @throws InvalidSettingsException If the model was invalid
     */
    public GoogleApiConnection(final ModelContentRO model) throws GeneralSecurityException, IOException,
            InvalidSettingsException {
        this(model.getString(CFG_SERVICE_ACCOUNT_EMAIL), model.getString(CFG_KEY_FILE_LOCATION), model
                .getStringArray(CFG_SCOPES));
    }

    /**
     * @return The {@link HttpTransport} instance to be used to build Google API objects
     */
    public HttpTransport getHttpTransport() {
        return HTTP_TRANSPORT;
    }

    /**
     * @return The {@link JsonFactory} instance to be used to build Google API objects
     */
    public JsonFactory getJsonFactory() {
        return JSON_FACTORY;
    }

    /**
     * @return The {@link GoogleCredential} object used to build specific Google API objects
     */
    public GoogleCredential getCredential() {
        return m_credential;
    }

    /**
     * @param model The model to save the current configuration in
     */
    public void save(final ModelContentWO model) {
        model.addString(CFG_SERVICE_ACCOUNT_EMAIL, m_serviceAccountEmail);
        model.addString(CFG_KEY_FILE_LOCATION, m_keyFileLocation);
        model.addStringArray(CFG_SCOPES, m_scopes);
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
        return hcb.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Service account email address:\n" + m_serviceAccountEmail + "\n\n");
        sb.append("Key file:\n" + m_keyFileLocation + "\n\n");
        sb.append("Scopes:\n");
        for (String scope : m_scopes) {
            sb.append(scope + "\n");
        }
        return sb.toString();
    }

}
