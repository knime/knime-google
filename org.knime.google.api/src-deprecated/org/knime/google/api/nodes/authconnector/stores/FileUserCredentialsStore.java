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
package org.knime.google.api.nodes.authconnector.stores;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.knime.google.api.nodes.authenticator.AbstractUserCredentialStore;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

/**
 * {@link AbstractUserCredentialStore} implementation that persists/restores to/from a folder in the local file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @since 5.2
 */
public class FileUserCredentialsStore extends AbstractUserCredentialStore {

    private final Path m_credentialPath;

    /**
     * Constructor.
     *
     * @param path A folder in the local file system. Will be created if necessary.
     * @param clientSecrets OAuth2 client id and secret to use when acquiring/refresh an access token.
     * @param scopes The OAuth2 scopes to request during interactive login.
     */
    public FileUserCredentialsStore(final Path path, final GoogleClientSecrets clientSecrets,
        final List<String> scopes) {

        super(clientSecrets, scopes);
        m_credentialPath = path;
    }

    @Override
    public void clear() {
        // nothing to do here because the (deprecated) Google Authenticator never deletes the
        // folder with the access token
    }

    @Override
    protected DataStoreFactory createDataStoreFactory() throws IOException {
        return new FileDataStoreFactory(m_credentialPath.toFile());
    }
}
