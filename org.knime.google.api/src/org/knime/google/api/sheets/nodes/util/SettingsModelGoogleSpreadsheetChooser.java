/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *   Oct 11, 2017 (oole): created
 */
package org.knime.google.api.sheets.nodes.util;

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 *  Settings model for the {@link DialogComponentGoogleSpreadsheetChooser}
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
final public class SettingsModelGoogleSpreadsheetChooser extends SettingsModel {

    private static final String SPREADSHEET_ID = "spreadsheetId";
    private static final String SPREADSHEET_NAME = "spreadsheetName";
    private static final String SHEETNAME = "sheetName";
    private static final String SELECT_FIRST_SHEET = "firstSheet";
    private String m_spreadsheetId = "";
    private String m_spreadsheetName = "";
    private String m_sheetName = "";
    private boolean m_selectFirst = false;


    private final String m_configName;


    /**
     * Creates a new object holding a string value.
     *
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     */
    public SettingsModelGoogleSpreadsheetChooser(final String configName) {
        m_configName = configName;
    }

    private SettingsModelGoogleSpreadsheetChooser(final String configName,
        final String spreadsheetName, final String SpreadsheetId, final String sheetName, final boolean selectFirst) {
        m_configName = configName;
        m_spreadsheetName = spreadsheetName;
        m_spreadsheetId = SpreadsheetId;
        m_sheetName = sheetName;
        m_selectFirst = selectFirst;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelGoogleSpreadsheetChooser createClone() {
        return new SettingsModelGoogleSpreadsheetChooser(
            m_configName, m_spreadsheetName, m_spreadsheetId, m_sheetName, m_selectFirst);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_spreadsheetchoser";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            loadSettingsForModel(settings);
        } catch (InvalidSettingsException e) {
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config = settings.getConfig(m_configName);
        CheckUtils.checkSetting(StringUtils.isNotEmpty(config.getString(SPREADSHEET_ID)),
                "Spreadsheet ID must not be empty");
        CheckUtils.checkSetting(StringUtils.isNotEmpty(config.getString(SPREADSHEET_NAME)),
                "Spreadsheet name must not be empty");
        if (!config.getBoolean(SELECT_FIRST_SHEET)) {
            CheckUtils.checkSetting(StringUtils.isNotEmpty(config.getString(SHEETNAME)),
                    "Sheet name must not be empty");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config;
        config = settings.getConfig(m_configName);
        m_spreadsheetId = config.getString(SPREADSHEET_ID);

        m_spreadsheetName = config.getString(SPREADSHEET_NAME);
        m_sheetName = config.getString(SHEETNAME);

        m_selectFirst = config.getBoolean(SELECT_FIRST_SHEET);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        Config config = settings.addConfig(m_configName);
        config.addString(SPREADSHEET_ID, m_spreadsheetId);
        config.addString(SPREADSHEET_NAME, m_spreadsheetName);
        config.addString(SHEETNAME, m_sheetName);
        config.addBoolean(SELECT_FIRST_SHEET, m_selectFirst);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * Returns the spreadsheet id.
     *
     * @return The spreadsheet id
     */
    public String getSpreadsheetId() {
        return m_spreadsheetId;
    }

    /**
     * Returns the spreadsheet name.
     *
     * @return The spreadsheet name
     */
    public String getSpreadsheetName() {
        return m_spreadsheetName;
    }

    /**
     * Returns the spreadsheet name.
     *
     * @return The spreadsheet name
     */
    public String getSheetName() {
        return m_sheetName;
    }

    /**
     * Returns whether or not the first sheet should be selected.
     *
     * @return Whether or not the first sheet should be selected
     */
    public boolean getSelectFirstSheet() {
        return m_selectFirst;
    }

    /**
     * Sets the spreadsheet id.
     *
     * @param spreadsheetId The spreadsheet id to be set
     */
    public void setSpreadsheetId(final String spreadsheetId) {
        m_spreadsheetId = spreadsheetId;
    }


    /**
     * Sets the spreadsheet name.
     *
     * @param spreadsheetName The spreadsheet name to be set
     */
    public void setSpreadsheetName(final String spreadsheetName) {
        m_spreadsheetName = spreadsheetName;
    }

    /**
     * Sets the sheet name.
     *
     * @param sheetName The sheet name to be set
     */
    public void setSheetname(final String sheetName) {
        m_sheetName = sheetName;
    }

    /**
     * Sets whether or not the first sheet should be selected.
     *
     * @param selectFirst Whether or not the first sheet should be selected
     */
    public void setSelectFirstSheet(final boolean selectFirst) {
        m_selectFirst = selectFirst;
    }

}
