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
package org.knime.google.api.sheets.nodes.writer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
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

import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Create;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * The model of the GoogleSheetsReader node.
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
public class GoogleSheetsWriterModel extends NodeModel {

    private SettingsModelString m_spreadsheetNameModel = getSpreadsheetNameModel();

    private SettingsModelString m_sheetNameModel = getSheetnameModel();

    private SettingsModelBoolean m_writeColNameModel = getWriteColNameModel();

    private SettingsModelBoolean m_writeRowIdModel = getWriteRowIdModel();

    private SettingsModelBoolean m_writeRawModel = getWriteRawModel();


    /**
     * Returns the {@link SettingsModelString} for the spreadsheet name.
     *
     * @return The {@link SettingsModelString} for the spreadsheet name
     */
    protected static SettingsModelString getSpreadsheetNameModel() {
        return new SettingsModelString("spreadsheetName", "");
    }

    /**
     * Returns the {@link SettingsModelString} for the sheet name.
     *
     * @return The {@link SettingsModelString} for the sheet name
     */
    protected static SettingsModelString getSheetnameModel() {
        return new SettingsModelString("sheetName", "");
    }

    /**
     * Returns the {@link SettingsModelBoolean} for the column name writing.
     *
     * @return The {@link SettingsModelBoolean} for the column name writing
     */
    protected static SettingsModelBoolean getWriteColNameModel() {
        return new SettingsModelBoolean("writeColName", true);
    }

    /**
     * Returns the {@link SettingsModelBoolean} for the row id writing.
     *
     * @return The {@link SettingsModelBoolean} for the row id writing
     */
    protected static SettingsModelBoolean getWriteRowIdModel() {
        return new SettingsModelBoolean("writeRowId", true);
    }

    /**
     * Returns the {@link SettingsModelBoolean} for raw writing.
     *
     * @return The {@link SettingsModelBoolean} for raw writing
     */
    protected static SettingsModelBoolean getWriteRawModel() {
        return new SettingsModelBoolean("writeRaw", true);
    }

    /**
     * Constructor of the node model.
     */
    protected GoogleSheetsWriterModel() {
        super(new PortType[]{GoogleSheetsConnectionPortObject.TYPE, BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        GoogleSheetsConnection connection =
                ((GoogleSheetsConnectionPortObject)inObjects[0]).getGoogleSheetsConnection();

        final String valueInputOption = m_writeRawModel.getBooleanValue() ? "RAW" : "USER_ENTERED";

        BufferedDataTable table = (BufferedDataTable)inObjects[1];

        Spreadsheet content = new Spreadsheet();
        SpreadsheetProperties properties = new SpreadsheetProperties();
        properties.setTitle(m_spreadsheetNameModel.getStringValue());
        content.setProperties(properties);
        Sheet sheet = new Sheet();
        SheetProperties sheetProperties = new SheetProperties();
        sheetProperties.setTitle(m_sheetNameModel.getStringValue());
        sheet.setProperties(sheetProperties);
        List<Sheet> sheetList = new ArrayList<Sheet>(Arrays.asList(sheet));
        content.setSheets(sheetList);
        Create create = connection.getSheetsService().spreadsheets().create(content);

        Spreadsheet execute = create.execute();
        if (execute == null) {
            throw new IOException("Could not get the requested data");
        }
        String spreadsheetId = execute.getSpreadsheetId();

        //column name as first row
        if (m_writeColNameModel.getBooleanValue()) {
            List<List<Object>> columnNames = new ArrayList<List<Object>>();
            DataTableSpec dataTableSpec = table.getDataTableSpec();
            String[] tableColumnNames = dataTableSpec.getColumnNames();
            List<Object> headers = new ArrayList<Object>();
            if (m_writeRowIdModel.getBooleanValue()) {
                headers.add("Row ID");
            }
            for (int i = 0; i < tableColumnNames.length; i++) {
                headers.add(tableColumnNames[i].toString());
            }
            columnNames.add(headers);
            ValueRange vRange = new ValueRange().setValues(columnNames);
            connection.getSheetsService()
                    .spreadsheets().values().append(spreadsheetId, m_sheetNameModel.getStringValue(), vRange)
                    .setValueInputOption(valueInputOption).execute();
        }
        // table content
        CloseableRowIterator iterator = table.iterator();
        List<List<Object>> sheetData = new ArrayList<List<Object>>();
        for (int i = 0; i < table.size(); i++) {
            DataRow next = iterator.next();
            Iterator<DataCell> iterator2 = next.iterator();
            List<Object> sheetRow = new ArrayList<Object>();
            if (m_writeRowIdModel.getBooleanValue()) {
               sheetRow.add(next.getKey().toString());
            }
            while (iterator2.hasNext()) {
                sheetRow.add(iterator2.next().toString());
            }
            sheetData.add(sheetRow);
        }
        ValueRange body = new ValueRange().setValues(sheetData);
        connection.getSheetsService().spreadsheets().values()
                .append(spreadsheetId, m_sheetNameModel.getStringValue(), body)
                .setValueInputOption(valueInputOption)
                .execute();

        BufferedDataContainer outContainer = exec.createDataContainer(createSpec());
        outContainer.addRowToTable(new DefaultRow("Row" + 0,
            new StringCell(m_spreadsheetNameModel.getStringValue()),new StringCell(spreadsheetId), new StringCell(m_sheetNameModel.getStringValue())));
        outContainer.close();

        return new PortObject[]{outContainer.getTable()};
    }

    /**
     * @return The KNIME table spec for the given data object
     */
    private DataTableSpec createSpec() {
        List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
        colSpecs.add(new DataColumnSpecCreator("SpreadsheetName", StringCell.TYPE).createSpec());
        colSpecs.add(new DataColumnSpecCreator("SpreadsheetID", StringCell.TYPE).createSpec());
        colSpecs.add(new DataColumnSpecCreator("SheetName", StringCell.TYPE).createSpec());
        return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createSpec()};
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
        m_spreadsheetNameModel.saveSettingsTo(settings);
        m_sheetNameModel.saveSettingsTo(settings);
        m_writeColNameModel.saveSettingsTo(settings);
        m_writeRowIdModel.saveSettingsTo(settings);
        m_writeRawModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_spreadsheetNameModel.validateSettings(settings);
        m_sheetNameModel.validateSettings(settings);
        m_writeColNameModel.validateSettings(settings);
        m_writeRowIdModel.validateSettings(settings);
        m_writeRawModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_spreadsheetNameModel.loadSettingsFrom(settings);
        m_sheetNameModel.loadSettingsFrom(settings);
        m_writeColNameModel.loadSettingsFrom(settings);
        m_writeRowIdModel.loadSettingsFrom(settings);
        m_writeRawModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // not used
    }
}
