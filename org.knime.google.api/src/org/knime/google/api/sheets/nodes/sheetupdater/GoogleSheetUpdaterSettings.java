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
package org.knime.google.api.sheets.nodes.sheetupdater;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelOptionalString;
import org.knime.google.api.sheets.nodes.util.AbstractGoogleSheetWriterSettings;
import org.knime.google.api.sheets.nodes.util.SettingsModelGoogleSpreadsheetAndSheetChooser;

/**
 * The settings for the {@link GoogleSheetUpdaterModel}.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class GoogleSheetUpdaterSettings extends AbstractGoogleSheetWriterSettings{

    private SettingsModelGoogleSpreadsheetAndSheetChooser m_spreadsheetChoserModel = getSpreadsheetChoserModel();

    private SettingsModelOptionalString m_rangeModel = getRangeModel();

    private SettingsModelBoolean m_appendModel = getAppendModel();

    /**
     * Returns the {@link SettingsModelBoolean} for appending.
     *
     * @return The {@link SettingsModelBoolean} for appending
     */
    protected static SettingsModelBoolean getAppendModel() {
        return new SettingsModelBoolean("append", false);
    }


    /**
     * Returns the {@link SettingsModelOptionalString} for the range.
     *
     * @return The {@link SettingsModelOptionalString} for the range
     */
    protected static SettingsModelOptionalString getRangeModel() {
        return new SettingsModelOptionalString("range", "", false);
    }

    /**
     * Returns the {@link SettingsModelGoogleSpreadsheetAndSheetChooser} for the spreadsheet and sheet.
     *
     * @return The {@link SettingsModelGoogleSpreadsheetAndSheetChooser} for the spreadsheet and sheet
     */
    protected static SettingsModelGoogleSpreadsheetAndSheetChooser getSpreadsheetChoserModel() {
        return new SettingsModelGoogleSpreadsheetAndSheetChooser("spreadsheetChooser");
    }

    /**
     * Returns the selected spreadsheet's id.
     *
     * @return The selected spreadsheet's id
     */
    protected String getSpreadsheetId() {
        return m_spreadsheetChoserModel.getSpreadsheetId();
    }

    /**
     * Returns the selected spreadsheet's name.
     *
     * @return The selected spreadsheet's name
     */
    protected String getSpreadsheetName() {
        return m_spreadsheetChoserModel.getSpreadsheetName();
    }

     * Returns the selected sheet name.
     *
     * @return The selected sheet name
     */
    protected String getSheetName() {
        return m_spreadsheetChoserModel.getSheetName();
    }

    /**
     * Returns whether of not the restricted range should be used.
     *
     * @return Whether or not the restricted range should be used
     */
    protected boolean useRange() {
        return m_rangeModel.isActive();
    }

    /**
     * Returns the restricted range.
     *
     * @return The restricted range
     */
    protected String getRange() {
        return m_rangeModel.getStringValue().trim();
    }

    /**
     * Returns whether or not the data should be appended to the existing sheet.
     *
     * @return Whether or not the data should be appended to the existing sheet
     */
    protected boolean append() {
        return m_appendModel.getBooleanValue();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_spreadsheetChoserModel.saveSettingsTo(settings);
        m_rangeModel.saveSettingsTo(settings);
        m_appendModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_spreadsheetChoserModel.validateSettings(settings);
        m_rangeModel.validateSettings(settings);
        m_appendModel.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_spreadsheetChoserModel.loadSettingsFrom(settings);
        m_rangeModel.loadSettingsFrom(settings);
        m_appendModel.loadSettingsFrom(settings);
    }
}
