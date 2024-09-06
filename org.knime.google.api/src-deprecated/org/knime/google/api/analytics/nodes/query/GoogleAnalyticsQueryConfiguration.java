/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 19, 2014 ("Patrick Winter"): created
 */
package org.knime.google.api.analytics.nodes.query;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.google.api.analytics.data.GoogleAnalyticsConnection;

import com.google.api.services.analytics.Analytics.Data.Ga.Get;
import com.google.api.services.analytics.model.Segment;

/**
 * Configuration of the GoogleAnalyticsQuery node.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @deprecated
 */
@Deprecated(since = "5.4")
public class GoogleAnalyticsQueryConfiguration {

    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final String CFG_DIMENSIONS = "dimensions";

    private static final String CFG_METRICS = "metrics";

    private static final String CFG_SEGMENT = "segment";

    private static final String CFG_FILTERS = "filters";

    private static final String CFG_SORT = "sort";

    private static final String CFG_START_DATE = "start-date";

    private static final String CFG_END_DATE = "end-date";

    private static final String CFG_START_INDEX = "start-index";

    private static final String CFG_MAX_RESULTS = "max-results";

    private String[] m_dimensions = new String[0];

    private String[] m_metrics = new String[0];

    private String m_segment = "";

    private String m_filters = "";

    private String m_sort = "";

    private String m_startDate = "";

    private String m_endDate = "";

    private int m_startIndex = 1;

    private int m_maxResults = 1000;

    /**
     * Create GoogleAnalyticsQueryConfiguration.
     */
    public GoogleAnalyticsQueryConfiguration() {
        Calendar calendar = new GregorianCalendar();
        m_endDate = FORMAT.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        m_startDate = FORMAT.format(calendar.getTime());
    }

    /**
     * @return the dimensions
     */
    public String[] getDimensions() {
        return m_dimensions;
    }

    /**
     * @param dimensions the dimensions to set
     * @throws InvalidSettingsException If the dimensions exceed the maximum of 7
     */
    public void setDimensions(final String[] dimensions) throws InvalidSettingsException {
        if (dimensions.length > 7) {
            throw new InvalidSettingsException("A single request is limited to a maximum of 7 dimensions");
        }
        m_dimensions = dimensions;
    }

    /**
     * @return the metrics
     */
    public String[] getMetrics() {
        return m_metrics;
    }

    /**
     * @param metrics the metrics to set
     * @throws InvalidSettingsException If the metrics exceed the maximum of 10 or no metric is given
     */
    public void setMetrics(final String[] metrics) throws InvalidSettingsException {
        if (metrics.length < 1) {
            throw new InvalidSettingsException("At least one metric is required");
        }
        if (metrics.length > 10) {
            throw new InvalidSettingsException("A single request is limited to a maximum of 10 metrics");
        }
        m_metrics = metrics;
    }

    /**
     * @return the segment
     */
    public String getSegment() {
        return m_segment;
    }

    /**
     * @param segment the segment to set
     */
    public void setSegment(final String segment) {
        m_segment = segment;
    }

    /**
     * @return the filters
     */
    public String getFilters() {
        return m_filters;
    }

    /**
     * @param filters the filters to set
     */
    public void setFilters(final String filters) {
        m_filters = filters;
    }

    /**
     * @return the sort
     */
    public String getSort() {
        return m_sort;
    }

    /**
     * @param sort the sort to set
     */
    public void setSort(final String sort) {
        m_sort = sort;
    }

    /**
     * @return the startDate
     */
    public String getStartDate() {
        return m_startDate;
    }

    /**
     * @param startDate the startDate to set
     * @throws InvalidSettingsException If the start date is missing or invalid
     */
    public void setStartDate(final String startDate) throws InvalidSettingsException {
        if (!isDateValid(startDate)) {
            throw new InvalidSettingsException("A valid start date in the format YYYY-MM-DD is required");
        }
        m_startDate = startDate;
    }

    /**
     * @return the endDate
     */
    public String getEndDate() {
        return m_endDate;
    }

