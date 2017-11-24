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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.google.api.sheets.data.GoogleSheetsConnectionPortObjectSpec;
import org.knime.google.api.sheets.nodes.util.AbstractGoogleSheetWriterComponents;
import org.knime.google.api.sheets.nodes.util.DialogComponentGoogleSpreadsheetChooser;

/**
 * The components for the {@link GoogleSheetAppenderModel}.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
final class GoogleSheetAppenderComponents extends AbstractGoogleSheetWriterComponents {

    private final DialogComponentGoogleSpreadsheetChooser m_spreadsheetChoser =
            new DialogComponentGoogleSpreadsheetChooser(GoogleSheetAppenderSettings.getSpreadsheetChoserModel());

    private final DialogComponentString m_sheetName =
            new DialogComponentString(GoogleSheetAppenderSettings.getSheetnameModel(), "Sheet name:      ");


    private final DialogComponentBoolean m_createUniqueSheetName =
            new DialogComponentBoolean(
                GoogleSheetAppenderSettings.getCreateUniqueSheetNameModel(), "Create unique sheet name");

    @Override
    protected JPanel getSpreadsheetPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Spreadsheet Settings "));
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = getDefaultGBC();
        panel.add(m_spreadsheetChoser.getComponentPanel(), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 10, 5, 5);
        panel.add(m_sheetName.getComponentPanel(), gbc);
        return panel;
    }

    @Override
    protected JPanel addWriterComponents(final JPanel panel, final GridBagConstraints gbc) {
        gbc.gridy++;
        panel.add(m_createUniqueSheetName.getComponentPanel(), gbc);
        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        m_spreadsheetChoser.saveSettingsTo(settings);
        m_sheetName.saveSettingsTo(settings);
        m_createUniqueSheetName.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_spreadsheetChoser.loadSettingsFrom(settings, specs);

        if (specs[0] == null) {
            throw new NotConfigurableException("Missing Google Sheets Connection");
        }
        GoogleSheetsConnectionPortObjectSpec connectionSpec = (GoogleSheetsConnectionPortObjectSpec)specs[0];
        try {
            m_spreadsheetChoser.loadSettingsFrom(settings, specs,
                connectionSpec.getGoogleSheetsConnection().getDriveService(),
                connectionSpec.getGoogleSheetsConnection().getSheetsService());
        } catch (IOException e) {
            throw new NotConfigurableException("Invalid Google Sheets Connection");
        }
        m_sheetName.loadSettingsFrom(settings, specs);
        m_createUniqueSheetName.loadSettingsFrom(settings, specs);
    }
}
