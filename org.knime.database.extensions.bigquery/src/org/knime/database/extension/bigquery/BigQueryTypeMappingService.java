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
 */
package org.knime.database.extension.bigquery;

import java.sql.JDBCType;
import java.sql.SQLType;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.database.datatype.mapping.AbstractDBDataTypeMappingService;

/**
 * Type mapping service for the Google BigQuery database.
 *
 * @author Noemi Balassa
 */
public final class BigQueryTypeMappingService
    extends AbstractDBDataTypeMappingService<BigQuerySource, BigQueryDestination> {

    private static final BigQueryTypeMappingService INSTANCE = new BigQueryTypeMappingService();

    /**
     * Gets the singleton {@link BigQueryTypeMappingService} instance.
     *
     * @return the only {@link BigQueryTypeMappingService} instance.
     */
    public static BigQueryTypeMappingService getInstance() {
        return INSTANCE;
    }

    private BigQueryTypeMappingService() {
        super(BigQuerySource.class, BigQueryDestination.class);
        final Map<DataType, SQLType> consumptionMap = new LinkedHashMap<>(getDefaultConsumptionMap());
        consumptionMap.put(BinaryObjectDataCell.TYPE, JDBCType.VARBINARY);
        consumptionMap.put(BooleanCell.TYPE, JDBCType.BOOLEAN);
        consumptionMap.put(IntCell.TYPE, JDBCType.BIGINT);
        consumptionMap.put(ZonedDateTimeCellFactory.TYPE, JDBCType.VARCHAR);
        setDefaultConsumptionPathsFrom(consumptionMap);
        setDefaultProductionPathsFrom(getDefaultProductionMap());
        addColumnType(JDBCType.BIGINT, "int64");
        addColumnType(JDBCType.BOOLEAN, "bool");
        addColumnType(JDBCType.DOUBLE, "float64");
        addColumnType(JDBCType.INTEGER, "int64");
        addColumnType(JDBCType.VARBINARY, "bytes");
        addColumnType(JDBCType.VARCHAR, "string");
    }

}