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

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.google.api.analytics.ga4.docs.ExternalLinks;
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
            .shortDescription("Query a connected Google Analytics 4 property.")
            .fullDescription("""
                    <p>This node queries a connected Google Analytics 4 property using the
                    <a href="%s">Google Analytics Data API v1</a>.</p>
                    <p>
                    The node supports building a
                    <a href="%s">Report</a> by specifying <a href="%s"> metrics, dimensions</a>, and date ranges.
                    </p>
                    <p>
                    In order to find available metrics and dimensions, you can use the
                    <a href="%s">GA4 Dimensions &amp; Metrics Explorer</a> offered by Google.
                    </p>
                    <p><b>Note:</b>
                    This node can only be used to connect with Google Analytics 4 properties and is not
                    compatible with Universal Analytics.
                    To migrate a website using Universal Analytics to Google Analytics 4, you can find more information
                    in the <a href="%s">official migration guide</a>.
                    </p>
                    """.formatted(ExternalLinks.API_DATA,
                        ExternalLinks.EXPLAIN_REPORT,
                        ExternalLinks.EXPLAIN_METRICS_AND_DIMENSIONS,
                        ExternalLinks.EXPLORER_TOOL,
                        ExternalLinks.EXPLAIN_MIGRATION))
            .modelSettingsClass(GAQueryNodeSettings.class)
            .addInputPort("Google Analytics Connection", GAConnectionPortObject.TYPE,
                "The connection to a Google Analytics 4 property.")
            .addOutputTable("Google Analytics data", "A table with data according to the query.")
            .sinceVersion(5, 1, 0)
            .build();

    @Override
    public GAQueryNodeModel createNodeModel() {
        return new GAQueryNodeModel(CONFIG);
    }

}
