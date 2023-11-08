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
 *   Oct 31, 2023 (bjoern): created
 */
package org.knime.google.api.credential;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.credentials.base.CredentialRef;
import org.knime.google.api.data.LegacyGoogleApiConnectionLoader;

import com.google.auth.oauth2.GoogleCredentials;

/**
 * Helper class for saving/loading {@link CredentialRef} instances as part of port object (specs).
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class CredentialRefSerializer {

    private static final String KEY_CREDENTIAL_REF = "credentialRef";

    private CredentialRefSerializer() {
    }

    /**
     * Loads a new {@link CredentialRef} from the given config. This method expects a {@link CredentialRef} to have been
     * saved before. Note that this method does not load a credential. The returned {@link CredentialRef} may not be
     * resolvable.
     *
     * @param config
     * @return a {@link CredentialRef}, deserialized from the given config.
     * @throws InvalidSettingsException When no {@link CredentialRef} was previously saved to the config.
     */
    public static CredentialRef loadRef(final ConfigRO config) throws InvalidSettingsException {
        final var ref = new CredentialRef();
        ref.load(config.getConfig(KEY_CREDENTIAL_REF));
        return ref;
    }

    /**
     * Saves the given {@link CredentialRef} to the given config.
     *
     * @param ref
     * @param config
     */
    public static void saveRef(final CredentialRef ref, final ConfigWO config) {
        ref.save(config.addConfig(KEY_CREDENTIAL_REF));
    }

    /**
     * Loads a new {@link CredentialRef} from the given config.
     *
     * <p>
     * This method supports configs saved prior to AP 5.2, which contain a saved GoogleApiConnection (removed), from
     * which we can restore a full {@link GoogleCredentials} instance. This method will restore the credential and wrap
     * it in a new {@link CredentialRef}.
     * </p>
     *
     * @param config
     * @return a {@link CredentialRef}, deserialized from the given config.
     * @throws InvalidSettingsException
     */
    public static CredentialRef loadRefWithLegacySupport(final ConfigRO config) throws InvalidSettingsException {
        final var optCredential = LegacyGoogleApiConnectionLoader.restoreAsCredential(config);

        if (optCredential.isPresent()) {
            return new CredentialRef(optCredential.get());
        } else if (config.containsKey(KEY_CREDENTIAL_REF)) {
            return loadRef(config);
        } else {
            // In a workflow saved prior to AP 5.2 and with the Google Authenticator (deprecated) set to MEMORY,
            // there may be neither a credential nor CredentialRef to restore. In this case we simply create an
            // unresolvable CredentialRef
            return new CredentialRef();
        }
    }
}
