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
package org.knime.google.api.sheets.nodes.sheetupdater;

import java.io.File;
import java.io.IOException;
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

import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * The model to the Google Sheets Updater node.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class GoogleSheetUpdaterModel extends NodeModel {


    GoogleSheetUpdaterSettings m_settings = new GoogleSheetUpdaterSettings();

    /**
     * Constructor of the node model.
     */
    protected GoogleSheetUpdaterModel() {
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

        String sheetRange = !m_settings.useRange() ?
            m_settings.getSheetName() : m_settings.getSheetName() + "!" + m_settings.getRange();

        if (m_settings.append()) {
            GoogleSpreadsheetWriterModel.writeSpreadsheet(connection, table, m_settings.writeRaw(), spreadsheetId,
                m_settings.getSheetName(), m_settings.addRowHeader(), m_settings.addColumnHeader(),
                m_settings.handleMissingValues(), m_settings.getMissingValuePattern());
        } else {
            updateSpreadsheet(connection, table, m_settings.writeRaw(), spreadsheetId,
                sheetRange, m_settings.addRowHeader(), m_settings.addColumnHeader(),
                m_settings.handleMissingValues(), m_settings.getMissingValuePattern());
        }

        if (m_settings.openAfterExecution()) {
            GoogleSpreadsheetWriterModel.openSpreadsheetInBrowser(
                connection.getSheetsService().spreadsheets().get(spreadsheetId).execute().getSpreadsheetUrl());

        }

        pushFlowvariables(m_settings.getSpreadsheetId(), spreadsheetId, m_settings.getSheetName());

        return new PortObject[]{};
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
    public static void updateSpreadsheet(final GoogleSheetsConnection sheetConnection,
        final BufferedDataTable dataTable, final boolean writeRaw, final String spreadsheetId, final String sheetName,
        final boolean addRowHeader, final boolean addColumnHeader, final boolean handleMissingValues,
        final String missingValuePattern) throws IOException {
        final String valueInputOption = writeRaw ? "RAW" : "USER_ENTERED";

        ValueRange body =
                GoogleSpreadsheetWriterModel.collectSheetData(dataTable, addRowHeader, addColumnHeader,
                    handleMissingValues, missingValuePattern);

        sheetConnection.getSheetsService().spreadsheets().
        values().update(spreadsheetId, sheetName,body).
        setValueInputOption(valueInputOption).execute();
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
