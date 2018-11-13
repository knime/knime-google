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
package org.knime.google.api.nodes.connector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.workflow.FlowVariable;

/**
 * The dialog to the GoogleApiConnector node.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class GoogleApiConnectorDialog extends NodeDialogPane {

    private JTextField m_serviceAccountEmail;

    private FilesHistoryPanel m_keyFileLocation;

    private JTextArea m_scopes;

    /**
     * Constructor creating the dialogs content.
     */
    public GoogleApiConnectorDialog() {
        m_serviceAccountEmail = new JTextField();
        FlowVariableModel fvm = createFlowVariableModel("key_file_location", FlowVariable.Type.STRING);
        m_keyFileLocation = new FilesHistoryPanel(fvm, "google_api_key_file_location", false);
        m_scopes = new JTextArea();
        m_scopes.setRows(5);
        final JComboBox<String> knownScopes =
                new JComboBox<String>(GoogleApiKnownScopes.MAP.keySet().toArray(new String[0]));
        JButton addKnownScope = new JButton("Add");
        addKnownScope.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                String selection = (String)knownScopes.getSelectedItem();
                String text = m_scopes.getText();
                boolean addNewLine = !text.isEmpty() && !text.endsWith("\n");
                String addedScope = GoogleApiKnownScopes.MAP.get(selection);
                text += (addNewLine ? "\n" : "") + addedScope + "\n";
                m_scopes.setText(text);
            }
        });
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
        panel.add(new JLabel("Service account email:"), gbc);
        gbc.gridy++;
        panel.add(m_serviceAccountEmail, gbc);
        gbc.gridy++;
        panel.add(new JLabel("P12 key file location:"), gbc);
        gbc.gridy++;
        panel.add(m_keyFileLocation, gbc);
        gbc.gridy++;
        panel.add(new JLabel("Scopes:"), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(new JScrollPane(m_scopes), gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        JPanel knownScopesPanel = new JPanel();
        knownScopesPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        gbc2.anchor = GridBagConstraints.NORTHWEST;
        gbc2.fill = GridBagConstraints.BOTH;
        gbc2.weightx = 1;
        gbc2.weighty = 0;
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        knownScopesPanel.add(knownScopes, gbc2);
        gbc2.weightx = 0;
        gbc2.gridx++;
        knownScopesPanel.add(addKnownScope, gbc2);
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(knownScopesPanel, gbc);
        addTab("Settings", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        GoogleApiConnectorConfiguration config = new GoogleApiConnectorConfiguration();
        config.setServiceAccountEmail(m_serviceAccountEmail.getText());
        config.setKeyFileLocation(m_keyFileLocation.getSelectedFile());
        config.setScopes(m_scopes.getText().trim().split("\n"));
        config.save(settings);
        m_keyFileLocation.addToHistory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        GoogleApiConnectorConfiguration config = new GoogleApiConnectorConfiguration();
        config.loadInDialog(settings);
        m_serviceAccountEmail.setText(config.getServiceAccountEmail());
        m_keyFileLocation.setSelectedFile(config.getKeyFileLocation());
        m_scopes.setText(StringUtils.join(config.getScopes()));
    }

}
