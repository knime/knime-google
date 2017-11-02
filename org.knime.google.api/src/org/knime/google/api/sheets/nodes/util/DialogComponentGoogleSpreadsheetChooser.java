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
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.DesktopUtil;
import org.knime.core.util.SwingWorkerWithContext;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;

/**
 * A {@link DialogComponent} that allows to choose from existing Google spreadsheets and the available sheets.
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
final public class DialogComponentGoogleSpreadsheetChooser extends DialogComponent {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentGoogleSpreadsheetChooser.class);

    private final JLabel m_spreadsheetLabel = new JLabel("Spreadsheet: ");
    private JButton m_spreadsheetSelectButton;
    private final JTextField m_spreadsheetNameField = createTextField();

    private final JLabel m_sheetLabel = new JLabel("Sheet: ");
    private final JComboBox<String> m_sheetCombobox = createComboBox();

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

    private JComboBox<String> createComboBox() {
        final JComboBox<String> comboBox = new JComboBox<String>(new String[0]);
        comboBox.setEditable(false);
        comboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    updateModel();
                } catch (InvalidSettingsException e1) {
                    // Ignore it here
                }
            }
        });
        return comboBox;
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

        public SpreadsheetOptionPane(final Frame rootFrame,final String title, final String message, final JList<File> listToDisplay){
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
            createAndDisplayOptionPane(rootFrame);
            m_dialog.setTitle(title);
        }

        private void createAndDisplayOptionPane(final Frame rootFrame){
            setupButtons();
            JPanel pane = layoutComponents();
            m_optionPane = new JOptionPane(pane);
            m_optionPane.setComponentOrientation(rootFrame.getComponentOrientation());
            m_optionPane.setOptions(new Object[]{m_okButton, m_cancelButton});
            m_dialog = m_optionPane.createDialog(rootFrame,"Select option");
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
     * Set the Google Drive and Google Sheet service that should be used in the dialog component.
     * This function should be called during leadSettings in the node dialog.
     *
     * @param driveService The Google Drive service that should be used for the dialog component
     * @param sheetService The Google Sheet service that should be used for the dialog component
     */
    public void setServices(final Drive driveService, final Sheets sheetService) {
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
        gbc.gridheight = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(m_sheetLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_sheetCombobox, gbc);
        return panel;
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
        gbc.insets = new Insets(0, 0, 10, 0);
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
                    JOptionPane.showMessageDialog(getParentFrame(), "Could not retrieve Spreadsheet URL");
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
                Frame frame = getParentFrame();
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
                        runRetrieveSheetsSwingWorker(null);
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

    /** Run a SwingWorker that queries and sets the sheets in a selected spreadsheet.
     * @param defaultSheetName The sheet to activate, if present (may be null)
     */
    private void runRetrieveSheetsSwingWorker(final String defaultSheetName) {
        SwingWorkerWithContext<List<String>, Void> retrieveSheetsSwingWorker =
                new SwingWorkerWithContext<List<String>, Void>() {
            @Override
            protected List<String> doInBackgroundWithContext() throws Exception {
                List<String> sheets = getSheets(m_spreadsheetId);
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
                    JOptionPane.showMessageDialog(getParentFrame(), error);
                }
            }
        };
        retrieveSheetsSwingWorker.execute();
    }

    private Frame getParentFrame() {
        Frame frame = null;
        Container container = m_panelWithSelectOrProgressBar.getParent();
        while (container != null) {
            if (container instanceof Frame) {
                frame = (Frame)container;
                break;
            }
            container = container.getParent();
        }
        return frame;
    }

    private void setSelectedSpreadsheet(final File selectedSpreadSheet) {
        m_spreadsheetNameField.setText(selectedSpreadSheet.getName());
        m_spreadsheetId = selectedSpreadSheet.getId();
        m_spreadsheetNameField.setToolTipText("Id: " + m_spreadsheetId);
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
     * Returns a list of available spreadsheet names and the corresponding spreadsheet id.
     *
     * @return A list of available spreadsheet names and the corresponding spreadsheet id
     * @throws IOException If the spreadsheets cannot be listed
     */
    private List<String> getSheets(final String spreadsheetId) throws IOException {
        List<Sheet> sheets = m_sheetsService.spreadsheets().get(spreadsheetId).execute().getSheets();
        List<String> sheetNames = new ArrayList<String>();
        sheets.forEach((sheet) -> sheetNames.add(sheet.getProperties().getTitle()));
        return sheetNames;
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
        if (IntStream.range(0, m_sheetCombobox.getItemCount())
                .mapToObj(i -> m_sheetCombobox.getItemAt(i))
                .anyMatch(s -> s.equals(model.getSheetName()))) {
            m_sheetCombobox.setSelectedItem(model.getSheetName());
        } else {
            if (StringUtils.isNotEmpty(m_spreadsheetId)) {
                runRetrieveSheetsSwingWorker(model.getSheetName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        final SettingsModelGoogleSpreadsheetChooser model = (SettingsModelGoogleSpreadsheetChooser)getModel();
        model.setSpreadsheetId(m_spreadsheetId);
        model.setSpreadsheetName(m_spreadsheetNameField.getText());
        model.setSheetname((String)m_sheetCombobox.getSelectedItem());
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
    private void updateModel() throws InvalidSettingsException {
        SettingsModelGoogleSpreadsheetChooser model = (SettingsModelGoogleSpreadsheetChooser)getModel();
        model.setSpreadsheetId(m_spreadsheetId);
        model.setSpreadsheetName(m_spreadsheetNameField.getText());
        model.setSpreadsheetName((String)m_sheetCombobox.getSelectedItem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_spreadsheetSelectButton.setEnabled(enabled);
        m_spreadsheetNameField.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_spreadsheetSelectButton.setToolTipText(text);
        m_sheetCombobox.setToolTipText(text);
    }

    private void setSelectPanelComponent(final JComponent comp) {
        if (m_panelWithSelectOrProgressBar.getComponent(0) != comp) {
            m_panelWithSelectOrProgressBar.removeAll();
            m_panelWithSelectOrProgressBar.add(comp);
            m_panelWithSelectOrProgressBar.revalidate();
            m_panelWithSelectOrProgressBar.repaint();
        }
    }
}