    /**
     * @param endDate the endDate to set
     * @throws InvalidSettingsException If the end date is missing or invalid
     */
    public void setEndDate(final String endDate) throws InvalidSettingsException {
        if (!isDateValid(endDate)) {
            throw new InvalidSettingsException("A valid end date in the format YYYY-MM-DD is required");
        }
        m_endDate = endDate;
    }

    /**
     * @return the startIndex
     */
    public int getStartIndex() {
        return m_startIndex;
    }

    /**
     * @param startIndex the startIndex to set
     * @throws InvalidSettingsException If the start index is below 1
     */
    public void setStartIndex(final int startIndex) throws InvalidSettingsException {
        if (startIndex < 1) {
            throw new InvalidSettingsException("The minimum start index is 1");
        }
        m_startIndex = startIndex;
    }

    /**
     * @return the maxResults
     */
    public int getMaxResults() {
        return m_maxResults;
    }

    /**
     * @param maxResults the maxResults to set
     * @throws InvalidSettingsException If the maximum number of results is below 1 or above 10000
     */
    public void setMaxResults(final int maxResults) throws InvalidSettingsException {
        if (maxResults < 1) {
            throw new InvalidSettingsException("The minimum number of results is 1");
        }
        if (maxResults > 10000) {
            throw new InvalidSettingsException("The maximum number of results is 10000");
        }
        m_maxResults = maxResults;
    }

    /**
     * @param settings The settings object to save in
     */
    public void save(final NodeSettingsWO settings) {
        settings.addStringArray(CFG_DIMENSIONS, m_dimensions);
        settings.addStringArray(CFG_METRICS, m_metrics);
        settings.addString(CFG_SEGMENT, m_segment);
        settings.addString(CFG_FILTERS, m_filters);
        settings.addString(CFG_SORT, m_sort);
        settings.addString(CFG_START_DATE, m_startDate);
        settings.addString(CFG_END_DATE, m_endDate);
        settings.addInt(CFG_START_INDEX, m_startIndex);
        settings.addInt(CFG_MAX_RESULTS, m_maxResults);
    }

