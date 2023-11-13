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
 *   17 May 2023 (carlwitt): created
 */
package org.knime.google.api.analytics.ga4.docs;

/**
 * User facing links via node description or widget help texts.
 *
 * Make sure hl=en sets the language in order to get a uniform experience and the correct API identifiers (for instance,
 * issuing a query with dimension = "Stadt" will return an API error, it needs to be "city").
 *
 * Make sure to escape the ampersand
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public final class ExternalLinks {

    public static final String API_ADMIN = "https://developers.google.com/analytics/devguides/config/admin/v1?hl=en";

    public static final String API_DATA = "https://developers.google.com/analytics/devguides/reporting/data/v1?hl=en";

    /** List of standard dimensions. */
    public static final String API_LIST_DIMENSION =
        "https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema?hl=en#dimensions";

    /** List of standard metrics. */
    public static final String API_LIST_METRIC =
        "https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema?hl=en#metrics";

    public static final String API_RESPONSE_METADATA =
        "https://developers.google.com/analytics/devguides/reporting/data/v1/rest/v1beta/ResponseMetaData?hl=en";

    public static final String API_QUOTAS =
        "https://developers.google.com/analytics/devguides/reporting/data/v1/quotas?hl=en";

    /** Explains how to create an analytics account. */
    public static final String EXPLAIN_ACCOUNT = "https://support.google.com/analytics/answer/9304153?hl=en#account";

    /** Explains how to create a property. */
    public static final String EXPLAIN_PROPERTY = "https://support.google.com/analytics/answer/9304153?hl=en#property";

    /** Explains the report concept. */
    public static final String EXPLAIN_REPORT =
        "https://support.google.com/analytics/answer/9212670?hl=en&amp;ref_topic=13395703";

    /** Explains the metrics and dimensions concept. */
    public static final String EXPLAIN_METRICS_AND_DIMENSIONS =
        "https://support.google.com/analytics/answer/9143382?hl=en&amp;ref_topic=11151952";

    /** How to migrate to Google Analytics 4. */
    public static final String EXPLAIN_MIGRATION = "https://support.google.com/analytics/answer/10759417?hl=en";

    /** Web page that allows to investigate your metrics. */
    public static final String EXPLORER_TOOL = "https://ga-dev-tools.google/ga4/dimensions-metrics-explorer/";
}
