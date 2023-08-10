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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.CancelableActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.credentials.base.CredentialCache;
import org.knime.google.api.nodes.authconnector.auth.GoogleAuthLocationType;
import org.knime.google.api.nodes.authconnector.auth.GoogleAuthentication;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScope;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScopeRegistry;

import com.google.api.client.auth.oauth2.Credential;

/**
 * Node settings for the Google Authenticator node.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class GoogleAuthenticatorSettings implements DefaultNodeSettings {

    @Section(title = "Scopes")
    interface ScopesSection {
    }

    @Section(title = "Authentication")
    @After(ScopesSection.class)
    interface AuthenticationSection {
    }

    @Widget(title = "Scopes selection mode", description = "", hideTitle = true)
    @Layout(ScopesSection.class)
    @Signal(condition = ScopeSelectionIsSpecificScopes.class)
    @ValueSwitchWidget
    ScopesSelectionMode m_scopesSelectionMode = ScopesSelectionMode.SPECIFIC_SCOPES;

    enum ScopesSelectionMode {
            SPECIFIC_SCOPES, ALL_SCOPES;
    }

    static class ScopeSelectionIsSpecificScopes extends OneOfEnumCondition<ScopesSelectionMode> {
        @Override
        public ScopesSelectionMode[] oneOf() {
            return new ScopesSelectionMode[]{ScopesSelectionMode.SPECIFIC_SCOPES};
        }
    }

    @Widget(title = "Scopes", description = "The list of scopes to request for the access token.")
    @Layout(ScopesSection.class)
    @Effect(signals = ScopeSelectionIsSpecificScopes.class, type = EffectType.SHOW)
    @ArrayWidget
    @Persist(customPersistor = ScopeArrayPersistor.class)
    Scope[] m_scopes = new Scope[0];

    static final class Scope implements DefaultNodeSettings {
        @ChoicesWidget(choices = ScopeChoicesProvider.class)
        String m_scopeName;

        Scope() {
        }

        Scope(final String scopeName) {
            m_scopeName = scopeName;
        }

        static class ScopeChoicesProvider implements ChoicesProvider {
            @Override
            public String[] choices(final DefaultNodeSettingsContext context) {
                return KnimeGoogleAuthScopeRegistry.getInstance().getOAuthEnabledKnimeGoogleAuthScopes() //
                    .stream() //
                    .map(KnimeGoogleAuthScope::getAuthScopeName) //
                    .toArray(String[]::new);
            }

        }
    }

    private static final class ScopeArrayPersistor extends NodeSettingsPersistorWithConfigKey<Scope[]> {

        @Override
        public Scope[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(getConfigKey())) {
                return Arrays.stream(settings.getStringArray(getConfigKey()))//
                    .map(Scope::new)//
                    .toArray(Scope[]::new);
            } else {
                return new Scope[0];
            }
        }

        @Override
        public void save(final Scope[] obj, final NodeSettingsWO settings) {
            var stringArray = Arrays.stream(obj).map(s -> s.m_scopeName).toArray(String[]::new);
            settings.addStringArray(getConfigKey(), stringArray);
        }
    }

    @ButtonWidget(actionHandler = LoginActionHandler.class, //
        updateHandler = LoginUpdateHandler.class, //
        showTitleAndDescription = false)
    @Widget(title = "Login", //
        description = "Clicking on login opens a new browser window/tab which "
            + "allows to interactively log into the service.")
    @Persist(optional = true, hidden = true, customPersistor = TokenCacheKeyPersistor.class)
    @Layout(AuthenticationSection.class)
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
                var holder = new GoogleCredentialHolder();
                holder.m_token = fetchAccessToken(settings);
                holder.m_cacheKey = CredentialCache.store(holder);
                return holder.m_cacheKey;
            } catch (Exception e) {//NOSONAR
                throw new WidgetHandlerException(e.getMessage());
            }
        }

        private static Credential fetchAccessToken(final GoogleAuthenticatorSettings settings) throws IOException {
            return GoogleAuthentication.getCredential(GoogleAuthLocationType.MEMORY, null,
                getRelevantKnimeAuthScopes(settings), null);
        }

        private static List<KnimeGoogleAuthScope>
            getRelevantKnimeAuthScopes(final GoogleAuthenticatorSettings settings) {
            if (settings.m_scopesSelectionMode == ScopesSelectionMode.ALL_SCOPES) {
                return KnimeGoogleAuthScopeRegistry.getInstance().getOAuthEnabledKnimeGoogleAuthScopes();
            } else {
                return getSelectedScopes(settings);
            }
        }

        private static List<KnimeGoogleAuthScope> getSelectedScopes(final GoogleAuthenticatorSettings settings) {
            var scopeByName = KnimeGoogleAuthScopeRegistry.getInstance().getOAuthEnabledKnimeGoogleAuthScopes().stream()
                .collect(Collectors.toMap(KnimeGoogleAuthScope::getAuthScopeName, s -> s));

            return Stream.of(settings.m_scopes) //
                .map(s -> scopeByName.get(s.m_scopeName)) //
                .collect(Collectors.toList());
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

    private static class TokenCacheKeyPersistor extends NodeSettingsPersistorWithConfigKey<UUID> {

        @Override
        public UUID load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(getConfigKey())) {
                var uuidStr = settings.getString(getConfigKey());
                if (!StringUtils.isBlank(uuidStr)) {
                    final var uuid = UUID.fromString(uuidStr);
                    if (CredentialCache.get(uuid).isPresent()) {
                        return uuid;
                    }
                }
            }

            return null;
        }

        @Override
        public void save(final UUID uuid, final NodeSettingsWO settings) {
            if (uuid != null) {
                settings.addString(getConfigKey(), uuid.toString());
            }
        }
    }

    /**
     * Validates the settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (m_scopesSelectionMode == ScopesSelectionMode.SPECIFIC_SCOPES
            && (m_scopes == null || m_scopes.length == 0)) {
            throw new InvalidSettingsException("Please specify at least one scope");
        }
    }

}
