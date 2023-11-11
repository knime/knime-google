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
 *   Nov 11, 2023 (bjoern): created
 */
package org.knime.google.api.credential;

import java.io.IOException;
import java.sql.Date;

import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.credentials.base.CredentialRef;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.credentials.base.oauth.api.AccessTokenAccessor;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;

/**
 * Utility class to help convert an {@link AccessTokenAccessor} into Google a {@link OAuth2Credentials}, thus
 * making it available to the Google API libraries.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class CredentialUtil {

    private CredentialUtil() {
    }

    /**
     * Convenience method that attempts to convert the given {@link CredentialPortObjectSpec}
     * to a Google {@link OAuth2Credentials}.
     *
     * @param inSpec Port object spec from which to retrieve a credential.
     * @return the resulting {@link OAuth2Credentials}.
     * @throws IOException
     * @throws NoSuchCredentialException If no credential is present, or it is incompatible.
     */
    public static OAuth2Credentials toOAuth2Credentials(final CredentialPortObjectSpec inSpec)
        throws IOException, NoSuchCredentialException {

        return toOAuth2Credentials(inSpec.toAccessor(AccessTokenAccessor.class));
    }

    /**
     * Convenience method that attempts to convert the given {@link CredentialRef}
     * to a Google {@link OAuth2Credentials}.
     *
     * @param ref {@link CredentialRef} from which to retrieve a credential.
     * @return the resulting {@link OAuth2Credentials}.
     * @throws IOException
     * @throws NoSuchCredentialException If no credential is present, or it is incompatible.
     */
    public static OAuth2Credentials toOAuth2Credentials(final CredentialRef ref)
        throws IOException, NoSuchCredentialException {

        return toOAuth2Credentials(ref.toAccessor(AccessTokenAccessor.class));
    }

    /**
     * Attempts to convert the given {@link AccessTokenAccessor} to a Google {@link OAuth2Credentials}.
     *
     * If the given accessor is in fact a {@link GoogleCredential}, then this method returns the
     * {@link OAuth2Credentials} instance embedded within. Otherwise, an {@link OAuth2Credentials} instance is created
     * ad-hoc, which may involve fetching or refreshing an access token, hence this method is blocking and can throw an
     * {@link IOException}.
     *
     * @param accessor The {@link AccessTokenAccessor} to converts
     * @return the resulting {@link OAuth2Credentials}.
     * @throws IOException when something went wrong while fetching or refreshing an access token.
     */
    public static OAuth2Credentials toOAuth2Credentials(final AccessTokenAccessor accessor)
        throws IOException {

        if (accessor instanceof GoogleCredential googleCred) {
            return googleCred.getOAuth2Credentials();
        } else {
            return new OAuth2Credentials(fetchAccessToken(accessor)) {
                private static final long serialVersionUID = -1239874598;

                @Override
                public AccessToken refreshAccessToken() throws IOException {
                    return fetchAccessToken(accessor);
                }
            };
        }
    }

    @SuppressWarnings("static-access")
    private static AccessToken fetchAccessToken(final AccessTokenAccessor knimeCredential)
        throws IOException {

        final var accessToken = knimeCredential.getAccessToken();
        final var expiresAfter = knimeCredential.getExpiresAfter()//
            .map(Date::from)//
            .orElse(null);

        return AccessToken.newBuilder()//
            .setTokenValue(accessToken)//
            .setExpirationTime(expiresAfter)//
            .build();
    }
}
