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
package org.knime.google.api.sheets.nodes.util;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.DesktopUtil;
import org.knime.core.util.SwingWorkerWithContext;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;

/**
 * A {@link DialogComponent} that allows to choose from existing Google spreadsheets.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class DialogComponentGoogleSpreadsheetChooser extends DialogComponent {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentGoogleSpreadsheetChooser.class);

    private final JLabel m_spreadsheetLabel = new JLabel("Spreadsheet: ");
    private JButton m_spreadsheetSelectButton;
    private final JTextField m_spreadsheetNameField = createTextField();

    private JButton m_openSpreadsheetInBrowserButton;

    private Drive m_driveService;
    private Sheets m_sheetsService;

    private String m_spreadsheetId = "";

    private final JPanel m_panelWithSelectOrProgressBar = new JPanel(new GridBagLayout());

    private JTextField createTextField() {
        final JTextField textField = new JTextField(20);
        textField.setEditable(false);
        return textField;
    }

    /**
     * Constructor.
     *
     * @param model The settings model for this component
     */
    public DialogComponentGoogleSpreadsheetChooser (final SettingsModelGoogleSpreadsheetChooser model) {
        super(model);
        m_spreadsheetSelectButton = getSelectButton();
        m_openSpreadsheetInBrowserButton = getOpenSpreedSheetInBrowserButton();
    }

    private JButton getOpenSpreedSheetInBrowserButton() {
        JButton button = new JButton("Open in Browser...");
        button.addActionListener(e -> onOpenSpreadsheetInBrowserButtonPressed());
        return button;
    }

    private JButton getSelectButton() {
        final JButton button = new JButton("Select...");
        button.addActionListener(e -> onSelectButtonPressed());
        return button;
    }

    private class SpreadsheetOptionPane {
        private JList<File> m_list;
        private JLabel m_label;
        private JOptionPane m_optionPane;
        private JButton m_okButton, m_cancelButton;
        private JDialog m_dialog;
        private boolean m_okay;

        public SpreadsheetOptionPane(final Window rootWindow,final String title, final String message, final JList<File> listToDisplay){
            m_list = listToDisplay;
            m_list.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        m_okay = true;
                        hide();
                    }
                  }
            }) ;
            m_label = new JLabel(message);
            createAndDisplayOptionPane(rootWindow);
            m_dialog.setTitle(title);
        }

        private void createAndDisplayOptionPane(final Window rootWindow){
            setupButtons();
            JPanel pane = layoutComponents();
            m_optionPane = new JOptionPane(pane);
            m_optionPane.setComponentOrientation(rootWindow.getComponentOrientation());
            m_optionPane.setOptions(new Object[]{m_okButton, m_cancelButton});
            m_dialog = m_optionPane.createDialog(rootWindow,"Select option");
        }

        private void setupButtons(){
            m_okButton = new JButton("Ok");
            m_okButton.addActionListener(e -> handleOkButtonClick(e));

            m_cancelButton = new JButton("Cancel");
            m_cancelButton.addActionListener(e -> handleCancelButtonClick(e));
        }

        private JPanel layoutComponents(){
            JPanel panel = new JPanel(new GridBagLayout());
            JScrollPane scroll = new JScrollPane(m_list);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5);
            gbc.anchor = GridBagConstraints.NORTHWEST;
            panel.add(m_label, gbc);
            gbc.gridx++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(scroll, gbc);
            return panel;
        }

        private void handleOkButtonClick(final ActionEvent e){
            m_okay = true;
            hide();
        }

        private void handleCancelButtonClick(final ActionEvent e){
            m_okay = false;
            hide();
        }

        public File show(){
            m_dialog.setVisible(true);
            m_dialog.dispose();
            File file = null;
            if (m_okay) {
                file = m_list.getSelectedValue();
            }
            return file;
        }

        private void hide(){ m_dialog.setVisible(false); }
    }

    private class SpreadsheetCellListRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 256016396008249780L;

        @Override
        public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof File) {
                File spreadsheet = (File) value;
                setText(spreadsheet.getName());
                setToolTipText("Id: " + spreadsheet.getId());
            }
            return this;
        }
    }


    /**
     * Loads the settings and sets the sheet and drive service needed for the components functionality.
     *
     * This should be called in the Node dialog.
     *
     * @param settings The settings to load from
     * @param specs The specs to load from
     * @param driveService The drive service
     * @param sheetService The sheet service
     * @throws NotConfigurableException If the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs, final Drive driveService, final Sheets sheetService) throws NotConfigurableException {
        setServices(driveService, sheetService);
        super.loadSettingsFrom(settings, specs);
    }

    /**
     * Set the Google Drive and Google Sheet service that should be used in the dialog component.
     * This function should be called during leadSettings in the node dialog.
     *
     * @param driveService The Google Drive service that should be used for the dialog component
     * @param sheetService The Google Sheet service that should be used for the dialog component
     */
    private void setServices(final Drive driveService, final Sheets sheetService) {
        m_driveService = driveService;
        m_sheetsService = sheetService;
    }

    @Override
    public JPanel getComponentPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 1;
        panel.add(m_spreadsheetLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_spreadsheetNameField, gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        m_panelWithSelectOrProgressBar.add(getSelectPanel());
        gbc.gridheight = 2;
        panel.add(m_panelWithSelectOrProgressBar, gbc);
        final JPanel additionalPanel = getAdditionalSelectionPanel();
        if (additionalPanel != null) {
            gbc.gridheight = 1;
            gbc.gridwidth = 2;
            gbc.gridy++;
            gbc.gridx = 0;
            gbc.weightx = 1;

            panel.add(additionalPanel, gbc);
        }
        return panel;
    }

    /**
     * Returns an additional panel to be placed under the spreadsheet selection.
     *
     * (E.g. A sheet selection panel)
     *
     * @return Additional panel for the spreadsheet selection
     */
    protected JPanel getAdditionalSelectionPanel() {
        return null;
    }

    private JPanel getSelectPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.ipady = 0;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel.add(m_spreadsheetSelectButton, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(m_openSpreadsheetInBrowserButton, gbc);
        return panel;
    }

    private final void onOpenSpreadsheetInBrowserButtonPressed() {
        SwingWorkerWithContext<URL, Void> retrieveSpreadsheetUrlSwingWorker = new SwingWorkerWithContext<URL, Void>() {

            @Override
            protected URL doInBackgroundWithContext() throws Exception {
                String spreadsheetUrl =
                    m_sheetsService.spreadsheets().get(m_spreadsheetId).execute().getSpreadsheetUrl();
                return new URL(spreadsheetUrl);
            }

            @Override
            protected void doneWithContext() {
                try {
                    DesktopUtil.browse(get());
                } catch (InterruptedException | ExecutionException e) {
                    String error = "Could not retrieve Spreadsheet URL";
                    LOGGER.debug(error, e);
                    JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(m_spreadsheetLabel),
                        "Could not retrieve Spreadsheet URL");
                }

            }
        };
        retrieveSpreadsheetUrlSwingWorker.execute();
    }

    private final void onSelectButtonPressed() {
        SwingWorkerWithContext<File[], Void> retrieveSpreadsheetsSwingWorker =
                new SwingWorkerWithContext<File[], Void>() {

            @Override
            protected File[] doInBackgroundWithContext() throws Exception {
                File[] spreadsheets = getSpreadsheets();
                return spreadsheets;
            }

            @Override
            protected void doneWithContext() {
                if (isCancelled()) {
                    return;
                }
                Window frame = SwingUtilities.windowForComponent(m_spreadsheetLabel);
                try {
                    File[] files = get();

                    Optional<File> selectedFile =
                        Arrays.stream(files).filter(file -> file.getId().equals(m_spreadsheetId)).findFirst();

                    JList<File> spreadsheetList = new JList<File>(files);
                    spreadsheetList.setCellRenderer(new SpreadsheetCellListRenderer());
                    spreadsheetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                    setSelectPanelComponent(getSelectPanel());
                    SpreadsheetOptionPane dialog = new SpreadsheetOptionPane(frame, "Select existing spreadsheet",
                        "Select existing spreadsheet: ", spreadsheetList);
                    selectedFile.ifPresent(file -> spreadsheetList.setSelectedValue(file, true));

                    File selectedSpreadSheet = dialog.show();
                    if (selectedSpreadSheet != null) {
                        setSelectedSpreadsheet(selectedSpreadSheet);
                        runAdditionalSelectAction();
                    }
                } catch (InterruptedException e) {
                    // do nothing
                } catch (ExecutionException e) {
                    String error = "Could not retrieve spreadsheets. Check connection.";
                    LOGGER.debug(error, e);
                    JOptionPane.showMessageDialog(frame, error);
                } finally {
                    setSelectPanelComponent(getSelectPanel());
                    try {
                        updateModel();
                    } catch (InvalidSettingsException e) {
                        // Ignore it here
                    }
                }
            }
        };

        retrieveSpreadsheetsSwingWorker.execute();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            NodeLogger.getLogger(DialogComponentGoogleSpreadsheetChooser.class).coding("Ignoring AWT Interrupt");
        }
        if (!retrieveSpreadsheetsSwingWorker.isDone()) {
            JProgressBar b = new JProgressBar();
            b.setIndeterminate(true);
            b.setStringPainted(true);
            b.setString("...");
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> retrieveSpreadsheetsSwingWorker.cancel(true));
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5);
            panel.add(b, gbc);
            gbc.gridx++;
            panel.add(cancelButton, gbc);
            setSelectPanelComponent(panel);
        }
    }

    /**
     * Additional action that can be performed after spreadsheet selection.
     * Can be used when extending this dialog component's functionality.
     */
    protected void runAdditionalSelectAction() {
        // do nothing
    }


    private void setSelectedSpreadsheet(final File selectedSpreadSheet) {
        m_spreadsheetNameField.setText(selectedSpreadSheet.getName());
        m_spreadsheetId = selectedSpreadSheet.getId();
        m_spreadsheetNameField.setToolTipText("Id: " + m_spreadsheetId);
    }

    /**
     * Returns a list of available spreadsheet names and the corresponding spreadsheet id.
     *
     * @return A list of available spreadsheet names and the corresponding spreadsheet id
     * @throws IOException If the spreadsheets cannot be listed
     */
    private File[] getSpreadsheets() throws IOException {

        final List<File> spreadsheets = new ArrayList<File>();
        final com.google.api.services.drive.Drive.Files.List request =
                m_driveService.files().list()
                .setQ("mimeType='application/vnd.google-apps.spreadsheet'");

        do {
            final FileList execute = request.execute();
            spreadsheets.addAll(execute.getFiles());
        } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        return spreadsheets.toArray(new File[spreadsheets.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final SettingsModelGoogleSpreadsheetChooser model = (SettingsModelGoogleSpreadsheetChooser)getModel();
        setEnabledComponents(model.isEnabled());
        m_spreadsheetId = model.getSpreadsheetId();
        m_spreadsheetNameField.setText(model.getSpreadsheetName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        final SettingsModelGoogleSpreadsheetChooser model = (SettingsModelGoogleSpreadsheetChooser)getModel();
        model.setSpreadsheetId(m_spreadsheetId);
        model.setSpreadsheetName(m_spreadsheetNameField.getText());
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
    protected void updateModel() throws InvalidSettingsException {
        SettingsModelGoogleSpreadsheetChooser model = (SettingsModelGoogleSpreadsheetChooser)getModel();
        model.setSpreadsheetId(m_spreadsheetId);
        model.setSpreadsheetName(m_spreadsheetNameField.getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_spreadsheetSelectButton.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_spreadsheetSelectButton.setToolTipText(text);
    }

    private void setSelectPanelComponent(final JComponent comp) {
        if (m_panelWithSelectOrProgressBar.getComponent(0) != comp) {
            m_panelWithSelectOrProgressBar.removeAll();
            m_panelWithSelectOrProgressBar.add(comp);
            m_panelWithSelectOrProgressBar.revalidate();
            m_panelWithSelectOrProgressBar.repaint();
        }
    }

    /**
     * Returns the sheet service used in the dialog component.
     *
     * @return The sheet service used in the dialog component
     */
    protected Sheets getSheetService() {
        return m_sheetsService;
    }

    /**
     * Returns the spreadsheet id that is momentarily used in the dialog component.
     *
     * @return The spreadsheet id momentarily used in the dialog component
     */
    protected String getSpreadSheetId() {
        return m_spreadsheetId;
    }
}
