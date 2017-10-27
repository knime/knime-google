/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   Oct 4, 2017 (oole): created
 */
package org.knime.google.api.sheets.nodes.reader;

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelOptionalString;
import org.knime.core.node.util.CheckUtils;
import org.knime.google.api.sheets.nodes.util.SettingsModelGoogleSpreadsheetChooser;

/**
 * Settings for the {@link GoogleSheetsReaderModel}.
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
final class GoogleSheetsReaderSettings {

    private SettingsModelBoolean m_readColNameModel = new SettingsModelBoolean("readColName", true);

    private SettingsModelBoolean m_readRowIdModel = new SettingsModelBoolean("readRowId", true);

    private SettingsModelGoogleSpreadsheetChooser m_spreadsheetSheetChoserModel =
            new SettingsModelGoogleSpreadsheetChooser("spreadsheet");

    private SettingsModelOptionalString m_readRangeModel = new SettingsModelOptionalString("readRange", "", false);


    protected SettingsModelOptionalString getReadRangeModel() {
        return m_readRangeModel;
    }

    protected SettingsModelBoolean getReadColNameModel() {
        return m_readColNameModel;
    }

    protected SettingsModelBoolean getReadRowIdModel() {
        return m_readRowIdModel;
    }

    protected SettingsModelGoogleSpreadsheetChooser getSpreadsheetChoserModel() {
        return m_spreadsheetSheetChoserModel;
    }

    protected String getSpreadSheetId() {
        return StringUtils.trim(m_spreadsheetSheetChoserModel.getSpreadsheetId());
    }

    protected String getSheetName() {
        return StringUtils.trim(m_spreadsheetSheetChoserModel.getSheetName());
    }

    protected boolean readRowId() {
        return m_readRowIdModel.getBooleanValue();
    }

    protected boolean readColName() {
        return m_readColNameModel.getBooleanValue();
    }

    private boolean useCustomRange() {
        return m_readRangeModel.isActive();
    }

    protected String getRange() {
        String range =
            getSheetName() + (useCustomRange() ? "!" + StringUtils.trim(m_readRangeModel.getStringValue()) : "");
        return range;
    }


    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_readColNameModel.saveSettingsTo(settings);
        m_readRowIdModel.saveSettingsTo(settings);
        m_spreadsheetSheetChoserModel.saveSettingsTo(settings);
        m_readRangeModel.saveSettingsTo(settings);
    }

    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_readColNameModel.validateSettings(settings);
        m_readRowIdModel.validateSettings(settings);
        m_spreadsheetSheetChoserModel.validateSettings(settings);

        SettingsModelOptionalString rangeModelClone = m_readRangeModel.createCloneWithValidatedValue(settings);
        CheckUtils.checkSetting(!rangeModelClone.isActive()
            || StringUtils.isNotEmpty(rangeModelClone.getStringValue()), "No range defined");
    }

    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_readColNameModel.loadSettingsFrom(settings);
        m_readRowIdModel.loadSettingsFrom(settings);
        m_spreadsheetSheetChoserModel.loadSettingsFrom(settings);
        m_readRangeModel.loadSettingsFrom(settings);
    }

}