    /**
     * @param settings The settings object to load from
     * @throws InvalidSettingsException If the settings are invalid
     */
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dimensions = settings.getStringArray(CFG_DIMENSIONS);
        m_metrics = settings.getStringArray(CFG_METRICS);
        m_segment = settings.getString(CFG_SEGMENT);
        m_filters = settings.getString(CFG_FILTERS);
        m_sort = settings.getString(CFG_SORT);
        m_startDate = settings.getString(CFG_START_DATE);
        m_endDate = settings.getString(CFG_END_DATE);
        m_startIndex = settings.getInt(CFG_START_INDEX);
        m_maxResults = settings.getInt(CFG_MAX_RESULTS);
    }

    /**
     * @param settings The settings object to load from
     */
    public void loadInDialog(final NodeSettingsRO settings) {
        Calendar calendar = new GregorianCalendar();
        m_dimensions = settings.getStringArray(CFG_DIMENSIONS, new String[0]);
        m_metrics = settings.getStringArray(CFG_METRICS, new String[0]);
        m_segment = settings.getString(CFG_SEGMENT, "");
        m_filters = settings.getString(CFG_FILTERS, "");
        m_sort = settings.getString(CFG_SORT, "");
        m_endDate = settings.getString(CFG_END_DATE, "");
        if (!isDateValid(m_endDate)) {
            m_endDate = FORMAT.format(calendar.getTime());
        }
        m_startDate = settings.getString(CFG_START_DATE, "");
        if (!isDateValid(m_startDate)) {
            calendar.add(Calendar.DAY_OF_MONTH, -7);
            m_startDate = FORMAT.format(calendar.getTime());
        }
        m_startIndex = settings.getInt(CFG_START_INDEX, 1);
        m_maxResults = settings.getInt(CFG_MAX_RESULTS, 1000);
    }

    /**
     * @param connection The connection to use
     * @return A get request for the currently configured query
     * @throws IOException If an IO error occurs
     * @throws NoSuchCredentialException
     */
    public Get createGetRequest(final GoogleAnalyticsConnection connection) throws IOException, NoSuchCredentialException {
        String[] metrics = new String[m_metrics.length];
        for (int i = 0; i < metrics.length; i++) {
            metrics[i] = prependPrefix(m_metrics[i]);
        }
        // Create request with metrics, start and end date
        Get get =
                connection.getAnalytics().data().ga()
                        .get("ga:" + connection.getProfileId(), m_startDate, m_endDate, StringUtils.join(metrics, ","));
        // Add additional parameters if available
        if (m_dimensions.length > 0) {
            String[] dimensions = new String[m_dimensions.length];
            for (int i = 0; i < dimensions.length; i++) {
                dimensions[i] = prependPrefix(m_dimensions[i]);
            }
            get.setDimensions(StringUtils.join(dimensions, ","));
        }
        if (!m_segment.isEmpty()) {
            boolean dynamic = true;
            for (Segment item : connection.getAnalytics().management().segments().list().execute().getItems()) {
                if (m_segment.equals(item.getId())) {
                    dynamic = false;
                    break;
                }
            }
            get.setSegment(getSegmentWithPrefix(dynamic));
        }
        if (!m_filters.isEmpty()) {
            get.setFilters(prependPrefixToFilters(m_filters));
        }
        if (!m_sort.isEmpty()) {
            String[] noPrefix = m_sort.split(",");
            String[] withPrefix = new String[noPrefix.length];
            for (int i = 0; i < withPrefix.length; i++) {
                withPrefix[i] = prependPrefix(noPrefix[i]);
            }
            get.setSort(StringUtils.join(withPrefix, ","));
        }
        get.setStartIndex(m_startIndex);
        get.setMaxResults(m_maxResults);
        return get;
    }

    /**
     * @param dateToValidate The date to check
     * @return true if the date is in a valid format, false otherwise
     */
    private boolean isDateValid(final String dateToValidate) {
        if (dateToValidate == null) {
            // Date is missing
            return false;
        }
        // Strict parsing
        FORMAT.setLenient(false);
        try {
            FORMAT.parse(dateToValidate);
        } catch (ParseException e) {
            // Date could not be parsed
            return false;
        }
        // Everything ok
        return true;
    }

    /**
     * Returns the segment with prefix.
     *
     * The prefix is 'gaid::' for pre-defined and built-in segments or 'sessions::condition::' for dynamic segments.
     * @param dynamic whether the segment is dynamic or built-in/pre-defined
     *
     * @return The segment with prefix
     */
    private String getSegmentWithPrefix(final boolean dynamic) {
        if (!dynamic) {
            return "gaid::" + m_segment;
        }
        if (m_segment.contains("::")) {
            return m_segment;
        }
        return "sessions::condition::" + prependPrefixToFilters(m_segment);
    }

    /**
     * Will prepend the prefix 'ga:' to the string while keeping the '-' in front (for sort strings)
     *
     * @param string The string to prepend to
     * @return The string with the prepended prefix 'ga:'
     */
    private String prependPrefix(final String string) {
        if (string.startsWith("-")) {
            return string.replaceFirst("-", "-ga:");
        } else {
            return "ga:" + string;
        }
    }

    /**
     * Will prepend the prefix 'ga:' to all column names in the given filters string while respecting escaped characters.
     *
     * @param string The string to prepend to
     * @return The string with the prepended prefix 'ga:' to the columns
     */
    private String prependPrefixToFilters(final String string) {
        StringBuilder sb = new StringBuilder();
        sb.append("ga:");
        for (int i = 0; i < string.length(); i++) {
            boolean hasNext = i + 1 < string.length();
            char c = string.charAt(i);
            if (c == '\\') {
                sb.append(c);
                if (hasNext) {
                    // next character is escaped
                    sb.append(string.charAt(++i));
                }
            } else if (c == ',' || c == ';') {
                // new column starts after c
                sb.append(c + "ga:");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
