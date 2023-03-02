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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.google.api.analytics.ga4.node.util.Persistor;
/**
 * Settings for a Google Analytics 4 Data API
 * <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/rest/v1beta/DateRange">
 * {@code DateRange}</a>.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class GADateRange {

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    LocalDate m_fromDate;

    LocalDate m_toDate;

    String m_rangeName;

    /**
     * Create a new date range. If the date range should be named, provide a non-null name. The prefixes "RESERVED_" and
     * "date_range_" are reserved and cannot be used. Leave the name {@code null} to get the name auto-generated by the
     * API.
     *
     * @param fromDate start of date range
     * @param toDate end of date range
     * @param name name or {@code null} to get an auto-generated name
     */
    GADateRange(final LocalDate fromDate, final LocalDate toDate, final String name) {
        m_fromDate = Objects.requireNonNull(fromDate);
        m_toDate = Objects.requireNonNull(toDate);
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
        CheckUtils.checkSetting(!m_toDate.isBefore(m_fromDate),
            "End of date range must not be before start of date range.");
        // null indicates auto-generated name
        CheckUtils.checkSetting(m_rangeName == null || StringUtils.isNotBlank(m_rangeName),
                "Range name must not be blank. Leave the field empty to get an auto-generated range name.");
        if (m_rangeName != null) {
            for (final var forbidden : new String[]{ "date_range_", "RESERVED_" }) {
                CheckUtils.checkSetting(!m_rangeName.startsWith(forbidden),
                    String.format("Custom date range name must not start with \"%s\".", forbidden));
            }
        }
    }

    /**
     * Lookup the date range by non-null name.
     *
     * @param dateRanges ranges to look up from by name
     * @param name name to look up date ranges with
     * @return date range identified by name
     */
    static Optional<GADateRange> lookupDateRange(final List<GADateRange> dateRanges, final String name) {
        CheckUtils.checkArgument(!dateRanges.isEmpty(), "No date ranges specified.");
        CheckUtils.checkArgumentNotNull(name, "Date range name must not be null.");
        if (name.startsWith("RESERVED_")) {
            // this should never make its way into a valid date range, and as such should never be looked up
            throw new IllegalArgumentException(String.format("Unexpected date range dimension name \"%s\".", name));
        }
        // check auto-generated range name based on date range index
        if (name.startsWith("date_range_")) {
            final var idx = Integer.parseUnsignedInt(name, "date_range_".length(), name.length(), 10);
            return Optional.of(dateRanges.get(idx));
        }
        // resort to linear scan over the small number of date ranges one can specify
        for (final var d : dateRanges) {
            if (name.equals(d.m_rangeName)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    public static final class GADateRangePersistor implements Persistor<GADateRange> {

        @Override
        public String getModelTypeID() {
            return "SMID_GA_DATERANGE";
        }

        @Override
        public GADateRange load(final ConfigRO cfg) throws InvalidSettingsException {
            final var from = LocalDate.parse(cfg.getString("fromDate"), DATE_FMT);
            final var to = LocalDate.parse(cfg.getString("toDate"), DATE_FMT);
            final var rangeName = cfg.getString("rangeName");
            return new GADateRange(from, to, rangeName);
        }

        @Override
        public void save(final GADateRange range, final ConfigWO cfg) {
            cfg.addString("fromDate", range.m_fromDate.format(DATE_FMT));
            cfg.addString("toDate", range.m_toDate.format(DATE_FMT));
            cfg.addString("rangeName", range.m_rangeName);
        }

    }
}