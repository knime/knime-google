/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 * 
 * History
 *   Mar 19, 2014 ("Patrick Winter"): created
 */
package org.knime.google.api.analytics.nodes.query;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.google.api.analytics.data.GoogleAnalyticsConnection;
import org.knime.google.api.analytics.data.GoogleAnalyticsConnectionPortObject;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.analytics.model.GaData;

/**
 * The model of the GoogleAnalyticsQuery node.
 * 
 * @author "Patrick Winter", University of Konstanz
 */
public class GoogleAnalyticsQueryModel extends NodeModel {

    private GoogleAnalyticsQueryConfiguration m_config = new GoogleAnalyticsQueryConfiguration();

    /**
     * Constructor of the node model.
     */
    protected GoogleAnalyticsQueryModel() {
        super(new PortType[]{GoogleAnalyticsConnectionPortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {
        GoogleAnalyticsConnection connection =
                ((GoogleAnalyticsConnectionPortObject)inObjects[0]).getGoogleAnalyticsConnection();
        GaData results = null;
        // Use exponential backoff in case the user rate limit is exceeded (by multiple running nodes)
        int retry = 0;
        int sleepTime = 0;
        Random random = new Random();
        try {
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            results = m_config.createGetRequest(connection).execute();
        } catch (GoogleJsonResponseException e) {
            if (retry < 5 && e.getMessage().contains("User Rate Limit Exceeded")) {
                sleepTime = 2^retry++ + random.nextInt(1000);
            } else {
                throw e;
            }
        }
        if (results == null) {
            throw new IOException("Could not get the requested data");
        }
        DataTableSpec outSpec = createSpec(results);
        BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
        List<List<String>> rows = results.getRows();
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                List<DataCell> cells = new ArrayList<DataCell>(outSpec.getNumColumns());
                for (int j = 0; j < row.size(); j++) {
                    // Use already determined type
                    DataType type = outSpec.getColumnSpec(j).getType();
                    if (type.equals(IntCell.TYPE)) {
                        cells.add(new IntCell(Integer.parseInt(row.get(j))));
                    } else if (type.equals(DoubleCell.TYPE)) {
                        cells.add(new DoubleCell(Double.parseDouble(row.get(j))));
                    } else {
                        String value = row.get(j);
                        if (value.equals("(not set)")) {
                            // '(not set)' is Googles version of missing value
                            cells.add(new MissingCell("(not set)"));
                        } else {
                            cells.add(new StringCell(row.get(j)));
                        }
                    }
                }
                outContainer.addRowToTable(new DefaultRow("Row" + i, cells));
            }
        }
        outContainer.close();
        return new PortObject[]{outContainer.getTable()};
    }

    /**
     * @param data Google Analytics data object
     * @return The KNIME table spec for the given data object
     */
    private DataTableSpec createSpec(final GaData data) {
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>(data.getColumnHeaders().size());
        for (GaData.ColumnHeaders colHeaders : data.getColumnHeaders()) {
            String type = colHeaders.getDataType();
            String name = colHeaders.getName().replaceFirst("ga:", "");
            if (type.equals("INTEGER")) {
                colSpecs.add(new DataColumnSpecCreator(name, IntCell.TYPE).createSpec());
            } else if (type.equals("DOUBLE") || type.equals("PERCENT") || type.equals("TIME") || type.equals("FLOAT")
                    || type.equals("CURRENCY")) {
                // All of these are simple floating point numbers
                colSpecs.add(new DataColumnSpecCreator(name, DoubleCell.TYPE).createSpec());
            } else {
                colSpecs.add(new DataColumnSpecCreator(name, StringCell.TYPE).createSpec());
            }
        }
        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(File nodeInternDir, ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(File nodeInternDir, ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) {
        m_config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
        GoogleAnalyticsQueryConfiguration config = new GoogleAnalyticsQueryConfiguration();
        config.loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
        GoogleAnalyticsQueryConfiguration config = new GoogleAnalyticsQueryConfiguration();
        config.loadInModel(settings);
        m_config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // not used
    }

}
