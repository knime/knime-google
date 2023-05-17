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
 *   13 Mar 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.query;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.google.api.ga4.docs.ExternalLinks;

/**
 * Settings for a Google Analytics 4 Data API
 * <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/rest/v1beta/Metric">Metric</a>.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui*
final class GAMetric implements DefaultNodeSettings {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$"); // NOSONAR pattern from Google

    @Widget(title = "Metric", description = "Define up to ten names of metrics. "
        + "Metrics are quantitative measurements of a report. For example, the metric eventCount is the total number of events. "
        + "Available names can be seen in the <a href=\"" + ExternalLinks.API_LIST_METRIC + "\">API documentation</a>.")
    @TextInputWidget(pattern = "[a-zA-Z0-9_]+")
    String m_name;

    @Widget(title = "Expression (optional)", description = """
            A mathematical expression for derived metrics.
            For example, the metric Event count per user is eventCount/totalUsers.
            """)
    String m_expression;

    @Widget(title = "Invisible", description = """
            Indicates if a metric is invisible in the report response.
            If a metric is invisible, the metric will not produce a column in the response,
            but can be used in metricFilter, orderBys, or a metric expression.
            """)
    boolean m_invisible;

    GAMetric() {
        // ser/de
    }

    GAMetric(final String name, final String exp, final boolean invis) {
        m_name = name;
        m_expression = exp;
        m_invisible = invis;
    }

    void validate() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_expression)) {
            CheckUtils.checkSetting(StringUtils.isNotBlank(m_name), "Metric name cannot be blank unless an expression is specified.");
        }
        if (StringUtils.isNotEmpty(m_name)) {
            CheckUtils.checkSetting(NAME_PATTERN.matcher(m_name).matches(), "Metric name must follow the pattern \"[a-zA-Z0-9_]+\".");
        }
    }
}
