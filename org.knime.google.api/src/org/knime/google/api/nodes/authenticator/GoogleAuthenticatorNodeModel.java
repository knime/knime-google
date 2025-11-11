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
 *   Aug 6, 2023 (Alexander Bondaletov, Redfield SE): created
 */
package org.knime.google.api.nodes.authenticator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.credentials.base.CredentialRef;
import org.knime.credentials.base.node.AuthenticatorNodeModel;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.FileSystemHelper;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.FileFilterStatistic;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.google.api.credential.GoogleCredential;
import org.knime.google.api.nodes.authenticator.APIKeySettings.APIKeyType;
import org.knime.google.api.nodes.util.ServiceAccountCredentialsUtil;

import com.google.auth.oauth2.GoogleCredentials;

/**
 * The Google Authenticator node. Performs OAuth authentication to selected Google services and produces
 * {@link GoogleCredential}.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class GoogleAuthenticatorNodeModel extends AuthenticatorNodeModel<GoogleAuthenticatorSettings> {

    private static final String LOGIN_FIRST_ERROR = "Please use the configuration dialog to log in first.";

    /**
     * This references a {@link GoogleCredential} that was acquired interactively in the node dialog.
     * It is disposed when the workflow is closed, or when the authentication scheme is switched to
     * non-interactive. However, it is NOT disposed during reset().
     */
    private CredentialRef m_interactiveCredentialRef;

    private final NodeModelStatusConsumer m_statusConsumer;

    /**
     * @param configuration The node configuration.
     */
    protected GoogleAuthenticatorNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, GoogleAuthenticatorSettings.class);
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    @Override
    protected void validateOnConfigure(final PortObjectSpec[] inSpecs, final GoogleAuthenticatorSettings settings)
        throws InvalidSettingsException {
        settings.validateOnConfigure();

        if (settings.m_authType == GoogleAuthenticatorSettings.AuthType.INTERACTIVE) {
            m_interactiveCredentialRef = Optional.ofNullable(settings.m_loginCredentialRef)//
                .map(CredentialRef::new)//
                .orElseThrow(() -> new InvalidSettingsException(LOGIN_FIRST_ERROR));

            if (!m_interactiveCredentialRef.isPresent()) {
                throw new InvalidSettingsException(LOGIN_FIRST_ERROR);
            }
        } else {
            disposeInteractiveCredential();
        }
    }

    @Override
    protected GoogleCredential createCredential(final PortObject[] inObjects, final ExecutionContext exec,
        final GoogleAuthenticatorSettings settings) throws Exception {

        return switch (settings.m_authType) {
            case INTERACTIVE -> m_interactiveCredentialRef.getCredential(GoogleCredential.class)//
                    .orElseThrow(() -> new InvalidSettingsException(LOGIN_FIRST_ERROR));
            case API_KEY -> new GoogleCredential(loadFromAPIKey(settings));
            default -> throw new IllegalArgumentException("Usupported auth type: " + settings.m_authType);
        };
    }

    private GoogleCredentials loadFromAPIKey(final GoogleAuthenticatorSettings settings)
            throws IOException, InvalidSettingsException {

        final var apiKeysettings = settings.m_apiKeySettings;
        final var scopes = settings.m_scopeSettings.getScopes();

        GoogleCredentials credential;

        try (var accessor = createPathAccessor(settings.m_apiKeySettings)) {
            final Path keyFilePath = accessor.getRootPath(m_statusConsumer);

            if (apiKeysettings.m_apiKeyFormat == APIKeySettings.APIKeyType.JSON) {
                credential = ServiceAccountCredentialsUtil.loadFromJson(keyFilePath).createScoped(scopes);
            } else {
                credential = ServiceAccountCredentialsUtil
                    .loadFromP12(apiKeysettings.m_serviceAccountEmail, keyFilePath).createScoped(scopes);
            }
        }

        // get access token by refreshing token
        credential.refresh();

        return credential;
    }

    private static ReadPathAccessor createPathAccessor(final APIKeySettings settings) throws IOException {
        var fileChooser = settings.m_apiKeyFormat == APIKeyType.JSON ? settings.m_jsonFile : settings.m_p12File;
        var fsLocation = fileChooser.getFSLocation();

        return new FSLocationPathAccessor(fsLocation);
    }

    private void disposeInteractiveCredential() {
        if (m_interactiveCredentialRef != null) {
            m_interactiveCredentialRef.dispose();
            m_interactiveCredentialRef = null;
        }
    }

    @Override
    protected void onDisposeInternal() {
        disposeInteractiveCredential();
    }

    /**
     * Temporary solution to enable FSLocation flow variables on all convenience file systems.
     */
    static class FSLocationPathAccessor implements ReadPathAccessor {

        private final FSLocation m_fsLocation;

        private final FSConnection m_connection;

        private final FSFileSystem<?> m_fileSystem;

        public FSLocationPathAccessor(final FSLocation fsLocation) throws IOException {
            m_fsLocation = fsLocation;
            m_connection = FileSystemHelper.retrieveFSConnection(Optional.empty(), fsLocation)
                    .orElseThrow(() -> new IOException("File system is not available"));
            m_fileSystem = m_connection.getFileSystem();
        }

        @Override
        public void close() throws IOException {
            m_fileSystem.close();
            m_connection.close();
        }

        @Override
        public List<FSPath> getFSPaths(final Consumer<StatusMessage> statusMessageConsumer)
            throws IOException, InvalidSettingsException {
            return List.of(getRootPath(statusMessageConsumer));
        }

        @Override
        public FSPath getRootPath(final Consumer<StatusMessage> statusMessageConsumer)
            throws IOException, InvalidSettingsException {
            return m_fileSystem.getPath(m_fsLocation);
        }

        @Override
        public FileFilterStatistic getFileFilterStatistic() {
            return new FileFilterStatistic(0, 0, 0, 1, 0, 0, 0);
        }

    }
}
