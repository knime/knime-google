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
package org.knime.google.api.sheets.nodes.sheetappender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
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
import org.knime.google.api.sheets.data.GoogleSheetsConnection;
import org.knime.google.api.sheets.data.GoogleSheetsConnectionPortObject;
import org.knime.google.api.sheets.nodes.spreadsheetwriter.GoogleSpreadsheetWriterModel;

import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;

/**
 * The model to the Google Sheet Appender node.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class GoogleSheetAppenderModel extends NodeModel {


    GoogleSheetAppenderSettings m_settings = new GoogleSheetAppenderSettings();

    /**
     * Constructor of the node model.
     */
    protected GoogleSheetAppenderModel() {
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

        String spreadsheetId = m_settings.getSpreadsheetId();

        String sheetName = createSheet(connection, spreadsheetId, m_settings.getSheetName(), m_settings.createUniqueSheetName());

        GoogleSpreadsheetWriterModel.writeSpreadsheet(connection, table, m_settings.writeRaw(), spreadsheetId,
            sheetName, m_settings.addRowHeader(), m_settings.addColumnHeader(),
            m_settings.handleMissingValues(), m_settings.getMissingValuePattern());

        if (m_settings.openAfterExecution()) {
            GoogleSpreadsheetWriterModel.openSpreadsheetInBrowser(
                connection.getSheetsService().spreadsheets().get(spreadsheetId).execute().getSpreadsheetUrl());

        }

        pushFlowvariables(m_settings.getSpreadsheetName(), spreadsheetId, sheetName);

        return new PortObject[]{};
    }

    /**
     * Creates a new spreadsheet given the spreadsheet name and the sheet name.
     *
     * @param sheetConnection The Google Sheet connection to use
     * @param spreadsheetName The spreadsheet name
     * @param sheetName The sheet name
     * @throws IOException If the spreadsheet could not be created
     */
    private static String createSheet(final GoogleSheetsConnection sheetConnection, final String spreadsheetId,
        final String sheetName, final boolean createUniqueSheetName) throws IOException {

        Spreadsheet spreadsheet = sheetConnection.getSheetsService().spreadsheets().get(spreadsheetId).execute();

        List<Sheet> existingSheets = spreadsheet.getSheets();

        String postfix = "";
        if (createUniqueSheetName) {
            int i = 1;
            boolean sheetDoesExist = true;
            while (sheetDoesExist){
                final String innerPostfix = postfix;
                if (existingSheets.stream().anyMatch(
                    sheet -> sheet.getProperties().getTitle().equals(sheetName + innerPostfix))) {
                    postfix =  " (#" + i + ")";
                    i++;
                } else{
                    sheetDoesExist = false;
                }
            }
        }
        SheetProperties sheetProperties = new SheetProperties();
        sheetProperties.setTitle(sheetName + postfix);

        List<Request> sheetCreationRequest = new ArrayList<>();
        sheetCreationRequest.add(new Request().setAddSheet(new AddSheetRequest().setProperties(sheetProperties)));

        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(sheetCreationRequest);
        sheetConnection.getSheetsService().spreadsheets().batchUpdate(spreadsheetId, body).execute();
        return sheetName + postfix;
    }


    /**
     * Pushes the given information to the flow variables.
     *
     * @param spreadsheetName The spreadsheet name to push
     * @param spreadsheetId The spreadsheet id to push
     * @param sheetName The sheet name to push
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
