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
 *   Aug 31, 2023 (Alexander Bondaletov, Redfield SE): created
 */
package org.knime.google.api.nodes.authenticator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget.ElementLayout;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoice;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.google.api.nodes.authenticator.GoogleAuthenticatorSettings.AuthType;
import org.knime.google.api.nodes.authenticator.GoogleAuthenticatorSettings.AuthTypeRef;
import org.knime.google.api.nodes.authenticator.ScopeSettings.CustomScope.CustomScopesPersistor;
import org.knime.google.api.scopes.KnimeGoogleAuthScopeRegistry;

/**
 * Scope settings for the Google Authenticator node.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class ScopeSettings implements WidgetGroup, PersistableSettings {

    @Widget(title = "Scope type", description = """
            Scopes are
            <a href="https://developers.google.com/identity/protocols/oauth2/scopes">
            permissions</a> that need to be requested during login. They specify what the resulting
            access token can be used for. This setting defines whether to select scopes from a list of predefined
            <b>standard</b> scopes or to enter <b>custom</b> scopes manually.
            """)
    @ValueSwitchWidget
    @ValueReference(ScopesSelectionModeRef.class)
    ScopesSelectionMode m_scopesSelectionMode = ScopesSelectionMode.STANDARD;

    enum ScopesSelectionMode {
            STANDARD, CUSTOM;
    }

    interface ScopesSelectionModeRef extends Reference<ScopesSelectionMode> {
    }

    static final class ScopesSelectionModeIsStandard implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ScopesSelectionModeRef.class).isOneOf(ScopesSelectionMode.STANDARD);
        }
    }

    static final class ScopesSelectionModeIsCustom implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ScopesSelectionModeRef.class).isOneOf(ScopesSelectionMode.CUSTOM);
        }
    }

    @Widget(title = "Standard scopes", description = """
            Choose scopes from a predefined list of standard scopes. Scopes are
            <a href="https://developers.google.com/identity/protocols/oauth2/scopes">
            permissions</a> and define what the resulting access token can be used for.
            """)
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add scope")
    @Effect(predicate = ScopesSelectionModeIsStandard.class, type = EffectType.SHOW)
    @Persistor(StandardScope.ScopeArrayPersistor.class)
    @Migrate(loadDefaultIfAbsent = true)
    StandardScope[] m_standardScopes = new StandardScope[0];

    static final class StandardScope implements DefaultNodeSettings {
        @Widget(title = "Scope/permission", description = "")
        @ChoicesProvider(ScopeChoicesUpdateHandler.class)
        String m_scopeId;

        StandardScope() {
        }

        StandardScope(final String scopeId) {
            m_scopeId = scopeId;
        }

        static class ScopeChoicesUpdateHandler implements StringChoicesProvider {

            private Supplier<AuthType> m_authTypeSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
                m_authTypeSupplier = initializer.computeFromValueSupplier(AuthTypeRef.class);
            }

            @Override
            public List<StringChoice> computeState(final DefaultNodeSettingsContext context) {
                final var authType = m_authTypeSupplier.get();
                final var registry = KnimeGoogleAuthScopeRegistry.getInstance();
                final var scopes =
                    authType == AuthType.API_KEY ? registry.getServiceAccountEnabledKnimeGoogleAuthScopes()
                        : registry.getOAuthEnabledKnimeGoogleAuthScopes();

                return scopes.stream().map(s -> new StringChoice(s.getScopeID(), s.getAuthScopeName())) //
                    .sorted(Comparator.comparing(StringChoice::text)) //
                    .toList();
            }
        }

        static final class ScopeArrayPersistor implements NodeSettingsPersistor<StandardScope[]> {

            private static final String CONFIG_KEY = "standardScopes";

            @Override
            public StandardScope[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return Arrays.stream(settings.getStringArray(CONFIG_KEY))//
                    .map(StandardScope::new)//
                    .toArray(StandardScope[]::new);
            }

            @Override
            public void save(final StandardScope[] obj, final NodeSettingsWO settings) {
                var stringArray = Arrays.stream(obj).map(s -> s.m_scopeId).toArray(String[]::new);
                settings.addStringArray(CONFIG_KEY, stringArray);
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{CONFIG_KEY}};
            }
        }
    }

    @Widget(title = "Custom scopes", description = """
            Enter a list of custom scopes to request during login. Scopes are
            <a href="https://developers.google.com/identity/protocols/oauth2/scopes">
            permissions</a> and define what the resulting access token can be used for.
            """)
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add scope")
    @Effect(predicate = ScopesSelectionModeIsCustom.class, type = EffectType.SHOW)
    @Persistor(CustomScopesPersistor.class)
    CustomScope[] m_customScopes = new CustomScope[0];

    static class CustomScope implements DefaultNodeSettings {
        @Widget(title = "Scope/permission", description = "")
        String m_scope;

        CustomScope(final String scope) {
            m_scope = scope;
        }

        CustomScope() {
        }

        static class CustomScopesPersistor implements NodeSettingsPersistor<CustomScope[]> {

            private static final String CONFIG_KEY = "customScopes";

            @Override
            public CustomScope[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return Stream.of(settings.getStringArray(CONFIG_KEY)) //
                    .map(CustomScope::new) //
                    .toArray(CustomScope[]::new);
            }

            @Override
            public void save(final CustomScope[] obj, final NodeSettingsWO settings) {
                var strings = Stream.of(obj).map(s -> s.m_scope).toArray(String[]::new);
                settings.addStringArray(CONFIG_KEY, strings);
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{CONFIG_KEY}};
            }
        }
    }

    /**
     * Validates the settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (m_scopesSelectionMode == ScopesSelectionMode.STANDARD) {
            validateScopes(m_standardScopes, s -> s.m_scopeId);
        }
        if (m_scopesSelectionMode == ScopesSelectionMode.CUSTOM) {
            validateScopes(m_customScopes, s -> s.m_scope);
        }
    }

    private static <T> void validateScopes(final T[] scopes, final Function<T, String> toString)
        throws InvalidSettingsException {
        if (scopes == null || scopes.length == 0) {
            throw new InvalidSettingsException("Please specify at least one scope");
        }

        var pos = 1;
        for (final var scope : scopes) {
            if (StringUtils.isBlank(toString.apply(scope))) {
                throw new InvalidSettingsException("Please remove blank scope at position " + pos);
            }

            pos++;
        }
    }

    /**
     * @return The selected scopes.
     */
    public List<String> getScopes() {
        return switch (m_scopesSelectionMode) {
            case STANDARD -> getSelectedStandardScopes();
            case CUSTOM -> getSelectedCustomScopes();
            default -> throw new IllegalArgumentException("Unexpected scope selection type: " + m_scopesSelectionMode);
        };
    }

    private List<String> getSelectedStandardScopes() {
        List<String> scopeIds = Stream.of(m_standardScopes) //
            .map(s -> s.m_scopeId)//
            .toList();
        return KnimeGoogleAuthScopeRegistry
            .getAuthScopes(KnimeGoogleAuthScopeRegistry.getInstance().getScopesFromString(scopeIds));
    }

    private List<String> getSelectedCustomScopes() {
        return Stream.of(m_customScopes).map(s -> s.m_scope).toList();
    }
}
