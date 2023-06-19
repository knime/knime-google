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
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.google.api.analytics.ga4.docs.ExternalLinks;

/**
 * Settings for a Google Analytics 4 Data API
 * <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/rest/v1beta/Metric">Metric</a>.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui*
final class GAMetric implements DefaultNodeSettings {

    // custom metrics are prefixed by colon
    private static final String PAT_STR = "^([a-zA-Z0-9_]+:)?[a-zA-Z0-9_]+$"; // NOSONAR pattern from Google
    private static final Pattern NAME_PATTERN = Pattern.compile(PAT_STR);

    @Widget(title = "Metric",
            description = """
                Specify the name of the metric.
                For example, the metric "eventCount" is the total number of events.

                Available metric names can be seen in the
                <a href=" """ + ExternalLinks.API_LIST_METRIC + """
                ">list of metrics</a> under the column "API Name".
                """)
    @TextInputWidget(pattern = PAT_STR)
    @ChoicesWidget(choices = MetricsChoicesProvider.class)
    String m_name;

    GAMetric() {
        // ser/de
    }

    GAMetric(final String name) {
        m_name = name;
    }

    void validate() throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotBlank(m_name),
            "Metric name cannot be blank.");
        if (StringUtils.isNotEmpty(m_name)) {
            CheckUtils.checkSetting(NAME_PATTERN.matcher(m_name).matches(),
                "Metric name must follow the pattern \"%s\".".formatted(PAT_STR));
        }
    }
}
