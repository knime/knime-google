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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation.PatternValidation;
/**
 * Settings for a Google Analytics 4 Data API
 * <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/rest/v1beta/DateRange">
 * {@code DateRange}</a>.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
// How to support "NdaysAgo", "yesterday", "today" (supported by GA4 API)?
@SuppressWarnings("restriction") // webui*
final class GADateRange implements DefaultNodeSettings {

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @HorizontalLayout
    interface DateRangeLayout {}

    @Widget(title = "From date", description = """
            The inclusive start date for the query, not after the end date.
            """)
    @Layout(DateRangeLayout.class)
    LocalDate m_fromDate;

    @Widget(title = "To date",
        description = "The inclusive end date for the query, not before the start date.")
    @Layout(DateRangeLayout.class)
    LocalDate m_toDate;

    static final class RangeNamePatternValidation extends PatternValidation {
        @Override
        protected String getPattern() {
            return "(?!date_range_|RESERVED_).*";
        }

        @Override
        public String getErrorMessage() {
            return "The name must not start with \"date_range_\" or \"RESERVED_\"";
        }
    }

    @Widget(title = "Name (optional)",
            description = """
                    A custom name for the date range to which the dimension <tt>dateRange</tt> is valued.
                    If no name is given, Google Analytics automatically names each date range based on their index:
                    <tt>date_range_0</tt>, <tt>date_range_1</tt>, etc.

                    <b>Note:</b> The custom name must not start with "date_range_" or "RESERVED_".
                    """)
    @Layout(DateRangeLayout.class)
    @TextInputWidget(patternValidation = RangeNamePatternValidation.class)
    String m_rangeName;

    private GADateRange() {
        // ser-/deserialization
    }

    private GADateRange(final LocalDate fromDate, final LocalDate toDate, final String name) {
        m_fromDate = fromDate;
        m_toDate = toDate;
        m_rangeName = name;
    }

    /**
     * Creates an absolute date range representing "last week" (as of method call time).
     * @return absolute date range representing "last week"
     */
    static GADateRange lastWeek() {
        return new GADateRange(LocalDate.now().minus(7, ChronoUnit.DAYS), LocalDate.now(), null);
    }

    /**
     * Validate the date range.
     *
     * @throws InvalidSettingsException if the date range is invalid
     */
    void validate() throws InvalidSettingsException {
        CheckUtils.checkSetting(m_fromDate != null, "\"From date\" must not be empty.");
        CheckUtils.checkSetting(m_toDate != null, "\"To date\" must not be empty.");
        CheckUtils.checkSetting(!m_toDate.isBefore(m_fromDate),
                "End of date range must not be before start of range.");

        if (m_rangeName == null) {
            // will request auto-generated name from API
            return;
        }

        for (final var forbidden : new String[]{ "date_range_", "RESERVED_" }) {
            CheckUtils.checkSetting(!m_rangeName.startsWith(forbidden),
                "Custom date range name must not start with \"%s\".".formatted(forbidden));
        }
    }

    /**
     * Lookup the date range by non-null name.
     *
     * @param dateRanges ranges to look up from by name
     * @param name name to look up date ranges with
     * @return date range identified by name
     */
    static Optional<GADateRange> lookupDateRange(final GADateRange[] dateRanges, final String name) {
        CheckUtils.checkArgument(dateRanges.length > 0, "No date ranges specified.");
        CheckUtils.checkArgumentNotNull(name, "Date range name must not be null.");
        if (name.startsWith("RESERVED_")) {
            // this should never make its way into a valid date range, and as such should never be looked up
            throw new IllegalArgumentException("Unexpected date range dimension name \"%s\".".formatted(name));
        }
        // check auto-generated range name based on date range index
        if (name.startsWith("date_range_")) {
            final var idx = Integer.parseUnsignedInt(name, "date_range_".length(), name.length(), 10);
            return Optional.of(dateRanges[idx]);
        }
        // resort to linear scan over the small number of date ranges one can specify
        for (final var d : dateRanges) {
            if (name.equals(d.m_rangeName)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }
}