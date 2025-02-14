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
 *
 * History
 *   Aug 21, 2017 (oole): created
 */
package org.knime.google.api.sheets.nodes.sheetupdater;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.workflow.VariableType;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.google.api.sheets.data.GoogleSheetsConnection;
import org.knime.google.api.sheets.data.GoogleSheetsConnectionPortObject;
import org.knime.google.api.sheets.nodes.reader.GoogleSheetsReaderModel;
import org.knime.google.api.sheets.nodes.spreadsheetwriter.GoogleSpreadsheetWriterModel;
import org.knime.google.api.sheets.nodes.util.NodesUtil;
import org.knime.google.api.sheets.nodes.util.RangeUtil;
import org.knime.google.api.sheets.nodes.util.RetryUtil;
import org.knime.google.api.sheets.nodes.util.ValueInputOption;

import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * The model to the Google Sheets Updater node.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class GoogleSheetUpdaterModel extends NodeModel {


    private static final NodeLogger LOGGER = NodeLogger.getLogger(GoogleSheetUpdaterModel.class);


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

        exec.setMessage("Updating Sheet");
        // column filter
        BufferedDataTable table = (BufferedDataTable)inObjects[1];
        DataTableSpec spec = table.getSpec();
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        FilterResult filter = m_settings.applyColumnFilter(spec);
        rearranger.keepOnly(filter.getIncludes());
        table = exec.createColumnRearrangeTable(table, rearranger, exec);

        String spreadsheetId = m_settings.getSpreadsheetId();

        final var sheetName = m_settings.selectFirstSheet()//
            ? GoogleSheetsReaderModel.getFirstSheet(connection, spreadsheetId, exec, LOGGER)//
            : m_settings.getSheetName();

        final var range = m_settings.useRange()//
            ? ("!" + m_settings.getRange())//
            : "";

        final var sheetRange = RangeUtil.quoteSheetName(sheetName) + range;

        if (m_settings.clearSheet()) {
            RetryUtil.withRetry(() -> RangeUtil.escapedRangeExecute(
                connection.getSheetsService().spreadsheets().values()
                .clear(m_settings.getSpreadsheetId(), sheetRange, new ClearValuesRequest())), exec);
        }
        if (m_settings.append()) {
            GoogleSpreadsheetWriterModel.writeSpreadsheet(connection, table, m_settings.writeRaw(), spreadsheetId,
                sheetRange, m_settings.addRowHeader(), m_settings.addColumnHeader(),
                m_settings.handleMissingValues(), m_settings.getMissingValuePattern(), exec);
        } else {
            updateSpreadsheet(connection, table, m_settings.writeRaw(), spreadsheetId,
                sheetRange, m_settings.addRowHeader(), m_settings.addColumnHeader(),
                m_settings.handleMissingValues(), m_settings.getMissingValuePattern(), exec);
        }

        if (m_settings.openAfterExecution()) {
            GoogleSpreadsheetWriterModel.openSpreadsheetInBrowser(RetryUtil.withRetry(
                () -> connection.getSheetsService().spreadsheets().get(spreadsheetId).execute(), exec)
                .getSpreadsheetUrl());
        }

        NodesUtil.pushFlowVariables(m_settings.getSpreadsheetName(), spreadsheetId, m_settings.getSheetName(),
            getAvailableFlowVariables(VariableType.StringType.INSTANCE).keySet(), this::pushFlowVariableString);

        return new PortObject[]{};
    }

    /**
     * Writes the content of the given data table to the given spreadsheet and sheet ,
     * provided the given google sheets connection. Using the passed settings.
     *
     *
     * @param sheetConnection The sheets connection to be used
     * @param dataTable The data table to be written to google sheets
     * @param writeRaw Whether the table should be written to google sheets in raw format
     * @param spreadsheetId The designated spreadsheet id
     * @param sheetName The designated sheet name
     * @param addRowHeader Whether the row header should be written to the sheet
     * @param addColumnHeader Whether the column header should be written to the sheet
     * @param handleMissingValues Whether missing values should be handled specially
     * @param missingValuePattern The missing value pattern that should be used, when handling them specially
     * @param exec The node execution context
     * @throws IOException If the spreadsheet cannot be written
     * @throws CanceledExecutionException If execution is canceled
     * @throws NoSuchCredentialException
     */
    private static void updateSpreadsheet(final GoogleSheetsConnection sheetConnection,
        final BufferedDataTable dataTable, final boolean writeRaw, final String spreadsheetId, final String sheetName,
        final boolean addRowHeader, final boolean addColumnHeader, final boolean handleMissingValues,
        final String missingValuePattern, final ExecutionContext exec)
                throws IOException, CanceledExecutionException, NoSuchCredentialException {


        final String valueInputOption = writeRaw ? ValueInputOption.RAW.name() : ValueInputOption.USER_ENTERED.name();

        ValueRange body =
                GoogleSpreadsheetWriterModel.collectSheetData(dataTable, addRowHeader, addColumnHeader,
                    handleMissingValues, missingValuePattern, exec);

        exec.setMessage("Updating Sheet.");
        RetryUtil.withRetry(() -> RangeUtil.escapedRangeExecute(sheetConnection.getSheetsService().spreadsheets().
            values().update(spreadsheetId, sheetName, body).setValueInputOption(valueInputOption)), exec) ;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings.getSpreadsheetId().trim().isEmpty()) {
            throw new InvalidSettingsException("Spreadsheet selection must not be empty!");
        }
        if (m_settings.getSheetName().trim().isEmpty()) {
            throw new InvalidSettingsException("Sheet name must not be empty!");
        }
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
