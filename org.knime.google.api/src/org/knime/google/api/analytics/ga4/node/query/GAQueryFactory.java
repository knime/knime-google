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
 *   2 Mar 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.query;

import org.knime.core.webui.node.dialog.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.dialog.impl.WebUINodeFactory;
import org.knime.google.api.analytics.ga4.port.GAConnectionPortObject;

/**
 * Factory for the Google Analytics Query node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui classes
public final class GAQueryFactory extends WebUINodeFactory<GAQueryNodeModel>{

    /** Constructor. */
    public GAQueryFactory() {
        super(CONFIG);
    }

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
            .name("Google Analytics Query")
            .icon("./googleanalyticsquery.png")
            .shortDescription("Query Google Analytics (GA4) properties.")
            .fullDescription("""
                    This node queries Google Analytics 4 properties using the
                    <a href="https://developers.google.com/analytics/devguides/reporting/data/v1">Google Analytics
                    Data API v1</a>.

                    The node supports building a <a href="https://support.google.com/analytics/answer/9212670?hl=en
                    &amp;ref_topic=13395703">Report</a> by specifying
                    <a href="https://support.google.com/analytics/answer/9143382?hl=en&amp;ref_topic=11151952">metrics</a>,
                    <a href="https://support.google.com/analytics/answer/9143382?hl=en&amp;ref_topic=11151952">dimensions</a>,
                    and date ranges.

                    In order to find available metrics and dimensions, you can use the
                    <a href="https://ga-dev-tools.google/ga4/dimensions-metrics-explorer/">GA4 Dimensions &amp; Metrics
                    Explorer</a> offered by Google.

                    <b>Note:</b>
                    This node can only be used to connect with Google Analytics 4 properties and is not
                    compatible with Universal Analytics.
                    To migrate a website using Universal Analytics to Google Analytics 4, you can find more information
                    in the <a href="https://support.google.com/analytics/answer/9744165?ref_topic=9303319#zippy=%2Cin-this-article">
                    official migration guide</a>.
                    """)
            .modelSettingsClass(GAQueryNodeSettings.class)
            .addInputPort("Google Analytics connection", GAConnectionPortObject.TYPE,
                "Google Analytics connection to use")
            .addOutputTable("Google Analytics data", "Google Analytics data according to the query.")
            .sinceVersion(5, 1, 0)
            .build();

    @Override
    public GAQueryNodeModel createNodeModel() {
        return new GAQueryNodeModel(CONFIG);
    }

}
