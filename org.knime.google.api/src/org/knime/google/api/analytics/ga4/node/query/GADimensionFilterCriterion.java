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
 *   4 May 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.query;

import java.util.Objects;
import java.util.function.Function;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Before;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldBasedNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.google.api.analytics.ga4.node.query.GADimensionFilterCriterion.GADimensionFilterCriterionLayout.AfterFilterTypeSelection;
import org.knime.google.api.analytics.ga4.node.query.GADimensionFilterCriterion.GADimensionFilterCriterionLayout.BeforeFilterTypeSelection;
import org.knime.google.api.analytics.ga4.node.query.GADimensionFilterCriterion.GADimensionFilterCriterionLayout.FilterTypeSelection;

/**
 * A Google Analytics dimension filter criterion.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui* classes
@Persistor(org.knime.google.api.analytics.ga4.node.query.GADimensionFilterCriterion.GADimensionFilterSettingsPersistor.class)
final class GADimensionFilterCriterion implements DefaultNodeSettings {

    interface GADimensionFilterCriterionLayout {
        @Before(FilterTypeSelection.class)
        interface BeforeFilterTypeSelection{}

        @HorizontalLayout
        @After(BeforeFilterTypeSelection.class)
        @Before(AfterFilterTypeSelection.class)
        interface FilterTypeSelection {}

        @HorizontalLayout
        @After(FilterTypeSelection.class)
        interface AfterFilterTypeSelection {}

    }

    @Widget(title = "Dimension", description = "The name of the dimension to filter on.")
    @ChoicesWidget(choices = DimensionChoicesProvider.class)
    @Layout(BeforeFilterTypeSelection.class)
    String m_name;

    @Widget(title = "Invert matches",
            description = """
                    Invert results matched by the criterion in order to <i>exclude</i> certain events from the result.
                    """)
    @Layout(BeforeFilterTypeSelection.class)
    boolean m_isNegated;

    interface IsStringFilterSelected {}

    @Widget(title = "Match by",
            description = """
                    Match results by comparing values of the dimension to strings or based on list inclusion.
                    """)
    @Signal(id = IsStringFilterSelected.class, condition = DimensionFilterType.IsString.class)
    @ValueSwitchWidget
    @Layout(FilterTypeSelection.class)
    DimensionFilterType m_selectedType = DimensionFilterType.STRING;

    @Widget(title = "Case handling",
            description = """
                    Specifies whether strings should be matched case-insensitive or case-sensitive.
                    """, advanced = true)
    @ValueSwitchWidget
    @Layout(FilterTypeSelection.class)
    CaseSensitivity m_caseSensitivity = CaseSensitivity.CASE_INSENSITIVE;

    // BEGIN Union type
    @Layout(AfterFilterTypeSelection.class)
    GAStringFilter m_stringFilter;

    @Layout(AfterFilterTypeSelection.class)
    GAInListFilter m_inListFilter;
    // END Union type

    GADimensionFilterCriterion() {
        // ser/de
    }

    public GADimensionFilterCriterion(final String name, final GAStringFilter stringFilter,
            final CaseSensitivity caseSensitivity, final boolean isNegated) {
        m_name = Objects.requireNonNull(name);
        m_selectedType = DimensionFilterType.STRING;
        m_stringFilter = Objects.requireNonNull(stringFilter);
        m_caseSensitivity = Objects.requireNonNull(caseSensitivity);
        m_isNegated = isNegated;
    }

    public GADimensionFilterCriterion(final String name, final GAInListFilter inListFilter,
            final CaseSensitivity caseSensitivity, final boolean isNegated) {
        m_name = Objects.requireNonNull(name);
        m_selectedType = DimensionFilterType.IN_LIST;
        m_inListFilter = Objects.requireNonNull(inListFilter);
        m_caseSensitivity = Objects.requireNonNull(caseSensitivity);
        m_isNegated = isNegated;
    }

    enum DimensionFilterType {
        @Label("String comparison")
        STRING,
        @Label("List inclusion")
        IN_LIST;

        static class IsString extends OneOfEnumCondition<DimensionFilterType> {

            @Override
            public DimensionFilterType[] oneOf() {
                return new DimensionFilterType[] {STRING};
            }

        }
    }

    void validate() throws InvalidSettingsException {
        checkUnionValid(InvalidSettingsException::new);
        switch (m_selectedType) { // NOSONAR exhaustiveness check
            case STRING -> m_stringFilter.validate();
            case IN_LIST -> m_inListFilter.validate();
        }
        CheckUtils.checkSettingNotNull(m_caseSensitivity, "Case sensitivity is missing.");
    }

    /**
     * Checks that the part of the "union" we are interested in is valid. Due to the way the UI handles the display
     * of the union, both could be set at the same time if mistakenly enabled in the UI.
     *
     * @param <X> exception type
     * @param toThrowable function to provide an exception given a message
     * @throws X the exception in case the check fails
     */
    private <X extends Throwable> void checkUnionValid(final Function<String, X> toThrowable) throws X {
        switch (m_selectedType) { // NOSONAR exhaustiveness check
            case STRING -> CheckUtils.check(m_stringFilter != null, toThrowable, () -> "String filter is missing.");
            case IN_LIST -> CheckUtils.check(m_inListFilter != null, toThrowable, () -> "InList filter is missing.");
        }
    }

    /**
     * Custom persistor for the union of string- and list-based filter criteria.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    static final class GADimensionFilterSettingsPersistor implements NodeSettingsPersistor<GADimensionFilterCriterion> {

        private static final String CFG_KEY_FILTER_TYPE = "selectedType";
        private static final String CFG_KEY_NAME = "name";
        private static final String CFG_KEY_CASE_SENSITIVITY = "caseSensitivity";
        private static final String CFG_KEY_IS_NEGATED = "isNegated";

        @Override
        public GADimensionFilterCriterion load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var selected = valueOf(DimensionFilterType.class, settings, CFG_KEY_FILTER_TYPE);
            final var name = settings.getString(CFG_KEY_NAME);
            final var caseSensitivity = valueOf(CaseSensitivity.class, settings, CFG_KEY_CASE_SENSITIVITY);
            final var isNegated = settings.getBoolean(CFG_KEY_IS_NEGATED);
            return switch (selected) { // NOSONAR exhaustiveness
                case STRING -> new GADimensionFilterCriterion(name,
                    new FieldBasedNodeSettingsPersistor<>(GAStringFilter.class).load(settings),
                    caseSensitivity, isNegated);
                case IN_LIST -> new GADimensionFilterCriterion(name,
                    new FieldBasedNodeSettingsPersistor<>(GAInListFilter.class).load(settings),
                    caseSensitivity, isNegated);
            };
        }

        private static <T extends Enum<T>> T valueOf(final Class<T> enumClass, final NodeSettingsRO settings,
            final String key) throws InvalidSettingsException {
            return valueOf(enumClass, settings, key, null);
        }

        private static <T extends Enum<T>> T valueOf(final Class<T> enumClass, final NodeSettingsRO settings,
                final String key, final T defaultValue) throws InvalidSettingsException {
            if (defaultValue != null && !settings.containsKey(key)) {
                return defaultValue;
            }
            final var enumValue = settings.getString(key);
            try {
                return Enum.valueOf(enumClass, enumValue);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException("Unknown value \"%s\" for type \"%s\"".formatted(enumValue,
                    enumClass.getSimpleName()), e);
            }
        }

        @Override
        public void save(final GADimensionFilterCriterion filter, final NodeSettingsWO settings) {
            settings.addString(CFG_KEY_FILTER_TYPE, filter.m_selectedType.name());
            settings.addString(CFG_KEY_NAME, filter.m_name);
            settings.addString(CFG_KEY_CASE_SENSITIVITY, filter.m_caseSensitivity.name());
            settings.addBoolean(CFG_KEY_IS_NEGATED, filter.m_isNegated);
            switch (filter.m_selectedType) { // NOSONAR exhaustiveness
                case STRING -> new FieldBasedNodeSettingsPersistor<>(GAStringFilter.class)
                    .save(filter.m_stringFilter, settings);
                case IN_LIST -> new FieldBasedNodeSettingsPersistor<>(GAInListFilter.class)
                    .save(filter.m_inListFilter, settings);
            }
        }
    }
}