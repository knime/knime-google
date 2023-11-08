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
 *   Oct 10, 2023 (Zkriya Rakhimberdiyev, Redfield SE): created
 */
package org.knime.google.api.credential;

import static org.knime.credentials.base.CredentialPortViewUtil.obfuscate;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.knime.credentials.base.Credential;
import org.knime.credentials.base.CredentialPortViewData;
import org.knime.credentials.base.CredentialPortViewData.Section;
import org.knime.credentials.base.CredentialType;
import org.knime.credentials.base.CredentialTypeRegistry;
import org.knime.credentials.base.NoOpCredentialSerializer;
import org.knime.credentials.base.oauth.api.HttpAuthorizationHeaderCredentialValue;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;


/**
 * {@link Credential} implementation for Google.
 *
 * @author Zkriya Rakhimberdiyev, Redfield SE
 */
public class GoogleCredential implements Credential, HttpAuthorizationHeaderCredentialValue {

    /**
     * The serializer class
     */
    public static class Serializer extends NoOpCredentialSerializer<GoogleCredential> {
    }

    /** Error message if credentials are missing */
    public static final String NO_AUTH_ERROR = "No valid Google credentials found. "
            + "Please re-execute preceding authentication node.";

    /**
     * Credential type.
     */
    public static final CredentialType TYPE = CredentialTypeRegistry.getCredentialType("knime.GoogleCredential");

    private GoogleCredentials m_credentials;

    /**
     * Default constructor for ser(de).
     */
    public GoogleCredential() {
    }

    /**
     * Constructor that wraps a given {@link GoogleCredentials} instance.
     *
     * @param creds
     */
    public GoogleCredential(final GoogleCredentials creds) {
        m_credentials = creds;
    }

    private String getAccessTokenValue() throws IOException {
        return getAccessToken().getTokenValue();
    }

    private AccessToken getAccessToken() throws IOException {
        m_credentials.refreshIfExpired();
        return m_credentials.getAccessToken();
    }

    /**
     * @return {@link GoogleCredentials} credentials.
     */
    public GoogleCredentials getCredentials() {
        return m_credentials;
    }

    @Override
    public String getAuthScheme() {
        return "Bearer";
    }

    @Override
    public String getAuthParameters() throws IOException {
        return getAccessTokenValue();
    }

    @Override
    public CredentialType getType() {
        return TYPE;
    }

    @Override
    public CredentialPortViewData describe() {
        final var sections = new LinkedList<Section>();
        try {
            if (m_credentials instanceof ServiceAccountCredentials) {
                sections.addAll(describeServiceCredentials());
            } else {
                sections.addAll(describeUserCredentials());
            }
        } catch (IOException ex) {// NOSONAR error message is attached to description
            sections.add(new Section("Error", new String[][] { { "message", ex.getMessage() } }));
        }
        return new CredentialPortViewData(sections);
    }

    private List<Section> describeServiceCredentials() throws IOException {
        final var credentials = (ServiceAccountCredentials) m_credentials;
        return List.of(new Section("Google Service Account Credentials", new String[][] {
            { "Token", obfuscate(getAccessTokenValue()) },
            { "Project ID", StringUtils.trimToEmpty(credentials.getProjectId()) },
            { "Private Key ID", StringUtils.trimToEmpty(credentials.getPrivateKeyId()) },
            { "Client ID", StringUtils.trimToEmpty(credentials.getClientId()) },
            { "Client Email", credentials.getClientEmail() },
            { "Expires after", getFormattedExpiresAfter() },
        }));
    }

    private List<Section> describeUserCredentials() throws IOException {
        final var credentials = (UserCredentials) m_credentials;
        return List.of(new Section("Google User Credentials", new String[][] {
            { "Token", obfuscate(getAccessTokenValue()) },
            { "Refresh Token", obfuscate(credentials.getRefreshToken()) },
            { "Client ID", credentials.getClientId() },
            { "Client Secret", obfuscate(credentials.getClientSecret()) },
            { "Expires after", getFormattedExpiresAfter() },
        }));
    }

    private String getFormattedExpiresAfter() throws IOException {
        final var expiresAfter = getAccessToken().getExpirationTime().toInstant();
        return expiresAfter.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
