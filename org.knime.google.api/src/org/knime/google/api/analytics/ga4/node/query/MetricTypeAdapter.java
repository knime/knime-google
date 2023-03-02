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

import java.util.Optional;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * Adapter to parse
 * Google Analytics 4 Data API v1
 * <a href="https://developers.google.com/analytics/devguides/reporting/data/v1/rest/v1beta/MetricType">MetricType</a>
 * response data into data cells of specific KNIME data types.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class MetricTypeAdapter {

    private DataType m_dataType;
    private Function<String, ? extends DataCell> m_toCell;

    <T extends DataCell> MetricTypeAdapter(final DataType type, final Function<String, T> toCell) {
        m_dataType = type;
        m_toCell = toCell;
    }

    static Optional<MetricTypeAdapter> lookup(final String type) {
        return Optional.ofNullable(switch (type) {
            /** Unspecified type. */
            case "METRIC_TYPE_UNSPECIFIED" -> new MetricTypeAdapter(StringCell.TYPE, StringCell::new);
            /** Integer type. */
            case "TYPE_INTEGER" -> new MetricTypeAdapter(IntCell.TYPE, s -> new IntCell(Integer.parseInt(s)));

            case "TYPE_FLOAT", // Floating point type.
                 "TYPE_SECONDS", // Duration of seconds; a special floating point type.
                 "TYPE_MILLISECONDS", // Duration of milliseconds; a special floating point type.
                 "TYPE_MINUTES", // Duration of minutes; a special floating point type.
                 "TYPE_HOURS", // Duration of hours; a special floating point type.
                 "TYPE_STANDARD", // Custom metric of standard type; a special floating point type.
                 "TYPE_CURRENCY", // Amount of money; a special floating point type.
                 "TYPE_FEET", // Length in feed; a special floating point type.
                 "TYPE_MILES", // Length in miles; a special floating point type.
                 "TYPE_METERS", // Length in meters; a special floating point type.
                 "TYPE_KILOMETERS" // Length in kilometers; a special floating point type.
                -> new MetricTypeAdapter(DoubleCell.TYPE, s -> new DoubleCell(Double.parseDouble(s)));
            default -> null;
        });
    }

    /**
     * Get the KNIME data type used in this metric type adapter.
     *
     * @return the data type used by this metric type adapter
     */
    DataType getDataType() {
        return m_dataType;
    }

    /**
     * Parse the value according to the data type.
     * @param value value to parse
     * @return a data cell containing the parsed value
     */
    DataCell parse(final String value) {
        return m_toCell.apply(value);
    }

}
