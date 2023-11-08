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
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.LayoutGroup;
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
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.google.api.nodes.authenticator.ScopeSettings.CustomScope.CustomScopesPersistor;
import org.knime.google.api.scopes.KnimeGoogleAuthScopeRegistry;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Scope settings for the Google Authenticator node.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
@SuppressWarnings("restriction")
public class ScopeSettings implements DefaultNodeSettings, LayoutGroup {

    @Widget(title = "Standard/All/Custom scopes", description = """
            How to specify the scopes that will be requested during login.
            You can choose from a list of predefined <b>standard</b> scopes, select <b>all standard</b> scopes,
            or enter <b>custom</b> scopes manually.
            """, hideTitle = true)
    @ValueSwitchWidget
    @Signal(condition = ScopesSelectionMode.IsStandard.class)
    @Signal(condition = ScopesSelectionMode.IsCustom.class)
    ScopesSelectionMode m_scopesSelectionMode = ScopesSelectionMode.STANDARD;

    enum ScopesSelectionMode {
            STANDARD, ALL, CUSTOM;

        static class IsStandard extends OneOfEnumCondition<ScopesSelectionMode> {
            @Override
            public ScopesSelectionMode[] oneOf() {
                return new ScopesSelectionMode[]{STANDARD};
            }
        }

        static class IsCustom extends OneOfEnumCondition<ScopesSelectionMode> {
            @Override
            public ScopesSelectionMode[] oneOf() {
                return new ScopesSelectionMode[]{CUSTOM};
            }
        }
    }

    @Widget(title = "Scopes", description = "The list of scopes to request for the access token.")
    @ArrayWidget(addButtonText = "Add Scope")
    @Effect(signals = ScopesSelectionMode.IsStandard.class, type = EffectType.SHOW)
    @Persist(customPersistor = StandardScope.ScopeArrayPersistor.class)
    StandardScope[] m_standardScopes = new StandardScope[0];

    static final class StandardScope implements DefaultNodeSettings {
        @Widget(hideTitle = true)
        @ChoicesWidget(choices = ScopeChoicesProvider.class)
        String m_scopeId;

        StandardScope() {
        }

        StandardScope(final String scopeId) {
            m_scopeId = scopeId;
        }

        static class ScopeChoicesProvider implements ChoicesProvider {
            @Override
            public IdAndText[] choicesWithIdAndText(final DefaultNodeSettingsContext context) {
                return KnimeGoogleAuthScopeRegistry.getInstance().getOAuthEnabledKnimeGoogleAuthScopes() //
                    .stream() //
                    .map(s -> new IdAndText(s.getScopeID(), s.getAuthScopeName())) //
                    .toArray(IdAndText[]::new);
            }
        }

        static final class ScopeArrayPersistor extends NodeSettingsPersistorWithConfigKey<StandardScope[]> {
            @Override
            public StandardScope[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
                if (settings.containsKey(getConfigKey())) {
                    return Arrays.stream(settings.getStringArray(getConfigKey()))//
                        .map(StandardScope::new)//
                        .toArray(StandardScope[]::new);
                } else {
                    return new StandardScope[0];
                }
            }

            @Override
            public void save(final StandardScope[] obj, final NodeSettingsWO settings) {
                var stringArray = Arrays.stream(obj).map(s -> s.m_scopeId).toArray(String[]::new);
                settings.addStringArray(getConfigKey(), stringArray);
            }
        }
    }

    @Widget(title = "Custom scopes", description = "Enter the custom scopes to use.", hideTitle = true)
    @ArrayWidget(addButtonText = "Add")
    @Effect(signals = ScopesSelectionMode.IsCustom.class, type = EffectType.SHOW)
    @Persist(customPersistor = CustomScopesPersistor.class)
    CustomScope[] m_customScopes = new CustomScope[0];

    static class CustomScope implements DefaultNodeSettings {
        @Widget(hideTitle = true)
        String m_scope;

        CustomScope(final String scope) {
            m_scope = scope;
        }

        CustomScope() {
        }

        static class CustomScopesPersistor extends NodeSettingsPersistorWithConfigKey<CustomScope[]> {
            @Override
            public CustomScope[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return Stream.of(settings.getStringArray(getConfigKey())) //
                    .map(CustomScope::new) //
                    .toArray(CustomScope[]::new);
            }

            @Override
            public void save(final CustomScope[] obj, final NodeSettingsWO settings) {
                var strings = Stream.of(obj).map(s -> s.m_scope).toArray(String[]::new);
                settings.addStringArray(getConfigKey(), strings);
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
    @JsonIgnore
    public List<String> getScopes() {
        return switch (m_scopesSelectionMode) {
            case STANDARD -> getSelectedStandardScopes();
            case ALL -> getAllStandardScopes();
            case CUSTOM -> getSelectedCustomScopes();
            default -> throw new IllegalArgumentException("Unexpected scope selection type: " + m_scopesSelectionMode);
        };
    }

    private List<String> getSelectedStandardScopes() {
        List<String> scopeIds = Stream.of(m_standardScopes) //
            .map(s -> s.m_scopeId).collect(Collectors.toList());
        return KnimeGoogleAuthScopeRegistry
            .getAuthScopes(KnimeGoogleAuthScopeRegistry.getInstance().getScopesFromString(scopeIds));
    }

    private static List<String> getAllStandardScopes() {
        return KnimeGoogleAuthScopeRegistry
            .getAuthScopes(KnimeGoogleAuthScopeRegistry.getInstance().getOAuthEnabledKnimeGoogleAuthScopes());
    }

    private List<String> getSelectedCustomScopes() {
        return Stream.of(m_customScopes).map(s -> s.m_scope).collect(Collectors.toList());
    }
}
