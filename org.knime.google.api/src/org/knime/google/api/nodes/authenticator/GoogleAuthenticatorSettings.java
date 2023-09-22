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
import java.util.UUID;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.And;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.rule.TrueCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.CancelableActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.credentials.base.GenericTokenHolder;
import org.knime.credentials.base.oauth.api.nodesettings.TokenCacheKeyPersistor;
import org.knime.google.api.nodes.authconnector.auth.GoogleAuthLocationType;
import org.knime.google.api.nodes.authconnector.auth.GoogleAuthentication;

import com.google.api.client.auth.oauth2.Credential;

/**
 * Node settings for the Google Authenticator node.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class GoogleAuthenticatorSettings implements DefaultNodeSettings {

    @Section(title = "API Key")
    @Effect(signals = AuthTypeIsInteractive.class, type = EffectType.HIDE)
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

    @Section(title = "Client ID", advanced = true)
    @After(ScopesSection.class)
    interface ClientIdSection {
    }

    @Section(title = "Authentication")
    @Effect(signals = AuthTypeIsInteractive.class, type = EffectType.SHOW)
    @After(ClientIdSection.class)
    interface AuthenticationSection {
    }

    enum AuthType {
        @Label("Interactive")
        INTERACTIVE,
        @Label("API Key")
        API_KEY;
    }

    static class AuthTypeIsInteractive extends OneOfEnumCondition<AuthType> {
        @Override
        public AuthType[] oneOf() {
            return new AuthType[] { AuthType.INTERACTIVE };
        }
    }

    @Widget(title = "Authentication type", description = "Authentication method to use.")
    @Signal(condition = AuthTypeIsInteractive.class)
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
    @Persist(optional = true, hidden = true, customPersistor = TokenCacheKeyPersistor.class)
    @Layout(AuthenticationSection.class)
    @Effect(signals = AuthTypeIsInteractive.class, type = EffectType.SHOW)
    UUID m_tokenCacheKey;

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
                var holder = GenericTokenHolder.store(fetchAccessToken(settings));
                return holder.getCacheKey();
            } catch (Exception e) {//NOSONAR
                throw new WidgetHandlerException(e.getMessage());
            }
        }

        private static Credential fetchAccessToken(final GoogleAuthenticatorSettings settings) throws IOException {
            return GoogleAuthentication.getCredential(GoogleAuthLocationType.MEMORY, null,
                settings.m_scopeSettings.getScopes(), settings.getClientIdFile());
        }

        @Override
        protected String getButtonText(final States state) {
            switch (state) {
                case READY:
                    return "Login";
                case CANCEL:
                    return "Cancel login";
                case DONE:
                    return "Login again";
                default:
                    return null;
            }
        }

    }

    static class LoginUpdateHandler extends CancelableActionHandler.UpdateHandler<UUID, GoogleAuthenticatorSettings> {
    }

    @Widget(title = "Use custom client ID", description = "Enable the option to use custom client ID.", advanced = true)
    @Layout(ClientIdSection.class)
    @Effect(signals = AuthTypeIsInteractive.class, type = EffectType.SHOW)
    @Signal(id = UseCustomClientIdSignal.class, condition = TrueCondition.class)
    boolean m_useCustomClientId;

    interface UseCustomClientIdSignal {
    }

    @Widget(title = "Custom client ID file (JSON format)",//
            description = "The path to a JSON file with the custom client ID.",//
            advanced = true)
    @Layout(ClientIdSection.class)
    @Effect(signals = {UseCustomClientIdSignal.class, AuthTypeIsInteractive.class}, operation = And.class,
        type = EffectType.SHOW)
    String m_customClientIdFile;

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

    private String getClientIdFile() {
        if (m_useCustomClientId) {
            return m_customClientIdFile;
        } else {
            return null;
        }
    }
}
