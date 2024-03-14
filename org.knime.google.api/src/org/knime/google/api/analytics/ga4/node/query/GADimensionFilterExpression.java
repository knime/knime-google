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
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.HasMultipleItemsCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * An expression for combining dimension filter critera.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui* classes
final class GADimensionFilterExpression implements DefaultNodeSettings, WidgetGroup {

    @Widget(title = "Filter if matched by",
            description = """
                    Output the event if it is matched by:
                    <ul>
                        <li><b>All criteria</b>: an event is included if it is matched by <i>all</i> of the criteria
                        (intersection of matches)</li>
                        <li><b>Any criterion</b>: an event is included if it is matched by <i>at least one</i> of the
                        criteria (union of matches)</li>
                    </ul>
                    """)
    @Effect(signals = HasMultipleItemsCondition.class, type = EffectType.SHOW)
    @ValueSwitchWidget
    GAFilterGroup m_connectVia = GAFilterGroup.AND;

    @Signal(condition = HasMultipleItemsCondition.class)
    @ArrayWidget(addButtonText = "Add filter criterion", elementTitle = "Filter criterion")
    @Widget(title = "Dimension filter", description = """
            <p>
            Data can be filtered by comparing dimension values to strings or based on list inclusion.
            </p>
            <p>
            Each filter criterion can be negated to invert its results, for example to <i>exclude</i> results whose
            dimension values are in the specified list.
            The dimension filter can be configured such that <i>all</i> criteria
            must match for a result to be returned, or such that <i>any</i> criterion must match.
            </p>
            """)
    GADimensionFilterCriterion[] m_filters = new GADimensionFilterCriterion[0];

    GADimensionFilterExpression() {
        // ser/de and for optional annotation in settings
    }

    GADimensionFilterExpression(final GAFilterGroup connectVia, final GADimensionFilterCriterion[] filters) {
        m_connectVia = Objects.requireNonNull(connectVia);
        m_filters = checkFilters(Objects.requireNonNull(filters), IllegalArgumentException::new);
    }

    private static final <X extends Throwable> GADimensionFilterCriterion[] checkFilters(
            final GADimensionFilterCriterion[] filters, final Function<String, X> toThrowable) throws X {
        CheckUtils.check(filters != null && filters.length > 0, toThrowable,
                () -> "At least one criterion is required in a dimension filter.");
        return filters;
    }

    void validate() throws InvalidSettingsException {
        if (m_filters != null) {
            for (final var filter: m_filters) {
                filter.validate();
            }
        }
    }
}
