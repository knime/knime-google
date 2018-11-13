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
 *   Sep 5, 2017 (oole): created
 */
package org.knime.google.api.sheets.nodes.connectorinteractive;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Observer;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.google.api.sheets.data.GoogleSheetsInteractiveAuthentication;
import org.knime.google.api.util.DialogComponentCredentialLocation;
import org.knime.google.api.util.SettingsModelCredentialLocation;
import org.knime.google.api.util.SettingsModelCredentialLocation.CredentialLocationType;

import com.google.api.services.sheets.v4.Sheets;

/**
 * The components for the {@link GoogleSheetsInteractiveServiceProviderModel}.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
@Deprecated
final class GoogleInteractiveServiceProviderComponents {

    private final GoogleInteractiveServiceProviderSettings m_settings;

    private final JPanel m_panelWithAuthButtonOrProgressBar = new JPanel(new GridBagLayout());
    private final JButton m_authTestButton = new JButton("(Re-)Authentication");

    private DialogComponentCredentialLocation m_credentialLocationComponent;

    /**
     * Constructor.
     *
     * @param settings The settings for the dialog components
     */
    public GoogleInteractiveServiceProviderComponents(final GoogleInteractiveServiceProviderSettings settings) {
        m_settings = settings;
    }

    /**
     * Creates and returns {@link DialogComponentFileChooser} for the credential storage location.
     *
     * @return The component for the credential storage location
     */
    DialogComponentCredentialLocation createCredentialLocationComponent() {
        Action removeDefaultCredentialsAction = new AbstractAction("Forget Default Credentials") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_settings.removeInNodeCredentials();
                CredentialLocationType credentialLocationType = ((SettingsModelCredentialLocation)
                        m_credentialLocationComponent.getModel()).getCredentialLocationType();
                if (credentialLocationType.equals(CredentialLocationType.DEFAULT)) {
                    m_authTestButton.setBackground(Color.yellow);
                }
            }
        };
        Observer observer = (observable, updateObject) -> {
            removeDefaultCredentialsAction.setEnabled(
                m_settings.inNodeCredential() &&
                m_settings.getEncodedStoredCredential() != null);
        };
        observer.update(null, null); // call the lamda above to update enable-state
        m_settings.addObserver(observer);
        m_credentialLocationComponent = new DialogComponentCredentialLocation(m_settings.getCredentialLocationModel(),
            GoogleSheetsInteractiveServiceProviderModel.class.getCanonicalName(), removeDefaultCredentialsAction);
        m_credentialLocationComponent.getModel().addChangeListener(e -> m_authTestButton.setBackground(Color.YELLOW));
        return m_credentialLocationComponent;
    }

    /**
     * Returns the {@link JPanel} containing the dialog components
     *
     * @return The Panel containing the dialog components
     */
    JPanel getPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(createCredentialLocationComponent().getComponentPanel(), gbc);
        gbc.gridy++;
        m_authTestButton.setBackground(Color.yellow);
        m_authTestButton.addActionListener(e -> onAuthButtonPressed());
        m_panelWithAuthButtonOrProgressBar.add(m_authTestButton);
        gbc.insets = new Insets(5, 10, 5, 5);
        gbc.gridwidth = 2;
        panel.add(m_panelWithAuthButtonOrProgressBar, gbc);
        return panel;
    }

    /** Validates the Service and opens the browser window for authentication if necessary. It does all sorts of
     * SwingWorker voodoo to handle the event that the user doesn't enter anything in the browser window.
     */
    private final void onAuthButtonPressed() {

        SwingWorkerWithContext<Sheets, Void> openBrowserSwingWorker = new SwingWorkerWithContext<Sheets, Void>() {

            @Override
            protected Sheets doInBackgroundWithContext() throws Exception {
                return GoogleSheetsInteractiveAuthentication.getAuthRenewedSheetsService(m_settings.getLocationType(),
                    m_settings.getCredentialLocation(), m_settings.getUserString());
            }

            @Override
            protected void doneWithContext() {
                try {
                    if (isCancelled()) {
                        m_authTestButton.setBackground(Color.yellow);
                        return;
                    }
                    get();
                    m_settings.setByteString();
                    m_authTestButton.setBackground(Color.green);
                } catch (InterruptedException e) {
                    m_authTestButton.setBackground(Color.yellow);
                } catch (URISyntaxException | IOException | ExecutionException e) {
                    m_authTestButton.setBackground(Color.red);
                    JOptionPane.showMessageDialog(
                        SwingUtilities.windowForComponent(m_authTestButton),
                        "Authentication failed: " + ExceptionUtils.getRootCauseMessage(e));
                } finally {
                    setAuthPanelComponent(m_authTestButton);
                }
            }
        };
        openBrowserSwingWorker.execute();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            NodeLogger.getLogger(GoogleInteractiveServiceProviderComponents.class).coding("Ignoring AWT Interrupt");
        }
        if (!openBrowserSwingWorker.isDone()) {
            JProgressBar b = new JProgressBar();
            b.setIndeterminate(true);
            b.setStringPainted(true);
            b.setString("    Authenticating in browser window...   ");
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> openBrowserSwingWorker.cancel(true));
            setAuthPanelComponent(ViewUtils.getInFlowLayout(b, cancelButton));
        }

    }

    /**
     *
     * This method has to be called in {@link NodeDialogPane}.
     *
     * @param settings The node settings.
     * @param specs The specs of inputs.
     * @throws NotConfigurableException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_credentialLocationComponent.loadSettingsFrom(settings, specs);
        m_authTestButton.setBackground(Color.yellow);
        try {
            m_settings.loadAuth(settings);
        } catch (InvalidSettingsException e) {
            // Not authenticated
        }
    }

    /**
     * This method has to be called in the {@link NodeDialogPane}.
     *
     * @param settings The node settings.
     * @throws InvalidSettingsException If the settings are invalid
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_credentialLocationComponent.saveSettingsTo(settings);
        m_settings.saveAuth(settings);
    }

    private void setAuthPanelComponent(final JComponent comp) {
        if (m_panelWithAuthButtonOrProgressBar.getComponent(0) != comp) {
            m_panelWithAuthButtonOrProgressBar.removeAll();
            m_panelWithAuthButtonOrProgressBar.add(comp);
            m_panelWithAuthButtonOrProgressBar.revalidate();
            m_panelWithAuthButtonOrProgressBar.repaint();
        }
    }

}
