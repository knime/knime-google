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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.google.api.analytics.ga4.node.query.GAOrderBy.SortOrder;
import org.knime.google.api.analytics.ga4.port.GAConnection;
import org.knime.google.api.analytics.ga4.port.GAConnectionPortObject;

import com.google.api.services.analyticsdata.v1beta.model.DateRange;
import com.google.api.services.analyticsdata.v1beta.model.Dimension;
import com.google.api.services.analyticsdata.v1beta.model.DimensionHeader;
import com.google.api.services.analyticsdata.v1beta.model.DimensionOrderBy;
import com.google.api.services.analyticsdata.v1beta.model.DimensionValue;
import com.google.api.services.analyticsdata.v1beta.model.Filter;
import com.google.api.services.analyticsdata.v1beta.model.FilterExpression;
import com.google.api.services.analyticsdata.v1beta.model.FilterExpressionList;
import com.google.api.services.analyticsdata.v1beta.model.InListFilter;
import com.google.api.services.analyticsdata.v1beta.model.Metric;
import com.google.api.services.analyticsdata.v1beta.model.MetricHeader;
import com.google.api.services.analyticsdata.v1beta.model.MetricOrderBy;
import com.google.api.services.analyticsdata.v1beta.model.OrderBy;
import com.google.api.services.analyticsdata.v1beta.model.PropertyQuota;
import com.google.api.services.analyticsdata.v1beta.model.QuotaStatus;
import com.google.api.services.analyticsdata.v1beta.model.ResponseMetaData;
import com.google.api.services.analyticsdata.v1beta.model.Row;
import com.google.api.services.analyticsdata.v1beta.model.RunReportRequest;
import com.google.api.services.analyticsdata.v1beta.model.RunReportResponse;
import com.google.api.services.analyticsdata.v1beta.model.StringFilter;

