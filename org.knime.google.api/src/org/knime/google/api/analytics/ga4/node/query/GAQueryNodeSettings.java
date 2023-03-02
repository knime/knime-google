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

import java.util.HashSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.impl.Schema;

/**
 * Settings for the Google Analytics 4 query node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // WebUI* classes
final class GAQueryNodeSettings implements DefaultNodeSettings {

    /** The Google Analytics API restriction for one API call. */
    private static final int MAX_NUM_METRICS = 10;
    /** The Google Analytics API restriction for one API call. */
    private static final int MAX_NUM_DIMENSIONS = 9;
    /** The Google Analytics API restriction for one API call. */
    private static final int MAX_NUM_DATE_RANGES = 4;

    /** Metrics to query. */
    @Schema(title = "Metrics (up to ten)",
            description = """
            Define up to ten names of metrics. Available names can be seen in the
            <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema#metrics">
            API documentation</a>.
                """,
            minLength = 1,
            maxLength = MAX_NUM_METRICS)
    GAMetric[] m_gaMetrics;

    /** Dimensions to query metrics under. */
    @Schema(title = "Dimensions (up to nine)",
            description = """
                Define up to nine names of dimensions. Available names can be seen in the
                <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema#dimensions">
                API documentation</a>.
                    """,
        minLength = 0,
        maxLength = MAX_NUM_DIMENSIONS)
    GADimension[] m_gaDimensions;

    /** Date ranges to make an API call with. */
    @Schema(title = "Date Ranges (up to four)",
            description = """
                Define up to four date ranges (with optional names). When multiple date ranges are specified, a column
                with the name of the range is added automatically.
                    """,
            minLength = 1,
            maxLength = MAX_NUM_DATE_RANGES)
    GADateRange[] m_dateRanges;

    @Schema(title = "Include date range name column",
            description = """
                Include a column containing the name of the date range. If a date range is not given a name
                explicitly, Google Analytics will auto-generate a name.
                    """)
    boolean m_includeDateRangeNameColumn = true;

    @Schema(title = "Order by", description = "Order the output by the metrics/dimensions specified. All "
        + "metrics/dimensions to order by must be included in the report and must be visible.")
    GAOrderBy[] m_gaOrderBy;

    @Schema(title = "Currency code", description = "Currency code to use, specified in three letter ISO-4217 format. "
        + "If left empty, the report uses the property's default currency.")
    String m_currencyCode;

    @Schema(title = "Keep empty rows",
            description = "If enabled, rows will also be returned if all their metrics are equal to 0. "
                + "Otherwise they are omitted from the result.")
    boolean m_keepEmptyRows;

    @Schema(title = "Output Response Metadata as Flow Variables",
            description = """
                    If enabled, outputs response metadata from the Google Analytics API as flow variables.
                    In particular, the following fields are returned: <tt>dataLossFromOtherRow</tt>,
                    <tt>currencyCode</tt>, <tt>timeZone</tt>, <tt>emptyReason</tt>, <tt>subjectToThresholding</tt>.
                    The format of the flow variable name follows the pattern "analytics.response.$FIELD", where $FIELD
                    specifies the field name.

                    More information can be obtained from the
                    <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/rest/v1beta/ResponseMetaData">
                    API documentation</a>.
                    """)
    boolean m_returnResponseMetadata;

    @Schema(title = "Output Property Quotas as Flow Variables",
        description = """
            Return the Property Quotas as flow variables. Each quota will indicate the "consumed" and "remaining"
            tokens after the last API request of the node. The format of the flow variable name follows the pattern
            "analytics.quota.$QUOTA.$TYPE", where $QUOTA specifies the quota and $TYPE is either "consumed" or
            "remaining".
            Available quotas can be seen in the
            <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/quotas">API documentation</a>.

            Retrieving a lot of data (many rows, many columns, or long date ranges) or specifying complex filter
            criteria may be responsible for consumption of many tokens per node execution.
                """)
    boolean m_returnPropertyQuota;


    /**
     * Validates the current state of the settings instance. Intended to be called from the model.
     */
    void validate() throws InvalidSettingsException {
        CheckUtils.checkSetting(ArrayUtils.isNotEmpty(m_gaMetrics),
            "The Google Analytics query needs at least one metric.");
        CheckUtils.checkSetting(m_gaMetrics.length <= MAX_NUM_METRICS,
                """
                   There are too many metrics specified.

                   You can only query a maximum of %d metrics at once.
                   Use another Google Analytics Query node and combine their results, for example using a Joiner node.
                """, MAX_NUM_METRICS);
        for (final var metric : m_gaMetrics) {
            metric.validate();
        }

        CheckUtils.checkSetting(m_gaDimensions == null || m_gaDimensions.length <= MAX_NUM_DIMENSIONS,
                """
                   There are too many dimensions specified.

                   Google Analytics supports only up to %d dimensions at once.
                """, MAX_NUM_DIMENSIONS);
        for (final var dim : m_gaDimensions) {
            dim.validate();
        }

        CheckUtils.checkSetting(ArrayUtils.isNotEmpty(m_dateRanges),
            "The Google Analytics query is missing a date range.");
        CheckUtils.checkSetting(m_dateRanges.length <= MAX_NUM_DATE_RANGES,
                """
                   There are too many date ranges specified.

                   Google Analytics supports only up to %d date ranges at once.
                   Use another Google Analytics Query node and combine there results, for example using a Concatenate
                   node.
                """
                , MAX_NUM_DATE_RANGES);
        final var names = new HashSet<String>(4);
        for (final var range : m_dateRanges) {
            final var name = range.m_rangeName;
            // empty name is ok, since the Analytics API will generate a range name
            if (name != null && StringUtils.isNotEmpty(name)) {
                range.validate();
                if (names.contains(name)) {
                    throw new InvalidSettingsException("Ambiguous date range name %s.".formatted(name));
                }
                // Check with auto-generated names by GA API?
                names.add(name);
            }
        }

        if (m_gaOrderBy != null) {
            for (final var o : m_gaOrderBy) {
                // order by's must be in metrics/dimensions (the metric must be visible)
                o.validate(m_gaMetrics, m_gaDimensions);
            }
        }
    }

    GAQueryNodeSettings() {
        // for de-/serialization
    }

    GAQueryNodeSettings(@SuppressWarnings("unused") final SettingsCreationContext ctx) { // NOSONAR required by framework
        m_gaMetrics = new GAMetric[] { new GAMetric("activeUsers", null, false) };
        m_gaDimensions = new GADimension[] { new GADimension("city") };
        m_dateRanges = new GADateRange[] { GADateRange.lastWeek() };
        m_gaOrderBy = new GAOrderBy[0];
    }

}
