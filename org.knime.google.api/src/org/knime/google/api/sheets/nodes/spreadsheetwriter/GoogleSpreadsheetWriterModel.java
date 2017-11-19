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
package org.knime.google.api.sheets.nodes.spreadsheetwriter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.util.DesktopUtil;
import org.knime.google.api.sheets.data.GoogleSheetsConnection;
import org.knime.google.api.sheets.data.GoogleSheetsConnectionPortObject;

import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Create;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * The model to the Google Spreadsheet Writer node.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class GoogleSpreadsheetWriterModel extends NodeModel {


    GoogleSpreadsheetWriterSettings m_settings = new GoogleSpreadsheetWriterSettings();

    /**
     * Constructor of the node model.
     */
    protected GoogleSpreadsheetWriterModel() {
        super(new PortType[]{GoogleSheetsConnectionPortObject.TYPE, BufferedDataTable.TYPE}, new PortType[]{});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        GoogleSheetsConnection connection =
                ((GoogleSheetsConnectionPortObject)inObjects[0]).getGoogleSheetsConnection();

        // column filter
        BufferedDataTable table = (BufferedDataTable)inObjects[1];
        DataTableSpec spec = table.getSpec();
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        FilterResult filter = m_settings.applyColumnFilter(spec);
        rearranger.keepOnly(filter.getIncludes());
        table = exec.createColumnRearrangeTable(table, rearranger, exec);


        String spreadsheetId = createSpreadsheet(connection, m_settings.getSpreadsheetName(), m_settings.getSheetName());


        writeSpreadsheet(connection, table, m_settings.writeRaw(), spreadsheetId,
            m_settings.getSheetName(), m_settings.addRowHeader(), m_settings.addColumnHeader(),
            m_settings.handleMissingValues(), m_settings.getMissingValuePattern());

        if (m_settings.openAfterExecution()) {
            openSpreadsheetInBrowser(
                connection.getSheetsService().spreadsheets().get(spreadsheetId).execute().getSpreadsheetUrl());

        }

        pushFlowvariables(m_settings.getSpreadsheetName(), spreadsheetId, m_settings.getSheetName());

        return new PortObject[]{};
    }

    /**
     * Opens the given url in the default browser if not executed in headless mode.
     *
     * @param spreadsheetUrl The url that should be opened
     * @throws MalformedURLException
     */
    public static void openSpreadsheetInBrowser(final String spreadsheetUrl) throws MalformedURLException{
        if (!Boolean.getBoolean("java.awt.headless")) {
            DesktopUtil.browse(new URL(spreadsheetUrl));
        }

    }

    /**
     * Creates a new spreadsheet given the spreadsheet name and the sheet name.
     *
     * @param sheetConnection The Google Sheet connection to use
     * @param spreadsheetName The spreadsheet name
     * @param sheetName The sheet name
     * @return The spreadsheet id of the created spreadsheet.
     * @throws IOException If the spreadsheet could not be created
     */
    private static String createSpreadsheet(final GoogleSheetsConnection sheetConnection, final String spreadsheetName,
        final String sheetName) throws IOException {
        Spreadsheet spreadsheet = new Spreadsheet();
        SpreadsheetProperties spreadsheetProperties = new SpreadsheetProperties();
        spreadsheetProperties.setTitle(spreadsheetName);
        spreadsheet.setProperties(spreadsheetProperties);

        Sheet sheet = new Sheet();
        SheetProperties sheetProperties = new SheetProperties();
        sheetProperties.setTitle(sheetName);
        sheet.setProperties(sheetProperties);
        List<Sheet> sheetList = new ArrayList<Sheet>(Arrays.asList(sheet));
        spreadsheet.setSheets(sheetList);
        Create create = sheetConnection.getSheetsService().spreadsheets().create(spreadsheet);

        Spreadsheet execute = create.execute();
        return execute.getSpreadsheetId();
    }

    /**
     * Writes the content of the given data table to the given spreadsheet and sheet ,
     * provided the given google sheets connection. Using the passed settings.
     *
     *
     * @param sheetConnection
     * @param dataTable
     * @param writeRaw
     * @param spreadsheetId
     * @param sheetName
     * @param addRowHeader
     * @param addColumnHeader
     * @param handleMissingValues
     * @param missingValuePattern
     * @throws IOException
     */
    public static void writeSpreadsheet(final GoogleSheetsConnection sheetConnection, final BufferedDataTable dataTable,
        final boolean writeRaw, final String spreadsheetId, final String sheetName,final boolean addRowHeader,
        final boolean addColumnHeader, final boolean handleMissingValues, final String missingValuePattern) throws IOException {
        final String valueInputOption = writeRaw ? "RAW" : "USER_ENTERED";

        ValueRange body =
            collectSheetData(dataTable, addRowHeader, addColumnHeader, handleMissingValues, missingValuePattern);
        sheetConnection.getSheetsService().spreadsheets().values()
                .append(spreadsheetId, sheetName, body)
                .setValueInputOption(valueInputOption)
                .execute();
    }

    /**
     * Collects the data from a {@link BufferedDataTable} to be written to a google sheet. Given the provided options.
     *
     * @param dataTable The {@link BufferedDataTable} that should be prepared for google sheet
     * @param addRowHeader Whether the row id should be added as a row header
     * @param addColumnHeader Whether the column name should be added as a column header
     * @param handleMissingValues Whether missing values should be handled specially
     * @param missingValuePattern The pattern that should be used for missing values,
     *          when they should be handled specially.
     * @return The ValueRange that can be written to google sheets
     */
    public static ValueRange collectSheetData(final BufferedDataTable dataTable, final boolean addRowHeader,
        final boolean addColumnHeader, final boolean handleMissingValues, final String missingValuePattern) {
        List<List<Object>> sheetData = new ArrayList<List<Object>>();
        if (addColumnHeader) {
            DataTableSpec dataTableSpec = dataTable.getDataTableSpec();
            String[] tableColumnNames = dataTableSpec.getColumnNames();
            List<Object> headers = new ArrayList<Object>();
            if (addRowHeader) {
                headers.add("Row ID");
            }
            for (int i = 0; i < tableColumnNames.length; i++) {
                headers.add(tableColumnNames[i].toString());
            }
            sheetData.add(headers);
        }
        CloseableRowIterator iterator = dataTable.iterator();
        for (int i = 0; i < dataTable.size(); i++) {
            DataRow next = iterator.next();
            Iterator<DataCell> iterator2 = next.iterator();
            List<Object> sheetRow = new ArrayList<Object>();
            if (addRowHeader) {
               sheetRow.add(next.getKey().toString());
            }
            while (iterator2.hasNext()) {
                DataCell next2 = iterator2.next();
                if (next2.isMissing()){
                    if (handleMissingValues) {
                        sheetRow.add(missingValuePattern);
                    } else {
                        sheetRow.add("");
                    }
                } else {
                    sheetRow.add(next2.toString());
                }
            }
            sheetData.add(sheetRow);
        }
        iterator.close();
        ValueRange body = new ValueRange().setValues(sheetData);
        return body;
    }



    /**
     * Pushes the given information to the flowvariables.
     *
     * @param spreadsheetName The spreadsheet name
     * @param spreadsheetId spreadsheeId
     * @param sheetName sheetName
     *
     */
    protected void pushFlowvariables(final String spreadsheetName, final String spreadsheetId, final String sheetName) {
        final Set<String> variables = getAvailableFlowVariables().keySet();
        String spreadsheetNameVar = "spreadsheetName";

        String postfix = "";
        if (variables.contains(spreadsheetNameVar)) {
            int i = 2;
            postfix += "_";
            while (variables.contains(spreadsheetNameVar + i)) {
                i++;
            }
            postfix += i;
        }
        pushFlowVariableString(spreadsheetNameVar + postfix, spreadsheetName);
        pushFlowVariableString("spreadsheetId" + postfix, spreadsheetId);
        pushFlowVariableString("sheetName" + postfix, sheetName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{};
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
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}Name
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
}
