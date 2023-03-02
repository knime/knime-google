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

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.google.api.analytics.ga4.port.GAConnection;
import org.knime.google.api.analytics.ga4.port.GAConnectionPortObject;
import org.knime.google.api.analytics.ga4.port.GAConnectionPortObjectSpec;

import com.google.api.services.analyticsdata.v1beta.model.DateRange;
import com.google.api.services.analyticsdata.v1beta.model.Dimension;
import com.google.api.services.analyticsdata.v1beta.model.DimensionHeader;
import com.google.api.services.analyticsdata.v1beta.model.DimensionValue;
import com.google.api.services.analyticsdata.v1beta.model.Metric;
import com.google.api.services.analyticsdata.v1beta.model.MetricHeader;
import com.google.api.services.analyticsdata.v1beta.model.PropertyQuota;
import com.google.api.services.analyticsdata.v1beta.model.QuotaStatus;
import com.google.api.services.analyticsdata.v1beta.model.ResponseMetaData;
import com.google.api.services.analyticsdata.v1beta.model.Row;
import com.google.api.services.analyticsdata.v1beta.model.RunReportRequest;
import com.google.api.services.analyticsdata.v1beta.model.RunReportResponse;

/**
 * Node model for the Google Analytics 4 Query node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class GAQueryNodeModel extends NodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GAQueryNodeModel.class);

    private static final int GA_CONNECTION_PORT = 0;

    /**
     * By default Google Analytics Data API v1 will return 10000 rows at a time.
     * But we can also set the limit ourselves and request more data at once, 100000 being the hard limit.
     */
    private static final int API_MAX_PAGESIZE = 100000;

    /** "(not set)" is Google's version of "missing value". */
    private static final String MISSING = "(not set)";
    private static final DataCell MISSING_CELL = new MissingCell(MISSING);

    /** Name of the date range column in the API response as defined by the Google Analytics Data API. Don't change! */
    private static final String DATE_RANGE_COLUMN_NAME = "dateRange";

    /** Map containing missing cell in case of unknown date range name from API response. */
    private final Map<String, MissingCell> m_missingDateRanges = new HashMap<>();

    private GAQueryNodeSettings m_settings = new GAQueryNodeSettings();

    /**
     * Constructor.
     */
    protected GAQueryNodeModel() {
        super(new PortType[] {GAConnectionPortObject.TYPE}, new PortType[] { BufferedDataTable.TYPE });
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // make sure we have a Google Analytics connection
        if (getGoogleAnalyticsConnectionPortObjectSpec(inSpecs).isEmpty()) {
            throw new InvalidSettingsException("Google Analytics connection is missing.");
        }
        // cannot really know the spec for sure before we make the first request
        return new DataTableSpec[] { null };
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no-op
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no-op
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsForModel(settings);
    }

    @Override
    protected void reset() {
        // not needed
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final var spec = ((GAConnectionPortObject)inObjects[GA_CONNECTION_PORT]);
        final GAConnection conn = spec.getConnection();
        final var prop = spec.getProperty();

        final var limit = API_MAX_PAGESIZE;
        final var req = configureRequest(m_settings, limit);

        exec.setProgress(0, "Running (initial) report request.");
        final RunReportResponse response = conn.runReport(prop, req);

        // this number counts matches without factoring in the date ranges
        // the actual number of returned rows will be this number multiplied by date ranges
        final Integer totalRows = response.getRowCount();
        if (totalRows == null) {
            LOGGER.debug("No rows received.");
        } else {
            LOGGER.debug(String.format("Total rows matched by query: %d", totalRows));
        }

        if (m_settings.m_returnPropertyQuota.getBooleanValue()) {
            pushFlowVariablesPropertyQuota(response.getPropertyQuota());
        }
        if (m_settings.m_returnResponseMetadata.getBooleanValue()) {
            pushFlowVariablesResponseMetadata(response.getMetadata());
        }

        // we want the following order but make sure our date column names don't clash with anything the user
        // writes:
        // [dimensions][dates][metrics]

        final var dimensions = new ArrayList<DataColumnSpec>();
        // we also need to identify which dimension represents the date range
        final int dateRangeColIdx = collectDimensionColumns(response.getDimensionHeaders(), dimensions::add);

        final var metrics = new ArrayList<DataColumnSpec>();
        // we get the specific metric types as well
        final List<MetricTypeAdapter> metricTypes = collectMetricColumns(response.getMetricHeaders(), metrics::add);

        // disambiguate names of date range columns we choose against any potential user
        // chosen metric/dimension names
        final var dates = new ArrayList<DataColumnSpec>();
        final var uniq = new UniqueNameGenerator(
            Stream.concat(dimensions.stream().map(DataColumnSpec::getName), metrics.stream()
                .map(DataColumnSpec::getName)).collect(Collectors.toSet()));
        collectDateRangeColumns(m_settings.m_includeDateRangeNameColumn.getBooleanValue(), uniq::newColumn, dates::add);

        final var specCreator = new DataTableSpecCreator();
        Stream.of(dimensions.stream(), dates.stream(), metrics.stream()).flatMap(s -> s)
            .forEach(specCreator::addColumns);
        final var outSpec = specCreator.createSpec();

        final var out = exec.createDataContainer(outSpec);
        final var rowIds = new AtomicLong(0);
        final Supplier<RowKey> rowKeys = () -> RowKey.createRowKey(rowIds.getAndIncrement());

        // If a request's offset was outside the total number of rows matched by the query,
        // the list of rows will be null...
        List<Row> rows = response.getRows();
        if (totalRows == null || rows == null) {
            out.close();
            return new BufferedDataTable[] { out.getTable() };
        }
        final Consumer<List<DataCell>> cells = c -> out.addRowToTable(new DefaultRow(rowKeys.get(), c));
        addResponseRows(m_settings, metricTypes, dateRangeColIdx, rows, cells, exec);
        int remaining = totalRows - limit;
        // Pagination of further query results from API
        final var requests = (int) Math.ceil(1.0 * totalRows / limit);
        for (var i = 0; remaining > 0; i++) {
            final var progress = (100.0 / requests * (i + 1)) / 100.0;
            exec.setProgress(progress, String.format("Paginating: running report page request #%d of %d.", i, requests));
            exec.checkCanceled();
            final var offset = (i + 1) * limit;
            final var resp = conn.runReport(prop, req.setOffset(Long.valueOf(offset)));
            rows = resp.getRows();
            if (rows == null) {
                break;
            }
            if (m_settings.m_returnPropertyQuota.getBooleanValue()) {
                // update quota flow variables based on new response
                pushFlowVariablesPropertyQuota(resp.getPropertyQuota());
            }
            if (m_settings.m_returnResponseMetadata.getBooleanValue()) {
                pushFlowVariablesResponseMetadata(response.getMetadata());
            }
            addResponseRows(m_settings, metricTypes, dateRangeColIdx, rows, cells, exec);
            remaining -= limit;
        }
        out.close();
        return new BufferedDataTable[] { out.getTable() };
    }

    /*
     * Request methods
     */

    /**
     * Configures a RunReport using the Google Analytics Data API request with the current node settings.
     * @param settings current node settings
     * @param limit limit parameter used in Data API
     * @return a configured run report request
     */
    private static RunReportRequest configureRequest(final GAQueryNodeSettings settings, final int limit) {
        return new RunReportRequest()
            .setDateRanges(settings.m_dateRanges.getRanges().stream().map(
                        r -> new DateRange()
                            .setStartDate(r.m_fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .setEndDate(r.m_toDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .setName(r.m_rangeName))
                .collect(Collectors.toList()))
            .setDimensions(settings.m_gaDimensions.getValue().stream()
                .map(d -> new Dimension().setName(d))
                .collect(Collectors.toList()))
            .setMetrics(settings.m_gaMetrics.getValue().stream()
                .map(m -> new Metric().setName(m))
                .collect(Collectors.toList()))
            .setLimit(Long.valueOf(limit))
            .setCurrencyCode(settings.m_currencyCode.getStringValue())
            .setKeepEmptyRows(settings.m_keepEmptyRows.getBooleanValue())
            .setReturnPropertyQuota(settings.m_returnPropertyQuota.getBooleanValue());
    }

    /*
     * Response methods
     */

    /**
     * Add the dimensions to the spec.
     *
     * In addition, we retrieve the index of the date range column returned from the API, which considers the date range
     * a special dimension.
     * If only one date range was requested, it is not part of the API response.
     *
     * @param dimensionHeaders headers returned from the API
     * @param columnSpecCollector collector for created column specs
     * @return the offset of the date range column in the given list, or {@code -1} if the date range column is not
     *   part of the headers
     */
    private static int collectDimensionColumns(final List<DimensionHeader> dimensionHeaders,
            final Consumer<DataColumnSpec> columnSpecCollector) {
        // even though the javadoc says "getDimensionHeaders" could be null, it effectively
        // cannot since a date range is a special dimension and we always need to specify at least one date range
        if (dimensionHeaders == null) {
            return -1;
        }
        var dateRangeColIdx = -1;
        var i = -1;
        for (final var d : dimensionHeaders) {
                i++;
                final var colName = d.getName();
                if (DATE_RANGE_COLUMN_NAME .equals(colName)) {
                    dateRangeColIdx = i;
                } else {
                    columnSpecCollector.accept(new DataColumnSpecCreator(colName, StringCell.TYPE).createSpec());
                }
        }
        return dateRangeColIdx;
    }

    /**
     * Collects the date range columns, disambiguating the default names using the given disambiguator.
     *
     * @param includeDateRangeNameColumn whether to include the name of the date range as a column or not
     * @param nameDisambiguator disambiguator to retrieve uniquely named columns
     * @param columnSpecCollector collector for date range column specs
     */
    private static void collectDateRangeColumns(final boolean includeDateRangeNameColumn,
            final BiFunction<String, DataType, DataColumnSpec> nameDisambiguator,
            final Consumer<DataColumnSpec> columnSpecCollector) {
        if (includeDateRangeNameColumn) {
            columnSpecCollector.accept(nameDisambiguator.apply(DATE_RANGE_COLUMN_NAME, StringCell.TYPE));
        }
        columnSpecCollector.accept(nameDisambiguator.apply("fromDate", LocalDateCellFactory.TYPE));
        columnSpecCollector.accept(nameDisambiguator.apply("toDate", LocalDateCellFactory.TYPE));
    }

    /**
     * Collects the metric columns given the header and returns the metric data types.
     *
     * @param metricHeaders metric information retrieved from the API
     * @param collector for metric column specs
     * @return list of metric types extracted from the headers
     */
    private static List<MetricTypeAdapter> collectMetricColumns(final List<MetricHeader> metricHeaders,
            final Consumer<DataColumnSpec> columnSpecCollector) {
        if (metricHeaders == null) {
            return Collections.emptyList();
        }
        return metricHeaders.stream()
            .map(m -> {
                final var metricType = m.getType();
                final var res = MetricTypeAdapter.lookup(metricType);
                final MetricTypeAdapter adapter = res.orElseGet(() -> {
                    LOGGER.warnWithFormat("Unknown metric type \"%s\". Falling back to string.", metricType);
                    return new MetricTypeAdapter(StringCell.TYPE, StringCell::new);
                });
                columnSpecCollector.accept(new DataColumnSpecCreator(m.getName(), adapter.getDataType()).createSpec());
                return adapter;
            }).collect(Collectors.toList());
    }

    /**
     * Adds the response rows to the given consumer as data cells.
     * Dimension cells are of type {@link StringCell.TYPE}, data range columns of type {@link LocalDateCell.TYPE}, and
     * metric cells according to the given metric type adapters.
     *
     * @param settings node settings used for mapping returned date range names to date range values
     * @param metricTypes types of metrics contained in the rows
     * @param dateRangeColIdx offset in list of dimensions where date range name column can be found, {@code -1} if it
     *  does not exist
     * @param rows response rows to process
     * @param cellsConsumer consumer for data cells created from response rows
     * @param exec context to check for cancellation
     * @throws CanceledExecutionException execution will be canceled
     */
    private void addResponseRows(final GAQueryNodeSettings settings, final List<MetricTypeAdapter> metricTypes,
            final int dateRangeColIdx, final List<Row> rows,
            final Consumer<List<DataCell>> cellsConsumer, final ExecutionContext exec)
                    throws CanceledExecutionException {
        if (rows == null) {
            return;
        }
        final var cells = new ArrayList<DataCell>();
        for (final var r : rows) {
            exec.checkCanceled();
            // first all non-date-range dimensions
            final var dateRangeName = addDimensions(r.getDimensionValues(), dateRangeColIdx, cells::add);
            // then the date range column(s)
            addDateRange(dateRangeName, settings.m_dateRanges.getRanges(),
                settings.m_includeDateRangeNameColumn.getBooleanValue(), cells::add);
            // then all metrics
            final var metricValues = r.getMetricValues();
            if (metricValues != null) {
                zipMap(metricValues.iterator(), metricTypes.iterator(), (v, t) -> t.parse(v.getValue()))
                    .forEachRemaining(cells::add);
            }
            cellsConsumer.accept(cells);
            cells.clear();
        }
    }

    /**
     * Add date range as cells of type {@link LocalDateCell.TYPE}.
     * The date range is looked up by name in case multiple ranges are given.
     * @param name name of date range to add
     * @param ranges date ranges to look up from or single date range to use
     * @param includeDateRangeNameColumn {@code true} if the date range name should be added,
     *  or {@code false} if it should not be added
     * @param cellCollector collector for cells created from date range
     */
    private void addDateRange(final String name, final List<GADateRange> ranges,
            final boolean includeDateRangeNameColumn, final Consumer<DataCell> cellCollector) {
        CheckUtils.checkArgument(!ranges.isEmpty(), "Date ranges are missing.");
        if (ranges.size() == 1) {
            // only one date range specified, so we probably did not get a date range name from the API
            final var range = ranges.get(0);
            if (includeDateRangeNameColumn) {
                // same format as API would return
                cellCollector.accept(new StringCell(range.m_rangeName != null ? range.m_rangeName : "date_range_0"));
            }
            cellCollector.accept(LocalDateCellFactory.create(range.m_fromDate));
            cellCollector.accept(LocalDateCellFactory.create(range.m_toDate));
            return;
        }
        CheckUtils.checkArgumentNotNull(name, "Date range name value must be present if multiple date "
            + "ranges are specified.");
        // multiple date ranges to choose from
        if (includeDateRangeNameColumn) {
            cellCollector.accept(new StringCell(name));
        }
        GADateRange.lookupDateRange(ranges, name).ifPresentOrElse(
            range -> {
                cellCollector.accept(LocalDateCellFactory.create(range.m_fromDate));
                cellCollector.accept(LocalDateCellFactory.create(range.m_toDate));
            },
            () -> {
                LOGGER.warnWithFormat("API response contained unknown date range name \"%s\".", name);
                cellCollector.accept(m_missingDateRanges.computeIfAbsent(name, MissingCell::new));
                cellCollector.accept(m_missingDateRanges.computeIfAbsent(name, MissingCell::new));
            }
        );
    }

    /**
     * Add the dimension values to the cells, skipping the date range value at the specified column index, and returning
     * it if it exists.
     *
     * @param dimensions dimension values to add to the cells
     * @param dateRangeColIdx index of the date range column, or {@code -1} if no date range column exists
     * @param cells cells to add dimension value to
     * @return the date range value or {@code null} if no such value exists
     *          (i.e. if {@link dateRangeColIdx} was {@code -1})
     */
    private static String addDimensions(final List<DimensionValue> dimensions, final int dateRangeColIdx,
            final Consumer<DataCell> cells) {
        String dateRangeName = null;
        if (dimensions != null) {
            var d = -1;
            for (final var dv : dimensions) {
                d++;
                final var v = dv.getValue();
                if (MISSING.equals(v)) { // NOSONAR depth is ok
                    // Google's version of missing value
                    cells.accept(MISSING_CELL);
                    continue;
                }

                if (d == dateRangeColIdx) {
                    // we let the caller handle the date range value
                    dateRangeName = v;
                } else {
                    cells.accept(new StringCell(v));
                }
            }
        }
        return dateRangeName;
    }

    private void pushFlowVariablesResponseMetadata(final ResponseMetaData metadata) {
        pushFlowVariableWithDefault("dataLossFromOtherRow", BooleanType.INSTANCE, metadata.getDataLossFromOtherRow(),
            false);
        pushFlowVariableWithDefault("currencyCode", StringType.INSTANCE, metadata.getCurrencyCode(), "");
        pushFlowVariableWithDefault("timeZone", StringType.INSTANCE, metadata.getTimeZone(), "");
        pushFlowVariableWithDefault("emptyReason", StringType.INSTANCE, metadata.getEmptyReason(), "");
        pushFlowVariableWithDefault("subjectToThresholding", BooleanType.INSTANCE, metadata.getSubjectToThresholding(),
            false);
    }

    private <T> void pushFlowVariableWithDefault(final String fieldName, final VariableType<T> type, final T value,
            final T defaultValue) {
        CheckUtils.checkNotNull(defaultValue);
        final T varValue;
        if (value != null) {
            varValue = value;
        } else {
            varValue = defaultValue;
        }
        pushFlowVariable("analytics.meta." + fieldName, type, varValue);
    }

    private void pushFlowVariablesQuota(final String infix, final QuotaStatus status) {
        if (status == null) {
            LOGGER.warn(String.format("Google Analytics Quota Status \"%s\" was not returned from API.", infix));
            return;
        }
        pushFlowVariableInt(String.format("analytics.quota.%s.consumed", infix), status.getConsumed());
        pushFlowVariableInt(String.format("analytics.quota.%s.remaining", infix), status.getRemaining());
    }

    private void pushFlowVariablesPropertyQuota(final PropertyQuota quota) {
        pushFlowVariablesQuota("concurrentRequests", quota.getConcurrentRequests());
        pushFlowVariablesQuota("potentiallyThresholdedRequestsPerHour",
            quota.getPotentiallyThresholdedRequestsPerHour());
        pushFlowVariablesQuota("serverErrorsPerProjectPerHour", quota.getServerErrorsPerProjectPerHour());
        pushFlowVariablesQuota("tokensPerDay", quota.getTokensPerDay());
        pushFlowVariablesQuota("tokensPerHour", quota.getTokensPerHour());
        pushFlowVariablesQuota("tokensPerProjectPerHour", quota.getTokensPerProjectPerHour());
    }

    /**
     * Pair-wise function application on two iterators, where the longer iterator is truncated.
     *
     * @param <T> left type
     * @param <U> right type
     * @param <V> result type
     * @param left left iterator
     * @param right right iterator
     * @param fn function to be applied on each pair
     * @return iterator with results
     */
    private static final <T, U, V> Iterator<V> zipMap(final Iterator<T> left, final Iterator<U> right,
            final BiFunction<T, U, V> fn) {
        return new Iterator<V>() {

            @Override
            public boolean hasNext() {
                return left.hasNext() && right.hasNext();
            }

            @Override
            public V next() {
                return fn.apply(left.next(), right.next());
            }
        };
    }

    private static Optional<GAConnectionPortObjectSpec> getGoogleAnalyticsConnectionPortObjectSpec(
            final PortObjectSpec[] pos) {
        return GAConnectionPortObjectSpec.getGoogleAnalyticsConnectionPortObjectSpec(pos, GA_CONNECTION_PORT);
    }
}
