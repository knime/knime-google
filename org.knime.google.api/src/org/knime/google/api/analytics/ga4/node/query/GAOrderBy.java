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

import java.util.Arrays;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings for a Google Analytics 4 Data API
 * <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/rest/v1beta/OrderBy">OrderBy</a>
 * specification.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui*
final class GAOrderBy implements DefaultNodeSettings {

    @Widget(title = "Sort order")
    SortOrder m_sortOrder = SortOrder.DESCENDING;

    @Widget(title = "Order by")
    OrderByType m_selectedType = OrderByType.METRIC;

    // BEGIN Union type (how to better represent a Union type and have it show up correctly in the modern ui?)
    @Widget(title = "Metric")
    String m_orderByMetric;

    @Widget(title = "Dimension")
    GAOrderByDimension m_orderByDimension;
    // END Union


    GAOrderBy() {
        // ser/de
    }

    GAOrderBy(@SuppressWarnings("unused") final SettingsCreationContext ctx) {
        m_orderByDimension = new GAOrderByDimension();
    }

    enum OrderByType {
        @Widget(title="Metric")
        METRIC,
        @Widget(title="Dimension")
        DIMENSION;
        // PIVOT?
    }

    enum SortOrder {
        @Widget(title="Ascending")
        ASCENDING,
        @Widget(title="Descending")
        DESCENDING;
    }

    /**
     * Checks that the part of the "union" we are interested in is valid. Due to the way the UI handles the display
     * of the union, both can be set at the same time.
     *
     * @param <X> exception type
     * @param toThrowable function to provide an exception given a message
     * @throws X the exception in case the check fails
     */
    private <X extends Throwable> void checkUnionValid(final Function<String, X> toThrowable) throws X {
        switch (m_selectedType) { // NOSONAR We want a warning about missing case label for enums,
            // e.g. for when we add PIVOT
            case METRIC -> { // NOSONAR
                CheckUtils.check(StringUtils.isNotBlank(m_orderByMetric), toThrowable,
                    () -> "Metric name must not be blank.");
            }
            case DIMENSION -> {
                CheckUtils.check(m_orderByDimension != null, toThrowable, () -> "Dimension is missing.");
                CheckUtils.check(StringUtils.isNotBlank(m_orderByDimension.m_dimensionName), toThrowable,
                    () -> "Dimension name must not be blank.");
            }
        }
    }

    void validate(final GAMetric[] metrics, final GADimension[] dimensions) throws InvalidSettingsException {
        checkUnionValid(InvalidSettingsException::new);
        // each order by must be present as either a visible metric or a dimension
        switch(m_selectedType) { // NOSONAR We want a warning about missing case label for enums,
            // e.g. for when we add PIVOT
            case METRIC -> checkInMetrics(metrics);
            case DIMENSION -> checkInDimensions(dimensions);
        }
    }

    private void checkInMetrics(final GAMetric[] metrics) throws InvalidSettingsException {
        if (Arrays.stream(metrics).noneMatch(m -> m.m_name.equals(m_orderByMetric) && !m.m_invisible)) {
            throw new InvalidSettingsException(
                "The metric order by \"%s\" must be in the list of metrics and be visible (advanced setting)."
                    .formatted(m_orderByMetric));
        }
    }

    private void checkInDimensions(final GADimension[] dimensions) throws InvalidSettingsException {
        if (Arrays.stream(dimensions)
                .noneMatch(m -> m_orderByDimension != null && m.m_name.equals(m_orderByDimension.m_dimensionName))) {
            throw new InvalidSettingsException(
                "The dimension order by \"%s\" must be in the list of dimensions."
                    .formatted(m_orderByDimension.m_dimensionName));
        }
    }
}
