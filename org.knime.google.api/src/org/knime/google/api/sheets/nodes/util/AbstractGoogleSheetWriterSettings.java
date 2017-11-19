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
 * ---------------------------------------------------------------------
 *
 * History
 *   Nov 14, 2017 (oole): created
 */
package org.knime.google.api.sheets.nodes.util;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelOptionalString;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.google.api.sheets.nodes.sheetappender.GoogleSheetAppenderModel;
import org.knime.google.api.sheets.nodes.sheetupdater.GoogleSheetUpdaterModel;
import org.knime.google.api.sheets.nodes.spreadsheetwriter.GoogleSpreadsheetWriterModel;

/**
 * Abstract class holding the default settings for the google sheets writer nodes.
 * Such as {@link GoogleSpreadsheetWriterModel}, {@link GoogleSheetUpdaterModel}, {@link GoogleSheetAppenderModel}.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class AbstractGoogleSheetWriterSettings {
    /** The default data port for the google sheets writer nodes. */
    protected static final int DATA_PORT = 1;

    private SettingsModelBoolean m_addColumnHeaderModel = getAddColumnHeaderModel();

    private SettingsModelBoolean m_addRowHeaderModel = getAddRowHeaderModel();

    private SettingsModelBoolean m_writeRawModel = getWriteRawModel();

    private SettingsModelBoolean m_openAfterExecutionModel = getOpenAfterExecutionModel();

    private SettingsModelColumnFilter2 m_columnFilterModel = getColumnFilterModel();

    private SettingsModelOptionalString m_missingValueModel = getMissingValueModel();

    /**
     * Returns the {@link SettingsModelColumnFilter2} for the column filter.
     *
     * @return The {@link SettingsModelColumnFilter2} for the column filter
     */
    protected static SettingsModelColumnFilter2 getColumnFilterModel() {
        return new SettingsModelColumnFilter2("columnFilter");
    }

    /**
     * Returns the {@link SettingsModelOptionalString} for the missing value handling.
     *
     * @return The {@link SettingsModelOptionalString} for the missing value handling
     */
    protected static SettingsModelOptionalString getMissingValueModel() {
        return new SettingsModelOptionalString("missingValue", "", false);
    }


    /**
     * Returns the {@link SettingsModelBoolean} for the column name writing.
     *
     * @return The {@link SettingsModelBoolean} for the column name writing
     */
    protected static SettingsModelBoolean getAddColumnHeaderModel() {
        return new SettingsModelBoolean("writeColName", true);
    }

    /**
     * Returns the {@link SettingsModelBoolean} for the row id writing.
     *
     * @return The {@link SettingsModelBoolean} for the row id writing
     */
    protected static SettingsModelBoolean getAddRowHeaderModel() {
        return new SettingsModelBoolean("writeRowId", true);
    }

    /**
     * Returns whether missing values should be handled specially.
     *
     * @return Whether missing values should be handled specially
     */
    public boolean handleMissingValues() {
        return m_missingValueModel.isActive();
    }

    /**
     *  The missing value pattern to apply when missing values should be handled specially.
     *
     * @return Missing value pattern to apply when missing values should be handled specially
     */
    public String getMissingValuePattern() {
        return m_missingValueModel.getStringValue();
    }

    /**
     * Applies configured column filter to the given {@link DataTableSpec}.
     *
     * @param spec The {@link DataTableSpec} to which the configured column filter should be applied
     * @return The result of the applied column filter
     */
    public FilterResult applyColumnFilter(final DataTableSpec spec) {
        return m_columnFilterModel.applyTo(spec);
    }

    /**
     * Returns whether the column names should be added as column headers.
     *
     * @return Whether the column names should be added as column headers
     */
    public boolean addColumnHeader() {
        return m_addColumnHeaderModel.getBooleanValue();
    }

    /**
     * Returns whether the row ids should be added as row headers.
     *
     * @return Whether the row ids should be added as row headers
     */
    public boolean addRowHeader() {
        return m_addRowHeaderModel.getBooleanValue();
    }

    /**
     * Returns whether the data should be written in raw format.
     *
     * @return Whether the data should be written in raw format
     */
    public boolean writeRaw() {
        return m_writeRawModel.getBooleanValue();
    }

    /**
     * Returns whether the spreadsheet should be opened after execution.
     *
     * @return Whether the spreadsheet should be opened after execution
     */
    public boolean openAfterExecution() {
        return m_openAfterExecutionModel.getBooleanValue();
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
     * Returns the {@link SettingsModelBoolean} for the spreadsheet opening after execution.
     *
     * @return The {@link SettingsModelBoolean} for the spreadsheet opening after execution
     */
    protected static SettingsModelBoolean getOpenAfterExecutionModel() {
        return new SettingsModelBoolean("openAfterExecution", false);
    }

    /**
     * Saves all the settings. Should be called from the {@link NodeModel}.
     *
     * @param settings The {@link NodeSettingsWO}
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_addColumnHeaderModel.saveSettingsTo(settings);
        m_addRowHeaderModel.saveSettingsTo(settings);
        m_writeRawModel.saveSettingsTo(settings);
        m_columnFilterModel.saveSettingsTo(settings);
        m_openAfterExecutionModel.saveSettingsTo(settings);
        m_missingValueModel.saveSettingsTo(settings);
    }

    /**
     * Validates all the settings. Should be called from the {@link NodeModel}.
     *
     * @param settings The {@link NodeSettingsRO}
     * @throws InvalidSettingsException If the settings are invalid
     */
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_addColumnHeaderModel.validateSettings(settings);
        m_addRowHeaderModel.validateSettings(settings);
        m_writeRawModel.validateSettings(settings);
        m_columnFilterModel.validateSettings(settings);
        m_openAfterExecutionModel.validateSettings(settings);
        m_missingValueModel.validateSettings(settings);
        }

    /**
     * Loads all validated settings. Should be called from the {@link NodeModel}.
     *
     * @param settings The {@link NodeSettingsRO}
     * @throws InvalidSettingsException If the settings are invalid
     */
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_addColumnHeaderModel.loadSettingsFrom(settings);
        m_addRowHeaderModel.loadSettingsFrom(settings);
        m_writeRawModel.loadSettingsFrom(settings);
        m_columnFilterModel.loadSettingsFrom(settings);
        m_openAfterExecutionModel.loadSettingsFrom(settings);
        m_missingValueModel.loadSettingsFrom(settings);
    }
}
