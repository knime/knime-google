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
 *   Mar 19, 2014 ("Patrick Winter"): created
 */
package org.knime.google.api.analytics.nodes.connector;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.google.api.analytics.data.GoogleAnalyticsConnection;
import org.knime.google.api.credential.CredentialUtil;

import com.google.auth.oauth2.OAuth2Credentials;

/**
 * The dialog to the GoogleAnalyticsConnector node.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("cast")
public class GoogleAnalyticsConnectorDialog extends NodeDialogPane {

    private Map<String, Map<String, Map<String, String>>> m_map =
            new TreeMap<String, Map<String, Map<String, String>>>();

    private JComboBox<String> m_accounts;

    private DefaultComboBoxModel<String> m_accountsModel;

    private JComboBox<String> m_webproperties;

    private DefaultComboBoxModel<String> m_webpropertiesModel;

    private JComboBox<String> m_profiles;

    private DefaultComboBoxModel<String> m_profilesModel;

    private JTextField m_profileId;

    private JLabel m_warning;

    private JPanel m_selectionsPanel;

    private final ConnectionTimeoutPanel m_connectionTimeoutPanel = new ConnectionTimeoutPanel();

    /**
     * Constructor creating the dialogs content.
     */
    public GoogleAnalyticsConnectorDialog() {
        m_warning = new JLabel("Warning: Could not connect to the Google API");
        m_warning.setForeground(Color.RED);
        m_accountsModel = new DefaultComboBoxModel<String>();
        m_accounts = new JComboBox<String>(m_accountsModel);
        m_webpropertiesModel = new DefaultComboBoxModel<String>();
        m_webproperties = new JComboBox<String>(m_webpropertiesModel);
        m_profilesModel = new DefaultComboBoxModel<String>();
        m_profiles = new JComboBox<String>(m_profilesModel);
        m_profileId = new JTextField();
        m_accounts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (!m_map.isEmpty() && m_accountsModel.getSize() > 0 && isValidSelection(m_accounts)) {
                    m_webpropertiesModel.removeAllElements();
                    Set<String> webproperties = m_map.get(m_accounts.getSelectedItem()).keySet();
                    for (String webproperty : webproperties) {
                        m_webpropertiesModel.addElement(webproperty);
                    }
                    if (webproperties.isEmpty()) {
                        m_profileId.setText("");
                    }
                }
            }
        });
        m_webproperties.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (!m_map.isEmpty() && m_webpropertiesModel.getSize() > 0 && isValidSelection(m_accounts)
                        && isValidSelection(m_webproperties)) {
                    m_profilesModel.removeAllElements();
                    Set<String> profiles =
                            m_map.get(m_accounts.getSelectedItem())
                                    .get(m_webproperties.getSelectedItem()).keySet();
                    for (String profile : profiles) {
                        m_profilesModel.addElement(profile);
                    }
                    if (profiles.isEmpty()) {
                        m_profileId.setText("");
                    }
                }
            }
        });
        m_profiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                String profileId = getSelectedProfileId();
                if (profileId != null) {
                    m_profileId.setText(profileId);
                }
            }
        });
        m_profileId.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(final DocumentEvent e) {
                invalidateSelection();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                invalidateSelection();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                invalidateSelection();
            }
        });
        m_selectionsPanel = new JPanel();
        m_selectionsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        gbc2.anchor = GridBagConstraints.NORTHWEST;
        gbc2.fill = GridBagConstraints.BOTH;
        gbc2.weightx = 1;
        gbc2.weighty = 0;
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        m_selectionsPanel.add(new JLabel("Account:"), gbc2);
        gbc2.gridy++;
        m_selectionsPanel.add(m_accounts, gbc2);
        gbc2.gridy++;
        m_selectionsPanel.add(new JLabel("Webproperty:"), gbc2);
        gbc2.gridy++;
        m_selectionsPanel.add(m_webproperties, gbc2);
        gbc2.gridy++;
        m_selectionsPanel.add(new JLabel("Profile:"), gbc2);
        gbc2.gridy++;
        m_selectionsPanel.add(m_profiles, gbc2);
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_warning, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(m_selectionsPanel, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel("Profile ID:"), gbc);
        gbc.gridy++;
        panel.add(m_profileId, gbc);
        addTab("Settings", panel);
        addTab("Advanced Settings", createAdvancedTab());
    }

    private JPanel createAdvancedTab() {
        final JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(m_connectionTimeoutPanel);
        container.add(Box.createHorizontalGlue());
        return container;
    }

    private void invalidateSelection() {
        if (!"".equals(m_profileId.getText()) && !m_profileId.getText().equals(getSelectedProfileId())) {
            m_accountsModel.setSelectedItem("");
            m_webpropertiesModel.setSelectedItem("");
            m_profilesModel.setSelectedItem("");
        }
    }

    private String getSelectedProfileId() {
        try {
            return m_map.get(m_accounts.getSelectedItem()).get(m_webproperties.getSelectedItem())
                    .get(m_profiles.getSelectedItem());
        } catch (Throwable e) {
            return null;
        }
    }

    private boolean isValidSelection(final JComboBox<String> comboBox) {
        return comboBox.getSelectedItem() != null && !((String)comboBox.getSelectedItem()).isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        GoogleAnalyticsConnectorConfiguration config = new GoogleAnalyticsConnectorConfiguration();
        config.setProfileId(m_profileId.getText());
        config.setConnectTimeout(m_connectionTimeoutPanel.getSelectedConnectionTimeout());
        config.setReadTimeout(m_connectionTimeoutPanel.getSelectedReadTimeout());
        config.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        if (specs[0] == null) {
            throw new NotConfigurableException("Missing Google API Connection");
        }
        final OAuth2Credentials creds;
        try {
            creds = CredentialUtil.toOAuth2Credentials(GoogleAnalyticsConnectorModel.getCredentialRef(specs[0]));
        } catch (NoSuchCredentialException | IOException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }

        GoogleAnalyticsConnectorConfiguration config = new GoogleAnalyticsConnectorConfiguration();
        config.loadInDialog(settings);
        Duration connectTimeout =
            config.getConnectTimeout().orElse(GoogleAnalyticsConnectorConfiguration.DEFAULT_CONNECT_TIMEOUT);
        Duration readTimeout =
            config.getReadTimeout().orElse(GoogleAnalyticsConnectorConfiguration.DEFAULT_READ_TIMEOUT);
        m_connectionTimeoutPanel.setSelectedConnectionTimeout(connectTimeout);
        m_connectionTimeoutPanel.setSelectedReadTimeout(readTimeout);
        try {
            if (m_map.isEmpty()) {
                m_map =
                        GoogleAnalyticsConnection.getAccountsWebpropertiesProfilesMap(
                            creds, //
                            connectTimeout, readTimeout
                        );
                m_warning.setVisible(false);
                m_selectionsPanel.setVisible(true);
            }
        } catch (IOException e) {
            m_warning.setVisible(true);
            m_selectionsPanel.setVisible(false);
        }
        m_accountsModel.removeAllElements();
        for (String account : m_map.keySet()) {
            m_accountsModel.addElement(account);
        }
        for (String account : m_map.keySet()) {
            for (String webproperty : m_map.get(account).keySet()) {
                for (String profile : m_map.get(account).get(webproperty).keySet()) {
                    if (m_map.get(account).get(webproperty).get(profile).equals(config.getProfileId())) {
                        m_accountsModel.setSelectedItem(account);
                        m_webpropertiesModel.setSelectedItem(webproperty);
                        m_profilesModel.setSelectedItem(profile);
                    }
                }
            }
        }
        if (!config.getProfileId().isEmpty()) {
            m_profileId.setText(config.getProfileId());
        }
    }

}
