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
final class GoogleSheetUpdaterSettings extends AbstractGoogleSheetWriterSettings{

    private SettingsModelGoogleSpreadsheetAndSheetChooser m_spreadsheetChoserModel =
            new SettingsModelGoogleSpreadsheetAndSheetChooser("spreadsheetChooser");

    private SettingsModelOptionalString m_rangeModel =
            new SettingsModelOptionalString("range", "", false);

    private SettingsModelBoolean m_appendModel;

    private SettingsModelBoolean m_clearSheetModel;

    private boolean changedByAction = false;

    GoogleSheetUpdaterSettings() {
        m_appendModel = new SettingsModelBoolean("append", false);
        m_clearSheetModel = new SettingsModelBoolean("clearSheet", false);
        m_appendModel.addChangeListener(e -> {
            if (!changedByAction) {
                changedByAction = true;
                m_clearSheetModel.setEnabled(!m_appendModel.getBooleanValue());
                m_rangeModel.setEnabled(!m_appendModel.getBooleanValue());
                changedByAction = false;
            }
        });
        m_clearSheetModel.addChangeListener(e -> {
            if (!changedByAction) {
                changedByAction = true;
                m_appendModel.setEnabled(!m_clearSheetModel.getBooleanValue() && !m_rangeModel.isActive());
                changedByAction = false;
            }
        });
        m_rangeModel.addChangeListener(e -> {
            if (!changedByAction) {
                changedByAction = true;
                m_appendModel.setEnabled(!m_clearSheetModel.getBooleanValue() && !m_rangeModel.isActive());
                changedByAction = false;
            }
        });

    }



    /**
     * Returns the {@link SettingsModelOptionalString} for the range.
     *
     * @return The {@link SettingsModelOptionalString} for the range
     */
    SettingsModelOptionalString getRangeModel() {
        return m_rangeModel;
    }

    /**
     * Returns the {@link SettingsModelBoolean} for appending.
     *
     * @return The {@link SettingsModelBoolean} for appending
     */
    SettingsModelBoolean getAppendModel() {
        return m_appendModel;
    }

    /**
     * Returns the {@link SettingsModelBoolean} for the sheet clearing before writing.
     *
     * @return The {@link SettingsModelBoolean} for the sheet clearing before writing
     */
    SettingsModelBoolean getClearSheetModel() {
        return m_clearSheetModel;
    }

    /**
     * Returns the {@link SettingsModelGoogleSpreadsheetAndSheetChooser} for the spreadsheet and sheet.
     *
     * @return The {@link SettingsModelGoogleSpreadsheetAndSheetChooser} for the spreadsheet and sheet
     */
    SettingsModelGoogleSpreadsheetAndSheetChooser getSpreadsheetChoserModel() {
        return m_spreadsheetChoserModel;
    }

    /**
     * Returns the selected spreadsheet's id.
     *
     * @return The selected spreadsheet's id
     */
    String getSpreadsheetId() {
        return m_spreadsheetChoserModel.getSpreadsheetId();
    }

    /**
     * Returns the selected spreadsheet's name.
     *
     * @return The selected spreadsheet's name
     */
    String getSpreadsheetName() {
        return m_spreadsheetChoserModel.getSpreadsheetName();
    }

    /**
     * Returns the selected sheet name.
     *
     * @return The selected sheet name
     */
    String getSheetName() {
        return m_spreadsheetChoserModel.getSheetName();
    }

    /**
     * @return whether the first sheet should always be selected.
     */
    boolean selectFirstSheet() {
        return m_spreadsheetChoserModel.getSelectFirstSheet();
    }

    /**
     * Returns whether of not the restricted range should be used.
     *
     * @return Whether or not the restricted range should be used
     */
    boolean useRange() {
        return m_rangeModel.isActive();
    }

    /**
     * Returns the restricted range.
     *
     * @return The restricted range
     */
    String getRange() {
        return m_rangeModel.getStringValue().trim();
    }

    /**
     * Returns whether or not the data should be appended to the existing sheet.
     *
     * @return Whether or not the data should be appended to the existing sheet
     */
    boolean append() {
        return m_appendModel.getBooleanValue();
    }

    /**
     * Returns whether or not the sheet should be cleared before writing.
     *
     * @return Whether or not the sheet should be cleared before writing
     */
    boolean clearSheet() {
        return m_clearSheetModel.getBooleanValue();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_spreadsheetChoserModel.saveSettingsTo(settings);
        m_rangeModel.saveSettingsTo(settings);
        m_appendModel.saveSettingsTo(settings);
        m_clearSheetModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_spreadsheetChoserModel.validateSettings(settings);
        m_rangeModel.validateSettings(settings);
        m_appendModel.validateSettings(settings);
        m_clearSheetModel.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_spreadsheetChoserModel.loadSettingsFrom(settings);
        m_rangeModel.loadSettingsFrom(settings);
        m_appendModel.loadSettingsFrom(settings);
        m_clearSheetModel.loadSettingsFrom(settings);
    }
}
