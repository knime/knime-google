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
 *   Oct 4, 2017 (oole): created
 */
package org.knime.google.api.sheets.nodes.util;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.credentials.base.NoSuchCredentialException;

import com.google.api.services.sheets.v4.model.Sheet;

/**
 * A {@link DialogComponent} that allows to choose from existing Google spreadsheets and the available sheets.
 *
 * It extends the {@link DialogComponentGoogleSpreadsheetChooser} by being able to select existing sheets.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
final public class DialogComponentGoogleSpreadsheetAndSheetChooser extends DialogComponentGoogleSpreadsheetChooser {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentGoogleSpreadsheetAndSheetChooser.class);


    private final JLabel m_sheetLabel = new JLabel("Sheet:                  ");
    private final JComboBox<String> m_sheetCombobox = createComboBox();

    private JCheckBox m_selectFirstSheet;

    private JComboBox<String> createComboBox() {
        final JComboBox<String> comboBox = new JComboBox<String>(new String[0]);
        comboBox.setEditable(false);
        comboBox.addActionListener(e -> {
            try {
                updateModel();
            } catch (InvalidSettingsException e1) {
                // Ignore it here
            }
        });
        return comboBox;
    }

    private JCheckBox createSelectFirstSheetCheckBox() {
        JCheckBox checkbox = new JCheckBox("Select First Sheet", false);
        checkbox.addActionListener(e -> {
            boolean selectFirst = checkbox.isSelected();
            m_sheetCombobox.setEnabled(!selectFirst);
        });
        return checkbox;
    }

    /**
     * Constructor.
     *
     * @param model The settings model for this component
     */
    public DialogComponentGoogleSpreadsheetAndSheetChooser (final SettingsModelGoogleSpreadsheetAndSheetChooser model) {
        super(model);
        m_selectFirstSheet = createSelectFirstSheetCheckBox();
    }

    @Override
    protected JPanel getAdditionalSelectionPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        panel.add(m_sheetLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.gridwidth =2;
        panel.add(m_sheetCombobox, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 10, 10);
        panel.add(m_selectFirstSheet, gbc);
        return panel;
    }

    @Override
    protected void runAdditionalSelectAction() {
        runRetrieveSheetsSwingWorker(null);
    }

    /** Run a SwingWorker that queries and sets the sheets in a selected spreadsheet.
     * @param defaultSheetName The sheet to activate, if present (may be null)
     */
    private void runRetrieveSheetsSwingWorker(final String defaultSheetName) {
        SwingWorkerWithContext<List<String>, Void> retrieveSheetsSwingWorker =
                new SwingWorkerWithContext<List<String>, Void>() {
            @Override
            protected List<String> doInBackgroundWithContext() throws Exception {
                List<String> sheets = getSheets(getSpreadSheetId());
                return sheets;
            }

            @Override
            protected void doneWithContext() {
                if (isCancelled()) {
                    return;
                }

                try {
                    setAvailableSheets(get());
                    if (defaultSheetName != null) {
                        m_sheetCombobox.setSelectedItem(defaultSheetName);
                    }
                } catch (InterruptedException e) {
                    // do nothing
                } catch (ExecutionException e) {
                    setAvailableSheets(null);
                    String error = "Could not retrieve sheets for spreadsheet";
                    LOGGER.debug(error, e);
                    JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(m_sheetLabel), error);
                }
            }
        };
        retrieveSheetsSwingWorker.execute();
    }

    private void setAvailableSheets(final List<String> sheets) {
        final DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>)m_sheetCombobox.getModel();
        model.removeAllElements();
        // Add new elements
        if (sheets != null) {
            for (final String string : sheets) {
                model.addElement(string);
            }
            m_sheetCombobox.setSelectedIndex(0);
        }
    }

    /**
     * Returns a list of available spreadsheet names and the corresponding spreadsheet id.
     *
     * @return A list of available spreadsheet names and the corresponding spreadsheet id
     * @throws IOException If the spreadsheets cannot be listed
     * @throws NoSuchCredentialException
     */
    private List<String> getSheets(final String spreadsheetId) throws IOException, NoSuchCredentialException {
        List<Sheet> sheets = getConnection().getSheetsService().spreadsheets().get(spreadsheetId).execute().getSheets();
        List<String> sheetNames = new ArrayList<String>();
        sheets.forEach((sheet) -> sheetNames.add(sheet.getProperties().getTitle()));
        return sheetNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        super.updateComponent();
        final SettingsModelGoogleSpreadsheetAndSheetChooser model = (SettingsModelGoogleSpreadsheetAndSheetChooser)getModel();
        m_selectFirstSheet.setSelected(model.getSelectFirstSheet());
        setEnabledComponents(model.isEnabled());
        if (IntStream.range(0, m_sheetCombobox.getItemCount())
                .mapToObj(i -> m_sheetCombobox.getItemAt(i))
                .anyMatch(s -> s.equals(model.getSheetName()))) {
            m_sheetCombobox.setSelectedItem(model.getSheetName());
        } else {
            if (StringUtils.isNotEmpty(getSpreadSheetId())) {
                runRetrieveSheetsSwingWorker(model.getSheetName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        super.validateSettingsBeforeSave();
        final SettingsModelGoogleSpreadsheetAndSheetChooser model = (SettingsModelGoogleSpreadsheetAndSheetChooser)getModel();
        model.setSheetname((String)m_sheetCombobox.getSelectedItem());
        model.setSelectFirstSheet(m_selectFirstSheet.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        //
    }

    /**
     * Transfers the current value from the component into the model.
     *
     * @throws InvalidSettingsException if the string was not accepted.
     */
    @Override
    protected void updateModel() throws InvalidSettingsException {
        super.updateModel();
        SettingsModelGoogleSpreadsheetAndSheetChooser model = (SettingsModelGoogleSpreadsheetAndSheetChooser)getModel();
        model.setSpreadsheetName((String)m_sheetCombobox.getSelectedItem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        super.setEnabledComponents(enabled);
        if (!m_selectFirstSheet.isSelected()) {
            m_sheetCombobox.setEnabled(enabled);
        } else {
            m_sheetCombobox.setEnabled(!enabled);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        super.setToolTipText(text);
        m_sheetCombobox.setToolTipText(text);
        m_selectFirstSheet.setToolTipText(text);
    }
}
