/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 2, 2018 (oole): created
 */
package org.knime.google.api.nodes.authconnector;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.google.api.GooglePlugInActivator;
import org.knime.google.api.nodes.authconnector.auth.GoogleAuthLocationType;
import org.knime.google.api.nodes.authconnector.auth.GoogleAuthentication;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScope;
import org.knime.google.api.nodes.authconnector.util.KnimeGoogleAuthScopeRegistry;

import com.google.api.client.auth.oauth2.Credential;

/**
 * Dialog for the Google Authentication node.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
final class GoogleAuthNodeDialogPane extends NodeDialogPane {

    /** Feedback from an exchange with the "Google Cloud Trust & Safety Team":
     * > Please update the Google sign-in button so that it complies with the Google sign-in branding guidelines.
     * (which also means, we not only are allowed but also required to include the Google icon in our product.
     */
    private static final ImageIcon GOOGLE_SIGN_IN_ICON = GooglePlugInActivator
        .getResource("/icons/btn_google_dark_normal_ios.png").map(ImageIcon::new).orElse(null);

    private static final Insets NEUTRAL_INSET = new Insets(0, 0, 0, 0);

    private static final int LEFT_INSET = 23;

    private static final Insets PANEL_INSET = new Insets(10, LEFT_INSET, 10, LEFT_INSET);

    private static final String ADVANCED_TAB_TITLE = "Advanced";

    private JList<CheckboxListItem> m_scopeList;

    private JCheckBox m_selectAllScopes;

    private final JPanel m_panelWithAuthButtonOrProgressBar = new JPanel(new GridBagLayout());

    private JButton m_authenticateButton;

    private JLabel m_authenticationState = new JLabel("Unknown");

    private final ButtonGroup m_buttonGroup = new ButtonGroup();

    private JRadioButton m_memoryLocationButton;

    private JRadioButton m_nodeLocationButton;

    private JRadioButton m_fileSystemLocationButton;

    private JPanel m_fileSystemLocationPanel;

    private final GoogleAuthNodeSettings m_settings = new GoogleAuthNodeSettings();

    private boolean m_isAuthenticated = false;

    private boolean m_scopeChanged = false;

    private final DialogComponentFileChooser m_credentialFileLocation =
        new DialogComponentFileChooser(m_settings.getCredentialFileLocationModel(),
            GoogleAuthNodeDialogPane.class.getCanonicalName(), JFileChooser.OPEN_DIALOG, true, "");

    private final DialogComponentBoolean m_useCustomClientId =
        new DialogComponentBoolean(m_settings.getUseCustomClientIdModel(), "Use custom OAuth Client ID");

    private final DialogComponentFileChooser m_customClientIdFile =
        new DialogComponentFileChooser(m_settings.getCustomClientIdFileModel(), "google-oauth-clientid", ".json");


    /**
     * Constructor creating the dialogs content.
     */
    GoogleAuthNodeDialogPane() {
        addTab("Authentication", getAuthPanel());
        addTab(ADVANCED_TAB_TITLE, createAdvancedTab());
    }

    private JPanel getAuthPanel() {
        JPanel panel = createBasePanel();
        GridBagConstraints gbc = getDefaultGBC();
        gbc.insets = new Insets(20, 20, 20, 20);
        JPanel innerPanel = createBasePanel();
        panel.add(innerPanel, gbc);
        gbc = getDefaultGBC();
        gbc.insets = new Insets(10, 5, 10, 5);
        // Authentication button and status message:
        m_authenticateButton = new JButton("Sign in with Google", GOOGLE_SIGN_IN_ICON);
        m_authenticateButton.addActionListener(e -> onAuthButtonPressed());
        m_panelWithAuthButtonOrProgressBar.add(m_authenticateButton);
        innerPanel.add(m_panelWithAuthButtonOrProgressBar, gbc);
        gbc.gridx++;
        JLabel statusLabel = new JLabel("Status: ");
        innerPanel.add(statusLabel, gbc);
        gbc.gridx++;
        gbc.gridx++;
        innerPanel.add(m_authenticationState, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;

        // Credential Location panel
        innerPanel.add(getCredentialLocationPanel(), gbc);
        gbc.gridy++;

        JPanel scopeHostPanel = new JPanel(new GridBagLayout());
        scopeHostPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Scopes "));
        scopeHostPanel.add(getScopePanel(), getDefaultGBC());
        innerPanel.add(scopeHostPanel, gbc);
        return panel;
    }

    /**
     * Opens auth window and waits for it to close, has the ability to be canceled by the user. Also will show some nice
     * progress bar while doing what it does.
     */
    private final void onAuthButtonPressed() {

        SwingWorkerWithContext<Credential, Void> openBrowserSwingWorker =
            new SwingWorkerWithContext<Credential, Void>() {

                @Override
                protected Credential doInBackgroundWithContext() throws Exception {
                    if (m_scopeChanged) {
                        clearCredentials();
                        m_scopeChanged = false;
                    }
                    m_settings.validate();
                    Credential credential = GoogleAuthentication.getCredential(m_settings.getCredentialLocationType(),
                        m_settings.getCredentialLocation(), m_settings.getRelevantKnimeAuthScopes(), m_settings.getClientIdFile());
                    m_settings.setAccessTokenHash(credential.getAccessToken().hashCode());
                    return credential;
                }

                @Override
                protected void doneWithContext() {
                    try {
                        if (isCancelled()) {
                            m_authenticationState.setText("Cancelled");
                            return;
                        }
                        // Credential not used here.
                        get();
                        m_settings.setByteString();
                        m_isAuthenticated = true;
                    } catch (InterruptedException e) { // NOSONAR
                        m_authenticationState.setText("Unknown");
                    } catch (IOException | URISyntaxException | ExecutionException e) {
                        m_authenticationState.setText("Not Authenticated");
                        JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(m_authenticateButton),
                            "Authentication failed: " + ExceptionUtils.getRootCauseMessage(e));
                    } finally {
                        setAuthPanelComponent(m_authenticateButton);
                        setAuthenticationStatus();
                    }
                }
            };
        openBrowserSwingWorker.execute();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            NodeLogger.getLogger(GoogleAuthNodeDialogPane.class).coding("Ignoring AWT Interrupt");
        }
        if (!openBrowserSwingWorker.isDone()) {
            JProgressBar b = new JProgressBar();
            b.setIndeterminate(true);
            b.setStringPainted(true);
            b.setString(" Auth in browser... ");
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> openBrowserSwingWorker.cancel(true));
            JPanel waitPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = getDefaultGBC();
            waitPanel.add(b, gbc);
            gbc.insets = new Insets(0, 5, 0, 5);
            gbc.gridx++;
            waitPanel.add(cancelButton, gbc);
            setAuthPanelComponent(waitPanel);
        }
    }

    private List<KnimeGoogleAuthScope> getSelectedKnimeScopes() {
        List<KnimeGoogleAuthScope> scopeList = new ArrayList<>();
        ListModel<CheckboxListItem> model = m_scopeList.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            CheckboxListItem elementAt = model.getElementAt(i);
            if (elementAt.isSelected()) {
                scopeList.add(elementAt.m_scope);
            }
        }
        return scopeList;
    }

    private void setAuthPanelComponent(final JComponent comp) {
        if (m_panelWithAuthButtonOrProgressBar.getComponent(0) != comp) {
            m_panelWithAuthButtonOrProgressBar.removeAll();
            m_panelWithAuthButtonOrProgressBar.add(comp);
            m_panelWithAuthButtonOrProgressBar.revalidate();
            m_panelWithAuthButtonOrProgressBar.repaint();
        }
    }

    /**
     * Creates a radio button for the given credential location type, belonging to the given button group with the given
     * action listener.
     *
     * @param type The credential location type
     * @param group The button group
     * @param l The action listener
     * @return A radio button for the given credential location type, belonging to the given button group with the given
     *         action listener
     */
    private static JRadioButton createLocationButton(final GoogleAuthLocationType type, final ButtonGroup group,
        final ActionListener l) {
        String buttonLabel = type.getText();
        String toolTip = type.getToolTip();

        final JRadioButton button = new JRadioButton(buttonLabel);
        button.setActionCommand(type.getActionCommand());
        if (type.isDefault()) {
            button.setSelected(true);
        }
        if (type.getToolTip() != null) {
            button.setToolTipText(toolTip);
        }
        if (l != null) {
            button.addActionListener(l);
        }
        group.add(button);
        return button;
    }

    /**
     * Returns the credential location panel.
     *
     * @return The credential location panel
     */
    private Component getCredentialLocationPanel() {
        JPanel credentialPanel = new JPanel(new GridBagLayout());
        credentialPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Credential "));
        final GridBagConstraints gbc = getDefaultGBC();
        gbc.insets = PANEL_INSET;

        JPanel selectionPanel = new JPanel(new GridBagLayout());
        selectionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Location "));
        credentialPanel.add(selectionPanel, gbc);

        ActionListener actionListener = e -> {
            updateCredentialLocationPanel();
            updateModel();
        };

        final GridBagConstraints innerGBC = getDefaultGBC();
        m_memoryLocationButton = createLocationButton(GoogleAuthLocationType.MEMORY, m_buttonGroup, actionListener);
        selectionPanel.add(m_memoryLocationButton, innerGBC);
        innerGBC.gridy++;

        m_fileSystemLocationButton =
            createLocationButton(GoogleAuthLocationType.FILESYSTEM, m_buttonGroup, actionListener);
        selectionPanel.add(m_fileSystemLocationButton, innerGBC);
        innerGBC.gridy++;
        m_fileSystemLocationPanel = getFileSystemLocationPanel();
        selectionPanel.add(m_fileSystemLocationPanel, innerGBC);
        innerGBC.gridy++;

        m_nodeLocationButton = createLocationButton(GoogleAuthLocationType.NODE, m_buttonGroup, actionListener);
        selectionPanel.add(m_nodeLocationButton, innerGBC);

        gbc.gridy++;

        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(10, 100, 10, 100);

        JButton button = new JButton("Clear Selected Credentials");
        button.addActionListener(e -> clearCredentials());
        credentialPanel.add(button, gbc);

        final Dimension originalSize = selectionPanel.getPreferredSize();
        final Dimension elementSize = m_fileSystemLocationPanel.getPreferredSize();
        final Dimension maxSize = getMaxDim(elementSize, originalSize);
        selectionPanel.setMinimumSize(maxSize);
        selectionPanel.setPreferredSize(maxSize);

        return credentialPanel;
    }

    /** Helper to figure out max size for credential location panel **/
    private static Dimension getMaxDim(final Dimension d1, final Dimension d2) {
        return new Dimension(Math.max(d1.width, d2.width), Math.max(d1.height, d2.height));
    }

    private JPanel getFileSystemLocationPanel() {
        JPanel panel = createBasePanel();
        GridBagConstraints gbc = getDefaultGBC();
        gbc.insets = new Insets(10, LEFT_INSET, 10, 10);
        panel.add(m_credentialFileLocation.getComponentPanel(), gbc);
        return panel;
    }

    private void clearCredentials() {
        try {
            if (m_settings.getCredentialLocationType() == GoogleAuthLocationType.NODE) {
                m_settings.removeInNodeCredentials();
            } else {
                GoogleAuthentication.removeCredential(m_settings.getCredentialLocationType(),
                    m_settings.getCredentialLocation());
            }
            m_isAuthenticated = false;
            m_settings.setAccessTokenHash("".hashCode());
            setAuthenticationStatus();
        } catch (IOException | InvalidSettingsException e) { // NOSONAR Ignore: During credential clearing, this can mean that maybe the file got deleted manually.
        }
    }

    private void updateCredentialLocationPanel() {
        m_fileSystemLocationPanel.setVisible(m_fileSystemLocationButton.isSelected());
    }

    private JPanel getScopePanel() {
        JPanel panel = createBasePanel();
        GridBagConstraints gbc = getDefaultGBC();
        gbc.insets = PANEL_INSET;
        List<KnimeGoogleAuthScope> knimeGoogleAuthScopes =
            KnimeGoogleAuthScopeRegistry.getInstance().getOAuthEnabledKnimeGoogleAuthScopes();
        DefaultListModel<CheckboxListItem> model = new DefaultListModel<>();
        for (KnimeGoogleAuthScope scope : knimeGoogleAuthScopes) {
            model.addElement(new CheckboxListItem(scope));
        }

        m_scopeList = new JList<>(model);
        m_scopeList.setCellRenderer(new CheckboxListCellRenderer());
        m_scopeList.setOpaque(false);

        m_scopeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent event) {
                @SuppressWarnings("unchecked")
                JList<CheckboxListItem> elementList = (JList<CheckboxListItem>)event.getSource();

                // Get index of item clicked
                int index = elementList.locationToIndex(event.getPoint());
                CheckboxListItem item = elementList.getModel().getElementAt(index);

                // Toggle selected state
                item.setSelected(!item.isSelected());
                m_scopeChanged = true;
                updateModel();

                // Repaint cell
                elementList.repaint(elementList.getCellBounds(index, index));
            }
        });

        panel.add(m_scopeList, gbc);
        gbc.gridy++;
        gbc.weighty++;
        m_selectAllScopes = new JCheckBox("All scopes");
        m_selectAllScopes.addActionListener(e -> {
            m_scopeChanged = true;
            updateModel();
            updateDialog();
        });
        panel.add(m_selectAllScopes, gbc);
        return panel;
    }

    private JComponent createAdvancedTab() {

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = getDefaultGBC();
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;

        panel.add(m_useCustomClientId.getComponentPanel(), c);

        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(m_customClientIdFile.getComponentPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy++;
        panel.add(Box.createVerticalGlue(), c);

        return panel;
    }

    private static JPanel createBasePanel() {
        return new JPanel(new GridBagLayout());
    }

    private static GridBagConstraints getDefaultGBC() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = NEUTRAL_INSET;
        return gbc;
    }

    private static class CheckboxListCellRenderer extends JCheckBox implements ListCellRenderer<CheckboxListItem> {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("rawtypes")
        @Override
        public Component getListCellRendererComponent(final JList list, final CheckboxListItem value, final int index,
            final boolean isSelected, final boolean cellHasFocus) {

            setComponentOrientation(list.getComponentOrientation());
            setFont(list.getFont());
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            setOpaque(list.isOpaque());
            setSelected(value.m_isSelected);

            setEnabled(list.isEnabled());

            setText(value.toString());
            setToolTipText(value.getToolTip());

            return this;
        }
    }

    private static class CheckboxListItem {
        private boolean m_isSelected = false;

        private KnimeGoogleAuthScope m_scope;

        public CheckboxListItem(final KnimeGoogleAuthScope scope) {
            m_scope = scope;
        }

        public void setSelected(final boolean set) {
            m_isSelected = set;
        }

        public boolean isSelected() {
            return m_isSelected;
        }

        @Override
        public String toString() {
            return m_scope.getAuthScopeName();
        }

        public String getToolTip() {
            return m_scope.getDescription();
        }
    }

    private void updateModel() {
        m_settings.setKnimeGoogleAuthScopes(getSelectedKnimeScopes());
        m_settings
            .setCredentialLocationType(GoogleAuthLocationType.get(m_buttonGroup.getSelection().getActionCommand()));
        m_settings.setAllScopes(m_selectAllScopes.isSelected());
        m_settings.setIsAuthenticated(m_isAuthenticated);
    }

    private void updateDialog() {
        Enumeration<AbstractButton> elements = m_buttonGroup.getElements();
        while (elements.hasMoreElements()) {
            final AbstractButton button = elements.nextElement();
            if (button.getActionCommand().equals(m_settings.getCredentialLocationType().getActionCommand())) {
                button.setSelected(true);
            }
        }

        updateCredentialLocationPanel();

        m_selectAllScopes.setSelected(m_settings.useAllScopes());
        m_scopeList.setEnabled(!m_settings.useAllScopes());

        List<KnimeGoogleAuthScope> knimeAuthScopes = m_settings.getSelectedKnimeAuthScopes();
        ListModel<CheckboxListItem> model = m_scopeList.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            CheckboxListItem item = model.getElementAt(i);
            Optional<Boolean> candidate = knimeAuthScopes.stream()
                .map(k -> k.getAuthScopeName().equals(item.toString())).filter(r -> r.equals(true)).findFirst();
            if (candidate.orElse(false)) {
                item.setSelected(true);
            }
        }

        m_customClientIdFile.setEnabled(m_settings.getUseCustomClientIdModel().getBooleanValue());
    }

    private void setAuthenticationStatus() {
        if (m_isAuthenticated) {
            m_authenticationState.setText("Authenticated");
        } else {
            m_authenticationState.setText("Not authenticated");
        }
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            m_settings.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) { // NOSONAR okay
        }

        // Authentication status is unknown when opening the dialog.
        m_authenticationState.setText("Unknown");
        updateDialog();
    }
}