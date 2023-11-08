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
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.knime.google.api.nodes.authenticator.AbstractUserCredentialStore;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.util.IOUtils;
import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.AbstractMemoryDataStore;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.auth.oauth2.UserCredentials;

/**
 * {@link AbstractUserCredentialStore} implementation that persists/restores to/from an in-memory {@link DataStore}. The
 * {@link DataStore} contents can be persisted/restored from a string. Unfortunately the string is the result of Java
 * Serialization (and base64-encoding on top). This might break in future Java versions, so this class should not be
 * used anymore.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @since 5.2
 * @deprecated
 */
@Deprecated(since = "5.2")
public class StringSerializedCredentialStore extends AbstractUserCredentialStore {

    /**
     * {@link DataStoreFactory} implementation used to load a credential from a byte array holding a previously
     * serialized {@link DataStore}.
     */
    private class ByteArrayDataStoreFactory extends AbstractDataStoreFactory {

        @SuppressWarnings("unchecked")
        @Override
        protected DataStore<StoredCredential> createDataStore(final String id) throws IOException {
            return new TempDataStore(this, id, m_serializedDataStore);
        }

        private static class TempDataStore extends AbstractMemoryDataStore<StoredCredential> {
            protected TempDataStore(final DataStoreFactory dataStoreFactory, final String id,
                final byte[] serializedDataStore) throws IOException {

                super(dataStoreFactory, id);

                if (serializedDataStore != null) {
                    keyValueMap = IOUtils.deserialize(serializedDataStore);
                }
            }
        }
    }

    private final byte[] m_serializedDataStore;

    /**
     * Constructor.
     *
     * @param serializedDataStore A string containing a serialized {@link DataStore} (base64 encoded).
     * @param clientSecrets OAuth2 client id and secret to use when acquiring/refresh an access token.
     * @param scopes The OAuth2 scopes to request during interactive login.
     */
    public StringSerializedCredentialStore(final String serializedDataStore, final GoogleClientSecrets clientSecrets,
        final List<String> scopes) {

        super(clientSecrets, scopes);

        if (!StringUtils.isBlank(serializedDataStore)) {
            m_serializedDataStore = Base64.getDecoder().decode(serializedDataStore);
        } else {
            m_serializedDataStore = null;
        }
    }

    @Override
    public void clear() {
        // nothing we can do here
    }

    @Override
    protected DataStoreFactory createDataStoreFactory() throws IOException {
        return new ByteArrayDataStoreFactory();
    }

    private static class SerializingMemoryDataStore extends AbstractMemoryDataStore<StoredCredential> {

        SerializingMemoryDataStore() {
            super(MemoryDataStoreFactory.getDefaultInstance(), "dummy");
        }

        byte[] serialize() throws IOException {
            save();
            return IOUtils.serialize(keyValueMap);
        }
    }

    /**
     * Utility method to create a String by seralizing the given {@link UserCredentials}.
     *
     * @param credentials
     * @return a String containing the serialized {@link UserCredentials}.
     */
    public static String toSerializedString(final UserCredentials credentials) {
        final var storedCred = new StoredCredential();
        storedCred.setAccessToken(credentials.getAccessToken().getTokenValue());
        storedCred.setRefreshToken(credentials.getRefreshToken());
        storedCred.setExpirationTimeMilliseconds(credentials.getAccessToken().getExpirationTime().getTime());

        final var tmpDataStore = new SerializingMemoryDataStore();
        try {
            tmpDataStore.set(DEFAULT_KNIME_USER, storedCred);
            return Base64.getEncoder().encodeToString(tmpDataStore.serialize());
        } catch (IOException e) {
            throw new UncheckedIOException(e); // should never happen
        }
    }
}
