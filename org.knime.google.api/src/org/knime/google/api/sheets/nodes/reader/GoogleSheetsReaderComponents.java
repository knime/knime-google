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
 *   Oct 4, 2017 (oole): created
 */
package org.knime.google.api.sheets.nodes.reader;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentOptionalString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.google.api.sheets.data.GoogleSheetsConnectionPortObjectSpec;
import org.knime.google.api.sheets.nodes.util.DialogComponentGoogleSpreadsheetChooser;

/**
 * Components for the {@link GoogleSheetsReaderDialog}.
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
final class GoogleSheetsReaderComponents {

    private final GoogleSheetsReaderSettings m_settings;

    private final DialogComponentBoolean m_readColName;

    private final DialogComponentBoolean m_readRowId;

    private final DialogComponentGoogleSpreadsheetChooser m_spreadsheetChooser;

    private final DialogComponentOptionalString m_readRange;

    GoogleSheetsReaderComponents(final GoogleSheetsReaderSettings settings) {
        m_settings = settings;
        m_readColName = new DialogComponentBoolean(m_settings.getReadColNameModel(), "Has Column Header");
        m_readRowId = new DialogComponentBoolean(m_settings.getReadRowIdModel(), "Has Row Header");
        m_spreadsheetChooser = new DialogComponentGoogleSpreadsheetChooser(m_settings.getSpreadsheetChoserModel());
        m_readRange = new DialogComponentOptionalString(m_settings.getReadRangeModel(), "Range:            ");

    }

    protected JPanel getPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_spreadsheetChooser.getComponentPanel(), gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
        panel.add(m_readRange.getComponentPanel(), gbc);
        gbc.gridy++;
        panel.add(m_readColName.getComponentPanel(), gbc);
        gbc.gridy++;
        panel.add(m_readRowId.getComponentPanel(), gbc);
        return panel;
    }

    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_readColName.saveSettingsTo(settings);
        m_readRowId.saveSettingsTo(settings);
        m_spreadsheetChooser.saveSettingsTo(settings);
        m_readRange.saveSettingsTo(settings);
    }

    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        if (specs[0] == null) {
            throw new NotConfigurableException("Missing Google Sheets Connection");
        }
        GoogleSheetsConnectionPortObjectSpec connectionSpec = (GoogleSheetsConnectionPortObjectSpec)specs[0];
        try {
            m_spreadsheetChooser.setServices(connectionSpec.getGoogleSheetsConnection().getDriveService(),
                connectionSpec.getGoogleSheetsConnection().getSheetsService());
        } catch (IOException e) {
            throw new NotConfigurableException("Invalid Google Sheets Connection");
        }
        m_spreadsheetChooser.loadSettingsFrom(settings, specs);

        m_readColName.loadSettingsFrom(settings, specs);
        m_readRowId.loadSettingsFrom(settings, specs);
        m_readRange.loadSettingsFrom(settings, specs);
    }

}
