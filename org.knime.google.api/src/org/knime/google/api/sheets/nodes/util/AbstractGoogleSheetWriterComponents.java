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
package org.knime.google.api.sheets.nodes.util;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentOptionalString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.google.api.sheets.nodes.sheetappender.GoogleSheetAppenderModel;
import org.knime.google.api.sheets.nodes.sheetupdater.GoogleSheetUpdaterModel;
import org.knime.google.api.sheets.nodes.spreadsheetwriter.GoogleSpreadsheetWriterModel;

/**
 * Abstract class holding the default components for the google sheets writer nodes.
 * Such as {@link GoogleSpreadsheetWriterModel}, {@link GoogleSheetUpdaterModel}, {@link GoogleSheetAppenderModel}.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractGoogleSheetWriterComponents {

    private final DialogComponentBoolean m_addColumnHeader =
            new DialogComponentBoolean(AbstractGoogleSheetWriterSettings.getAddColumnHeaderModel(), "Add column header");

    private final DialogComponentBoolean m_addRowHeader =
            new DialogComponentBoolean(AbstractGoogleSheetWriterSettings.getAddRowHeaderModel(), "Add row header");

    private final DialogComponentBoolean m_openAfterExecution =
            new DialogComponentBoolean(AbstractGoogleSheetWriterSettings.getOpenAfterExecutionModel(),
                "Open spreadsheet after execution");

    private final DialogComponentColumnFilter2 m_columnFilter =
            new DialogComponentColumnFilter2(AbstractGoogleSheetWriterSettings.getColumnFilterModel(),
                AbstractGoogleSheetWriterSettings.DATA_PORT);

    private final DialogComponentOptionalString m_missingValue =
            new DialogComponentOptionalString(
                AbstractGoogleSheetWriterSettings.getMissingValueModel(), "For missing values write: ");

    private final DialogComponentBoolean m_writeRaw =
            new DialogComponentBoolean(AbstractGoogleSheetWriterSettings.getWriteRawModel(), "Write raw");

    /**
     * Returns the panel containing the google sheets writer components.
     *
     * @return The panel containing the google sheets writer components
     */
    public JPanel getPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = getDefaultGBC();
        panel.add(getSpreadsheetPanel(), gbc);
        gbc.gridy++;
        panel.add(getWriteSettingsPanel(),gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_openAfterExecution.getComponentPanel(), gbc);
        gbc.gridy++;
        panel.add(m_columnFilter.getComponentPanel(), gbc);
        return panel;
    }

    private JPanel getWriteSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Write Settings "));
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = getDefaultGBC();
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_addColumnHeader.getComponentPanel(), gbc);
        gbc.gridy++;
        panel.add(m_addRowHeader.getComponentPanel(), gbc);
        gbc.gridy++;
        panel.add(m_missingValue.getComponentPanel(), gbc);
        panel = addWriterComponents(panel, gbc);
        return panel;
    }

    /**
     * Abstract function, which adds additional components to the writer section of the google sheets writer panel.
     *
     * @param panel The parent panel
     * @param gbc The parent panel's {@link GridBagConstraints}
     * @return The given parent panel with additional components.
     */
    protected abstract JPanel addWriterComponents(JPanel panel, GridBagConstraints gbc);

    /**
     * Abstract function which adds components for the spreadsheet selection/entering
     * section of the google sheets writer panel.
     *
     * @return The panel containing the components for the spreadsheet selection/entering.
     */
    protected abstract JPanel getSpreadsheetPanel();

    /**
     * Returns the default {@link GridBagConstraints} for the sections of the google sheets writer panel.
     *
     * @return The default {@link GridBagConstraints} for the sections of the google sheets writer panel
     */
    protected GridBagConstraints getDefaultGBC() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        return gbc;
    }

    /**
     * Save all settings. Should be called from the {@link NodeDialog}.
     *
     * @param settings The settings to save to
     * @throws InvalidSettingsException If the settings are invalid
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_addColumnHeader.saveSettingsTo(settings);
        m_addRowHeader.saveSettingsTo(settings);
        m_columnFilter.saveSettingsTo(settings);
        m_openAfterExecution.saveSettingsTo(settings);
        m_missingValue.saveSettingsTo(settings);
        m_writeRaw.saveSettingsTo(settings);
    }

    /**
     * Loads all settings. Should be called from the {@link NodeDialog}.
     *
     * @param settings The settings to read from
     * @param specs The specs to load from
     * @throws NotConfigurableException If the settings are invalid
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        m_addColumnHeader.loadSettingsFrom(settings, specs);
        m_addRowHeader.loadSettingsFrom(settings, specs);
        m_columnFilter.loadSettingsFrom(settings, specs);
        m_openAfterExecution.loadSettingsFrom(settings, specs);
        m_missingValue.loadSettingsFrom(settings, specs);
        m_writeRaw.loadSettingsFrom(settings, specs);
    }
}
