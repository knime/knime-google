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
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Advanced;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget.ElementLayout;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation.PatternValidation;
import org.knime.google.api.analytics.ga4.docs.ExternalLinks;

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

    @Section(title = "Metrics")
    interface MetricsSection {
    }

    /** Metrics to query. */
    @Widget(title = "Metrics", description = """
            <p>
            Metrics are quantitative measurements of event data for your Google Analytics property.
            </p>
            <p>
            Specify at least one and up to ten metrics to include in your query.
            Metrics can be specified by name and either be built-in metrics or custom metrics defined in your property.
            </p>
            <p>
            Built-in metrics can be seen in the <a href=" """ + ExternalLinks.API_LIST_METRIC + """
            ">list of metrics</a> under the column &quot;API Name&quot;.
            </p>
            """)
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add metric",
        showSortButtons = true) // TODO add validation constraints min = 1, max = MAX_NUM_METRICS
    @Layout(MetricsSection.class)
    GAMetric[] m_gaMetrics = new GAMetric[0];

    @Section(title = "Dimensions")
    @After(MetricsSection.class)
    interface DimensionsSection {
    }

    /** Dimensions to query metrics under. */
    @Widget(title = "Dimensions", description = """
            <p>
            Dimensions are attributes of the event data and their values are always strings.
            </p>
            <p>
            Specify up to nine dimensions by name. Available dimension names can be seen in the
            <a href=" """ + ExternalLinks.API_LIST_DIMENSION + """
            ">list of dimensions</a> under the column &quot;API Name&quot;.
            </p>
            """)
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add dimension",
        showSortButtons = true) // TODO add validation constraints max = MAX_NUM_DIMENSIONS
    @Layout(DimensionsSection.class)
    GADimension[] m_gaDimensions = new GADimension[0];

    /** Date ranges to make an API call with. */
    @Widget(title = "Date ranges", description = """
            <p>
            A date range specifies the range of dates for which to request event data from Google Analytics.
            </p>
            <p>
            Specify at least one and up to four date ranges for which event data will be requested.
            The custom name given to a date range replaces the date range name which is auto-generated by
            Google Analytics.
            </p>
            """)
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add date range") // TODO add validation constraints min = 1, max = MAX_NUM_DATE_RANGES
    @Layout(DimensionsSection.class)
    GADateRange[] m_dateRanges = new GADateRange[]{GADateRange.lastWeek()};

    @Widget(title = "Include date range name column", description = """
            <p>
            Include a column containing date range names. If a date range is not given a name
            explicitly, Google Analytics will auto-generate a unique name for each date range.
            </p>
            """, advanced = true)
    @Layout(DimensionsSection.class)
    boolean m_includeDateRangeNameColumn = true;

    @Section(title = "Dimension filter")
    @After(DimensionsSection.class)
    interface DimensionFiltersSection {
    }

    @Layout(DimensionFiltersSection.class)
    @Migrate(loadDefaultIfAbsent = true) // optional since unavailable in 4.7
    GADimensionFilterExpression m_gaDimensionFilter = new GADimensionFilterExpression();

    @Section(title = "Output")
    @Advanced
    @After(DimensionsSection.class)
    interface OutputSection {
    }

    static final class CurrencyCodePatternValidation extends PatternValidation {
        @Override
        protected String getPattern() {
            return "$|[a-zA-Z]{3}";
        }

        @Override
        public String getErrorMessage() {
            return "The string must be empty or in the three letter ISO-4217 format.";
        }
    }

    @Widget(title = "Currency code", description = """
            <p>
            Specify the currency code to use for currency returning metrics, to be stated in three letter
            ISO-4217 format. If it is left empty, the report uses the property&apos;s default currency.
            </p>
            """, advanced = true)
    @Layout(OutputSection.class)
    @TextInputWidget(patternValidation = CurrencyCodePatternValidation.class)
    String m_currencyCode;

    @Widget(title = "Keep empty rows", description = """
            If enabled, rows will also be returned if all their metrics are equal to 0.
            Otherwise they are omitted from the result.
            """, advanced = true)
    @Layout(OutputSection.class)
    boolean m_keepEmptyRows;

    @Widget(title = "Output response metadata as flow variables", description = """
            <p>
            If enabled, outputs response metadata from the Google Analytics API as flow variables.
            In particular, the following fields are returned: <tt>dataLossFromOtherRow</tt>,
            <tt>currencyCode</tt>, <tt>timeZone</tt>, <tt>emptyReason</tt>, <tt>subjectToThresholding</tt>.
            The format of the flow variable name follows the pattern <tt>analytics.response.$FIELD</tt>,
            where <tt>$FIELD</tt> specifies the field name.
            </p>
            <p>
            More information can be obtained from the
            """ + "<a href=\"" + ExternalLinks.API_RESPONSE_METADATA + "\">API documentation</a>.</p>", advanced = true)
    @Layout(OutputSection.class)
    boolean m_returnResponseMetadata;

    @Widget(title = "Output property quotas as flow variables",
        description = """
                <p>
                If enabled, outputs the Google Analytics property quotas as flow variables.
                Each quota will indicate the "consumed" and "remaining" tokens after the last API request made by the node.
                The format of the flow variable name follows the pattern
                <tt>analytics.quota.$QUOTA.$TYPE</tt>, where <tt>$QUOTA</tt> specifies the quota and <tt>$TYPE</tt>
                is either "consumed" or "remaining".
                Available quotas can be seen in the """
            + " <a href=\"" + ExternalLinks.API_QUOTAS + "\">API documentation</a>.</p>"
            + """
                    <p><b>Note:</b> Retrieving a lot of data (many rows, many columns, or long date ranges) or specifying complex
                    filter criteria may be responsible for consumption of many tokens per node execution.</p>
                    """,
        advanced = true)
    @Layout(OutputSection.class)
    boolean m_returnPropertyQuota;

    /**
     * Validates the current state of the settings instance. Intended to be called from the model.
     */
    void validate() throws InvalidSettingsException {
        // validate metrics
        CheckUtils.checkSetting(ArrayUtils.isNotEmpty(m_gaMetrics),
            "The Google Analytics query needs at least one metric.");
        CheckUtils.checkSetting(m_gaMetrics.length <= MAX_NUM_METRICS, """
                   There are too many metrics specified.

                   You can only query a maximum of %d metrics at once.
                   Use another Google Analytics Query node and combine their results, for example using a Joiner node.
                """, MAX_NUM_METRICS);
        for (final var metric : m_gaMetrics) {
            metric.validate();
        }

        // validate dimensions
        CheckUtils.checkSetting(m_gaDimensions == null || m_gaDimensions.length <= MAX_NUM_DIMENSIONS, """
                   There are too many dimensions specified.

                   Google Analytics supports only up to %d dimensions at once.
                """, MAX_NUM_DIMENSIONS);
        for (final var dim : m_gaDimensions) {
            dim.validate();
        }

        // validate date ranges
        CheckUtils.checkSetting(ArrayUtils.isNotEmpty(m_dateRanges),
            "The Google Analytics query is missing a date range.");
        CheckUtils.checkSetting(m_dateRanges.length <= MAX_NUM_DATE_RANGES, """
                   There are too many date ranges specified.

                   Google Analytics supports only up to %d date ranges at once.
                   Use another Google Analytics Query node and combine there results, for example using a Concatenate
                   node.
                """, MAX_NUM_DATE_RANGES);
        final var names = new HashSet<String>(4);
        for (final var range : m_dateRanges) {
            // validate individually
            range.validate();

            // check for ambiguous names
            final var name = range.m_rangeName;
            // duplicated empty names are ok, since the Analytics API will generate a unique range name for each of them
            if (name != null && StringUtils.isNotEmpty(name)) {
                if (names.contains(name)) {
                    throw new InvalidSettingsException("Ambiguous date range name \"%s\".".formatted(name));
                }
                names.add(name);
            }
        }

        // validate dimension filter
        if (m_gaDimensionFilter != null) {
            m_gaDimensionFilter.validate();
        }
    }

}