/**
 * Node model for the Google Analytics 4 Query node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui classes
final class GAQueryNodeModel extends WebUINodeModel<GAQueryNodeSettings> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GAQueryNodeModel.class);

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

    /**
     * Constructor.
     * @param configuration configuration for the node
     */
    protected GAQueryNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, GAQueryNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final GAQueryNodeSettings settings)
        throws InvalidSettingsException {
        // cannot really know the spec for sure before we make the first request
        return new DataTableSpec[] { null };
    }

    @Override
    protected void validateSettings(final GAQueryNodeSettings settings) throws InvalidSettingsException {
        settings.validate();
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] inObjects, final ExecutionContext exec,
            final GAQueryNodeSettings settings) throws Exception {
        final var spec = ((GAConnectionPortObject)inObjects[0]);
        final GAConnection conn = spec.getConnection();
        final var prop = spec.getProperty();

        final var limit = API_MAX_PAGESIZE;
        final var req = configureRequest(settings, limit);

        exec.setProgress(0, "Running (initial) report request.");
        final RunReportResponse response = conn.runReport(prop, req);

        // this number counts matches without factoring in the date ranges
        // the actual number of returned rows will be this number multiplied by date ranges
        final Integer totalRows = response.getRowCount();
        if (totalRows == null) {
            LOGGER.debug("No rows received.");
        } else {
            LOGGER.debug("Total rows matched by query: %d".formatted(totalRows));
        }

        if (settings.m_returnPropertyQuota) {
            pushFlowVariablesPropertyQuota(response.getPropertyQuota());
        }
        if (settings.m_returnResponseMetadata) {
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
        collectDateRangeColumns(settings.m_includeDateRangeNameColumn, uniq::newColumn, dates::add);

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
        addResponseRows(settings, metricTypes, dateRangeColIdx, rows, cells, exec);
        int remaining = totalRows - limit;
        // Pagination of further query results from API
        final var requests = (int) Math.ceil(1.0 * totalRows / limit);
        for (var i = 0; remaining > 0; i++) {
            final var progress = (100.0 / requests * (i + 1)) / 100.0;
            exec.setProgress(progress, "Paginating: running report page request #%d of %d.".formatted(i, requests));
            exec.checkCanceled();
            final var offset = (i + 1) * limit;
            final var resp = conn.runReport(prop, req.setOffset(Long.valueOf(offset)));
            rows = resp.getRows();
            if (rows == null) {
                break;
            }
            if (settings.m_returnPropertyQuota) {
                // update quota flow variables based on new response
                pushFlowVariablesPropertyQuota(resp.getPropertyQuota());
            }
            if (settings.m_returnResponseMetadata) {
                pushFlowVariablesResponseMetadata(response.getMetadata());
            }
            addResponseRows(settings, metricTypes, dateRangeColIdx, rows, cells, exec);
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
        final var req = new RunReportRequest();

        final var ranges = Arrays.stream(settings.m_dateRanges).map(
                r -> new DateRange()
                    .setStartDate(r.m_fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .setEndDate(r.m_toDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .setName(r.m_rangeName))
            .collect(Collectors.toList());
        req.setDateRanges(ranges);

        final var dimensions = Arrays.stream(settings.m_gaDimensions).map(
                d -> new Dimension()
                    .setName(d.m_name))
            .collect(Collectors.toList());
        req.setDimensions(dimensions);

        final var metrics = Arrays.stream(settings.m_gaMetrics).map(
                m -> new Metric()
                    .setName(m.m_name)
                    .setExpression(m.m_expression)
                    .setInvisible(m.m_invisible))
            .collect(Collectors.toList());
        req.setMetrics(metrics);

        final var filter = settings.m_gaDimensionFilter;
        req.setDimensionFilter(createDimensionFilterExpression(filter));

        final var orderBys = Arrays.stream(settings.m_gaOrderBy).map(GAQueryNodeModel::mapToOrderBy)
            .collect(Collectors.toList());
        req.setOrderBys(orderBys);

        req.setLimit(Long.valueOf(limit));

        req.setCurrencyCode(settings.m_currencyCode);
        req.setKeepEmptyRows(settings.m_keepEmptyRows);

        req.setReturnPropertyQuota(settings.m_returnPropertyQuota);

        return req;
    }


    private static OrderBy mapToOrderBy(final GAOrderBy o) {
        final var orderBy = new OrderBy().setDesc(o.m_sortOrder == SortOrder.DESCENDING);
        return switch (o.m_selectedType) {
            case METRIC -> orderBy.setMetric(new MetricOrderBy().setMetricName(o.m_orderByMetric));
            case DIMENSION -> {
                final var dim = o.m_orderByDimension;
                yield orderBy.setDimension(new DimensionOrderBy().setDimensionName(dim.m_dimensionName)
                        .setOrderType(dim.m_orderType.name()));
            }
        };
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
            addDateRange(dateRangeName, settings.m_dateRanges, settings.m_includeDateRangeNameColumn, cells::add);
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
    private void addDateRange(final String name, final GADateRange[] ranges,
            final boolean includeDateRangeNameColumn, final Consumer<DataCell> cellCollector) {
        CheckUtils.checkArgument(ranges.length > 0, "Date ranges are missing.");
        if (ranges.length == 1) {
            // only one date range specified, so we probably did not get a date range name from the API
            final var range = ranges[0];
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
            LOGGER.warn("Google Analytics Quota Status \"%s\" was not returned from API.".formatted(infix));
            return;
        }
        pushFlowVariableInt("analytics.quota.%s.consumed".formatted(infix), status.getConsumed());
        pushFlowVariableInt("analytics.quota.%s.remaining".formatted(infix), status.getRemaining());
    }

    private void pushFlowVariablesPropertyQuota(final PropertyQuota quota) {
        pushFlowVariablesQuota("concurrentRequests", quota.getConcurrentRequests());
        pushFlowVariablesQuota("potentiallyThresholdedRequestsPerHour", quota.getPotentiallyThresholdedRequestsPerHour());
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

    private static FilterExpression createDimensionFilterExpression(final GADimensionFilterExpression fx) {
        final var filters = fx.m_filters;
        if (filters.length == 0) {
            return null;
        } else if (filters.length == 1) {
            return mapToFilterExpression(filters[0]);
        }
        final var fel = new FilterExpressionList().setExpressions(
            Arrays.stream(filters).map(GAQueryNodeModel::mapToFilterExpression).collect(Collectors.toList()));
        final var fex = new FilterExpression();
        return switch (fx.m_connectVia) { // NOSONAR exhaustiveness check
            case AND -> fex.setAndGroup(fel);
            case OR -> fex.setOrGroup(fel);
        };
    }

    private static FilterExpression mapToFilterExpression(final GADimensionFilter filter) {
        final var f = new Filter().setFieldName(filter.m_name);
        switch (filter.m_selectedType) { // NOSONAR exhaustive switch notifies us automatically
                                         // when we add the other enum values
            case STRING -> {
                final var sf = filter.m_stringFilter;
                f.setStringFilter(new StringFilter().setMatchType(sf.m_matchType.name()).setValue(sf.m_value)
                    .setCaseSensitive(filter.m_caseSensitivity == CaseSensitivity.CASE_SENSITIVE));
            }
            case IN_LIST ->
                f.setInListFilter(new InListFilter().setValues(
                    Arrays.stream(filter.m_inListFilter.m_values).map(fv -> fv.m_value).collect(Collectors.toList()))
                    .setCaseSensitive(filter.m_caseSensitivity == CaseSensitivity.CASE_SENSITIVE));
        }
        final var exp = new FilterExpression().setFilter(f);
        if (filter.m_isNegated) {
            return new FilterExpression().setNotExpression(exp);
        }
        return exp;
    }
}
