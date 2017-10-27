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
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.UniqueNameGenerator;
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

    GoogleSheetsReaderSettings m_settings = getSettings();

    /**
     * Returns the settings for the Google Sheets Reader.
     *
     * @return The settings for the google sheets reader
     */
    protected static GoogleSheetsReaderSettings getSettings() {
        return new GoogleSheetsReaderSettings();
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

        exec.setMessage("Requesting Google Sheet");
        ValueRange result =
            createGetRequest(connection, m_settings.getSpreadSheetId(), m_settings.getRange()).execute();

        if (result == null) {
            throw new IOException("Could not get the requested data");
        }

        List<List<Object>> values = result.getValues();
        if (values == null) {
            throw new InvalidSettingsException("Specified Sheet or range is empty");
        }
        DataTableSpec outSpec = createSpec(values.get(0));
        BufferedDataContainer outContainer = exec.createDataContainer(outSpec);
        UniqueNameGenerator rowIDGen = new UniqueNameGenerator(Collections.emptySet());
        int i = m_settings.readColName() ? 1 : 0;
        for (; i < values.size(); i++) {
            exec.checkCanceled();
            exec.setProgress((i == 0) ? values.size() / (i + 1) : values.size() / i, "Reading row " + i);
            List<Object> row = values.get(i);
            List<DataCell> cells = new ArrayList<DataCell>(outSpec.getNumColumns());
            int leftBound = m_settings.readRowId() ? 1 : 0;
            for (int j = leftBound; j <= outSpec.getNumColumns() - (1 - leftBound); j++) {
                String value = "";
                if (j < row.size()) {
                    value = row.get(j).toString();
                }
                if (StringUtils.isEmpty(value)) {
                    cells.add(DataType.getMissingCell());
                } else {
                    cells.add(new StringCell(value));
                }
            }
            int rowNum = m_settings.readColName() ? i - 1 : i;
            String rowIdString = "Row" + (rowNum);
            if (m_settings.readRowId()) {
                if (row.size() > 0) {
                    rowIdString = rowIDGen.newName(StringUtils.defaultIfBlank(row.get(0).toString(), rowIdString));
                }
            }
            outContainer.addRowToTable(new DefaultRow(rowIdString, cells));
        }
        outContainer.close();
        return new PortObject[]{outContainer.getTable()};
    }

    /**
     * Creates the get request to retrieve a spreadsheet given the spreadsheet id and the range.
     *
     * @param connection The connection to use
     * @param spreadsheetId The spreadsheet id for the spreadsheet to retrieve
     * @param sheetRange The range which should be retrieved
     * @return A get request for the currently configured query
     * @throws IOException If an IO error occurs
     */
    public Get createGetRequest(final GoogleSheetsConnection connection, final String spreadsheetId,
        final String sheetRange) throws IOException {
        Get get = connection.getSheetsService().spreadsheets().values().get(spreadsheetId, sheetRange);
        return get;
    }

    /**
     * @param list Google Analytics data object
     * @return The KNIME table spec for the given data object
     */
    private DataTableSpec createSpec(final List<Object> list) {
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>(list.size());
        UniqueNameGenerator nameGen = new UniqueNameGenerator(Collections.emptySet());
        int i = m_settings.readRowId() ? 1 : 0;
        for (; i < list.size(); i++) {
            int colNumber = m_settings.readRowId() ? i - 1 : i;
            String colName = StringUtils.trimToNull(list.get(i).toString());
            if (!m_settings.readColName() || colName == null) {
                colName = "Col" + colNumber;
            }
            colSpecs.add(nameGen.newColumn(colName, StringCell.TYPE));
        }
        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings.getSpreadSheetId().isEmpty() || m_settings.getSheetName().isEmpty()) {
            throw new InvalidSettingsException("No settings available");
        }
        // TODO make sure we at least try to guess the column types in the future. Behaviour maybe similiar to excel node. Also same missing value handling (if any?).
        return new PortObjectSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //nothing to do
    }

}
