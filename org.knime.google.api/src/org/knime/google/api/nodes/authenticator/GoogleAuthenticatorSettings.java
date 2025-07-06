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
 *   Jul 25, 2023 (Alexander Bondaletov, Redfield SE): created
 */
package org.knime.google.api.nodes.authenticator;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonChange;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.CancelableActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.CancelableActionHandler.States;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Advanced;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.credentials.base.CredentialCache;
import org.knime.credentials.base.oauth.api.nodesettings.AbstractTokenCacheKeyPersistor;
import org.knime.google.api.clientsecrets.ClientSecrets;
import org.knime.google.api.credential.GoogleCredential;
import org.knime.google.api.nodes.authenticator.GoogleAuthenticatorNodeModel.FSLocationPathAccessor;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.auth.oauth2.UserCredentials;

/**
 * Node settings for the Google Authenticator node.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class GoogleAuthenticatorSettings implements DefaultNodeSettings {

    @Section(title = "Authentication Key")
    @Effect(predicate = AuthTypeIsInteractive.class, type = EffectType.HIDE)
    interface APIKeyTypeSection {
        interface TypeSwitcher {
        }

        @After(TypeSwitcher.class)
        interface Content {
        }
    }

    @Section(title = "Scopes of access")
    @After(APIKeyTypeSection.class)
    interface ScopesSection {
    }

    @Section(title = "Client/App configuration")
    @Advanced
    @After(ScopesSection.class)
    @Effect(predicate = AuthTypeIsInteractive.class, type = EffectType.SHOW)
    interface ClientIdSection {
    }

    @Section(title = "Authentication")
    @Effect(predicate = AuthTypeIsInteractive.class, type = EffectType.SHOW)
    @After(ClientIdSection.class)
    interface AuthenticationSection {
    }

    enum AuthType {
            @Label("Interactive") //
            INTERACTIVE, //
            @Label("Service Account") //
            API_KEY;
    }

    enum ClientType {
            @Label("Default") //
            DEFAULT, //
            @Label("Custom") //
            CUSTOM;
    }

    interface AuthTypeRef extends Reference<AuthType> {
    }

    static class AuthTypeIsInteractive implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(AuthTypeRef.class).isOneOf(AuthType.INTERACTIVE);
        }
    }

    @Widget(title = "Authentication type", description = "Authentication method to use.")
    @ValueReference(AuthTypeRef.class)
    AuthType m_authType = AuthType.INTERACTIVE;

    APIKeySettings m_apiKeySettings = new APIKeySettings();

    @Layout(ScopesSection.class)
    ScopeSettings m_scopeSettings = new ScopeSettings();

    @ButtonWidget(actionHandler = LoginActionHandler.class, //
        updateHandler = LoginUpdateHandler.class, //
        showTitleAndDescription = false)
    @Widget(title = "Login", //
        description = "Clicking on login opens a new browser window/tab which "
            + "allows to interactively log into the service.")
    @Persistor(LoginCredentialRefPersistor.class)
    @Layout(AuthenticationSection.class)
    @Effect(predicate = AuthTypeIsInteractive.class, type = EffectType.SHOW)
    UUID m_loginCredentialRef;

    static final class LoginCredentialRefPersistor extends AbstractTokenCacheKeyPersistor {
        LoginCredentialRefPersistor() {
            super("loginCredentialRef");
        }
    }

    static class LoginActionHandler extends CancelableActionHandler<UUID, GoogleAuthenticatorSettings> {

        @Override
        protected UUID invoke(final GoogleAuthenticatorSettings settings, final DefaultNodeSettingsContext context)
            throws WidgetHandlerException {

            try {
                settings.validate();
            } catch (InvalidSettingsException e) { // NOSONAR
                throw new WidgetHandlerException(e.getMessage());
            }

            try {
                var userCreds = loginInteractively(settings);
                return CredentialCache.store(new GoogleCredential(userCreds));
            } catch (Exception e) {//NOSONAR
                throw new WidgetHandlerException(e.getMessage());
            }
        }

        private static UserCredentials loginInteractively(final GoogleAuthenticatorSettings settings)
            throws IOException, InvalidSettingsException {

            final GoogleClientSecrets clientSecrets;
            if (settings.m_clientType == ClientType.CUSTOM) {
                try (final var pathAccessor =
                    new FSLocationPathAccessor(settings.m_customClientIdFile.getFSLocation())) {
                    clientSecrets = ClientSecrets.loadClientSecrets(pathAccessor.getRootPath(null));
                }
            } else {
                clientSecrets = ClientSecrets.loadDefaultClientSecrets();
            }

            final var scopes = settings.m_scopeSettings.getScopes();

            return new TmpMemoryUserCredentialsStore(clientSecrets, scopes).loginInteractively();
        }

        @Override
        protected String getButtonText(final States state) {
            return switch (state) {
                case READY -> "Login";
                case CANCEL -> "Cancel login";
                case DONE -> "Login again";
                default -> null;
            };
        }
    }

    static class LoginUpdateHandler extends CancelableActionHandler.UpdateHandler<UUID, GoogleAuthenticatorSettings> {

        // FIXME this method override was added to work around issue UIEXT-2324
        // Once the issue is fixed it should be possible to remove this workaround,
        // because it has an undesired side-effect (the button never shows "logged in"
        // when the dialog is opened.
        @Override
        public ButtonChange<UUID, States> update(final GoogleAuthenticatorSettings settings,
            final DefaultNodeSettingsContext context) throws WidgetHandlerException {
            return new ButtonChange<>(States.READY);
        }
    }

    static final class UseCustomClientIdRef implements Reference<ClientType> {

    }

    @Widget(title = "Which client/app to use", //
            description = "Choose whether to use the default client/app or specify a custom one.", advanced = true)
    @Layout(ClientIdSection.class)
    @Effect(predicate = AuthTypeIsInteractive.class, type = EffectType.SHOW)
    @Migration(MigrationFromUseCustomClientId.class)
    @ValueReference(UseCustomClientIdRef.class)
    @ValueSwitchWidget
    ClientType m_clientType = ClientType.DEFAULT;

    static final class UseCustomClientId implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(UseCustomClientIdRef.class).isOneOf(ClientType.CUSTOM) //
                    .and(i.getPredicate(AuthTypeIsInteractive.class));
        }

    }

    static final class MigrationFromUseCustomClientId implements NodeSettingsMigration<ClientType> {

        private static final String CFG_USE_CUSTOM_CLIENT = "useCustomClientId";

        private static ClientType loadFromSettings(final NodeSettingsRO settings) {
            final var useCustomClient = settings.getBoolean(CFG_USE_CUSTOM_CLIENT, false);
            return useCustomClient ? ClientType.CUSTOM : ClientType.DEFAULT;
        }

        @Override
        public List<ConfigMigration<ClientType>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(MigrationFromUseCustomClientId::loadFromSettings) //
                .withDeprecatedConfigPath(CFG_USE_CUSTOM_CLIENT).build());
        }
    }

    @Widget(title = "ID file (JSON format)", //
        description = "The path to a JSON file with the custom client ID.", //
        advanced = true)
    @Layout(ClientIdSection.class)
    @Effect(predicate = UseCustomClientId.class, type = EffectType.SHOW)
    FileSelection m_customClientIdFile = new FileSelection();

    /**
     * Validates the settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (m_authType == AuthType.API_KEY) {
            m_apiKeySettings.validate();
        }

        m_scopeSettings.validate();
    }
}
