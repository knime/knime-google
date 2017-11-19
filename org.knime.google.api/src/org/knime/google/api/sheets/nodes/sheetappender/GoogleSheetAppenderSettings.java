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
package org.knime.google.api.sheets.nodes.sheetappender;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.google.api.sheets.nodes.util.AbstractGoogleSheetWriterSettings;
import org.knime.google.api.sheets.nodes.util.SettingsModelGoogleSpreadsheetChooser;

/**
 * The Settings for the {@link GoogleSheetAppenderModel}.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class GoogleSheetAppenderSettings extends AbstractGoogleSheetWriterSettings {
    private SettingsModelGoogleSpreadsheetChooser m_spreadsheetChoserModel = getSpreadsheetChoserModel();

    private SettingsModelString m_sheetNameModel = getSheetnameModel();

    private SettingsModelBoolean m_createUniqueSheetNameModel = getCreateUniqueSheetNameModel();

    /**
     * Returns the {@link SettingsModelBoolean} for the unique sheet name.
     *
     * @return The {@link SettingsModelBoolean} for the unique sheet name
     */
    protected static SettingsModelBoolean getCreateUniqueSheetNameModel() {
        return new SettingsModelBoolean("createUniqueSheetName", true);
    }

    /**
     * Returns the {@link SettingsModelGoogleSpreadsheetChooser} for the spreadsheet name, id.
     *
     * @return The {@link SettingsModelGoogleSpreadsheetChooser} for the spreadsheet name, id
     */
    protected static SettingsModelGoogleSpreadsheetChooser getSpreadsheetChoserModel() {
        return new SettingsModelGoogleSpreadsheetChooser("spreadsheetChooser");
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
     * Returns whether or not a unique sheet name should be used.
     *
     * @return whether or not a unique sheet name should be used
     */
    protected boolean createUniqueSheetName() {
        return m_createUniqueSheetNameModel.getBooleanValue();
    }

    /**
     * Returns the selected spreadsheet name.
     *
     * @return The selected spreadsheet name
     */
    protected String getSpreadsheetName() {
        return m_spreadsheetChoserModel.getSpreadsheetName();
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
     * Returns the selected sheet name.
     *
     * @return The selected sheet name
     */
    protected String getSheetName() {
        return m_sheetNameModel.getStringValue();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_spreadsheetChoserModel.saveSettingsTo(settings);
        m_sheetNameModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_spreadsheetChoserModel.validateSettings(settings);
        m_sheetNameModel.validateSettings(settings);
        }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_spreadsheetChoserModel.loadSettingsFrom(settings);
        m_sheetNameModel.loadSettingsFrom(settings);
    }
}
