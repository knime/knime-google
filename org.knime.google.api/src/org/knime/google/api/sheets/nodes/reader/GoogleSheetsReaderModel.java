/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 21, 2017 (oole): created
 */
package org.knime.google.api.sheets.nodes.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.google.api.sheets.data.GoogleSheetsConnection;
import org.knime.google.api.sheets.data.GoogleSheetsConnectionPortObject;

import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get;
import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * The model for the GoogleSheetsReader node.
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
public class GoogleSheetsReaderModel extends NodeModel {

    private SettingsModelString m_spreadsheetIdModel = getSpreadSheetIdModel();

    private SettingsModelString m_sheetRangeModel = getSheetRangeModel();

    private SettingsModelBoolean m_readColNameModel = getReadColNameModel();

    private SettingsModelBoolean m_readRowIdModel = getReadRowIdModel();

    /**
     * Returns the  {@link SettingsModelString} for the spreadsheet id.
     *
     * @return The  {@link SettingsModelString} for the spreadsheet id
     */
    protected static SettingsModelString getSpreadSheetIdModel() {
        return new SettingsModelString("spreadsheetID", "");
    }

    /**
     * Returns the  {@link SettingsModelString} for the sheet name.
     *
     * @return The  {@link SettingsModelString} for the sheet name
     */
    protected static SettingsModelString getSheetRangeModel() {
        return new SettingsModelString("sheetRange", "");
    }

    /**
     * Returns the  {@link SettingsModelBoolean} for the column name reading.
     *
     * @return The  {@link SettingsModelBoolean} for the column name reading
     */
    protected static SettingsModelBoolean getReadColNameModel() {
        return new SettingsModelBoolean("readColName", true);
    }

    /**
     * Returns the  {@link SettingsModelBoolean} for the row id reading.
     *
     * @return The  {@link SettingsModelBoolean} for the row id reading
     */
    protected static SettingsModelBoolean getReadRowIdModel() {
        return new SettingsModelBoolean("readRowId", true);
    }

    /**
     * Constructor of the node model.
     */
    protected GoogleSheetsReaderModel() {
        super(new PortType[]{GoogleSheetsConnectionPortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        GoogleSheetsConnection connection =
                ((GoogleSheetsConnectionPortObject)inObjects[0]).getGoogleSheetsConnection();
        ValueRange result = null;
        result = createGetRequest(connection, m_spreadsheetIdModel.getStringValue().trim(),
            m_sheetRangeModel.getStringValue().trim()).execute();

        if (result == null) {
            throw new IOException("Could not get the requested data");
        }


       List<List<Object>> values = result.getValues();
       DataTableSpec outSpec = createSpec(values.get(0));
       BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
       int i = m_readColNameModel.getBooleanValue() ? 1 : 0;
       for (; i < values.size(); i++) {
           List<Object> row = values.get(i);
           List<DataCell> cells = new ArrayList<DataCell>(outSpec.getNumColumns());
           int leftBound = m_readRowIdModel.getBooleanValue() ? 1 : 0;
           for (int j = leftBound; j <= outSpec.getNumColumns() - (1 - leftBound); j++) {
               String value = "";
               if (j < row.size()) {
                   value = row.get(j).toString();
               }
               if (value.equals("")) {
                   cells.add(new MissingCell("mising"));
               } else {
                   cells.add(new StringCell(row.get(j).toString()));
               }
           }
           int rowNum = m_readColNameModel.getBooleanValue() ? i-1 : i;
           if (m_readRowIdModel.getBooleanValue()) {
               outContainer.addRowToTable(new DefaultRow(row.get(0).toString(), cells));
           } else {
               outContainer.addRowToTable(new DefaultRow("Row" + (rowNum), cells));
           }
       }
        outContainer.close();
        return new PortObject[]{outContainer.getTable()};
    }

    /**
     * @param list Google Analytics data object
     * @return The KNIME table spec for the given data object
     */
    private DataTableSpec createSpec(final List<Object> list) {
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>(list.size());
        int i = m_readRowIdModel.getBooleanValue() ? 1 :0;
        if (m_readColNameModel.getBooleanValue()) {
            for (; i < list.size(); i++) {
                colSpecs.add(new DataColumnSpecCreator(list.get(i).toString(), StringCell.TYPE).createSpec());
            }
        } else {
            for (; i < list.size(); i++) {
                int colNumber = m_readRowIdModel.getBooleanValue() ? i-1 : i;
                colSpecs.add(new DataColumnSpecCreator("Col"+ colNumber, StringCell.TYPE).createSpec());
            }
        }
        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // TODO make sure we at least try to guess the column types in the future. Behaviour maybe similiar to excel node. Also same missing value handling (if any?).
        return new PortObjectSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_spreadsheetIdModel.saveSettingsTo(settings);
        m_sheetRangeModel.saveSettingsTo(settings);
        m_readColNameModel.saveSettingsTo(settings);
        m_readRowIdModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_spreadsheetIdModel.validateSettings(settings);
        m_sheetRangeModel.validateSettings(settings);
        m_readColNameModel.validateSettings(settings);
        m_readRowIdModel.validateSettings(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_spreadsheetIdModel.loadSettingsFrom(settings);
        m_sheetRangeModel.loadSettingsFrom(settings);
        m_readColNameModel.loadSettingsFrom(settings);
        m_readRowIdModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // not used
    }

    /**
     * Creates the get request to retrieve a spreadsheet given the spreadsheet id and the range.
     *
     * @param connection The connection to use
     * @param spreadsheetId The spreadsheet id for the spreadsheet to retreive
     * @param sheetRange The range which should be retrieved
     * @return A get request for the currently configured query
     * @throws IOException If an IO error occurs
     */
    public Get createGetRequest(final GoogleSheetsConnection connection, final String spreadsheetId,
        final String sheetRange) throws IOException {
        Get get = connection.getSheetsService().spreadsheets().values().get(spreadsheetId, sheetRange);
        return get;
    }

}
