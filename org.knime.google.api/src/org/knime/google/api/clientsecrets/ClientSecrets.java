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
 *   Nov 3, 2023 (bjoern): created
 */
package org.knime.google.api.clientsecrets;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.knime.google.api.nodes.util.GoogleApiUtil;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;

/**
 * Utility class to retrieve {@link GoogleClientSecrets}, including the default client secret.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class ClientSecrets {

    /** The name of the client secret JSON file **/
    private static final String CLIENT_SECRET = "knime_client_secret-ap_3_7_0.json";

    /** The name of the deprecated client secret JSON file **/
    private static final String DEPRECATED_CLIENT_SECRET = "knime_client_secret_deprecated.json";

    private static GoogleClientSecrets defaultClientSecrets;

    private static GoogleClientSecrets deprecatedDefaultClientSecrets;

    private ClientSecrets() {
    }

    /**
     * Loads {@link GoogleClientSecrets} from the given file.
     *
     * @param path The file to load (client secret json format).
     * @return a {@link GoogleClientSecrets} instance loaded from the given file.
     * @throws IOException
     */
    public static GoogleClientSecrets loadClientSecrets(final Path path) throws IOException {
        try (final var reader = Files.newBufferedReader(path)) {
            return GoogleClientSecrets.load(GoogleApiUtil.getJsonFactory(), reader);
        }
    }

    /**
     * Loads the default {@link GoogleClientSecrets} shipped as part of this plugin.
     *
     * @return the default {@link GoogleClientSecrets}.
     * @throws IOException
     */
    public static synchronized GoogleClientSecrets loadDefaultClientSecrets() throws IOException {
        if (defaultClientSecrets == null) {
            try (final var in = ClientSecrets.class.getResourceAsStream(CLIENT_SECRET)) {
                return GoogleClientSecrets.load(GoogleApiUtil.getJsonFactory(),
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }

        return defaultClientSecrets;
    }

    /**
     * Loads the deprecated default {@link GoogleClientSecrets} shipped as part of this plugin.
     *
     * @return the default {@link GoogleClientSecrets}.
     * @throws IOException
     * @deprecated
     */
    @Deprecated(since = "3.7.0")
    public static synchronized GoogleClientSecrets loadDeprecatedDefaultClientSecrets() throws IOException {
        if (deprecatedDefaultClientSecrets == null) {
            try (final var in = ClientSecrets.class.getResourceAsStream(DEPRECATED_CLIENT_SECRET)) {
                return GoogleClientSecrets.load(GoogleApiUtil.getJsonFactory(),
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }

        return deprecatedDefaultClientSecrets;
    }
}
