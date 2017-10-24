/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Sep 27, 2017 (oole): created
 */
package org.knime.google.api.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.google.api.util.SettingsModelCredentialLocation.CredentialLocationType;

/**
 * Dialog component for Google Authentication nodes.
 *
 * The Dialog component provides two options, either Default (in-node) authentication or Custom, so that the user can
 * set the location for the key files received during the authentication process.
 *
 * It is intended to be used in dialogs of interactive authentication nodes,
 * such as {@link GoogleSheetsInteractiveServiceProviderDialog}
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
public class DialogComponentCredentialLocation extends DialogComponent implements ActionListener {

    private static final Insets NEUTRAL_INSET = new Insets(0, 0, 0, 0);
    private static final int LEFT_INSET = 23;

    private final ButtonGroup m_locationType = new ButtonGroup();

    private final JRadioButton m_typeDefault;
    private final JRadioButton m_typeCustom;

    private final JTextField m_userIdField = new JTextField(20);
    private final DialogComponentFileChooser m_credentialLocationComponent;

    private final Component m_customPanel;


    private final String m_label = "Credential Location";

    private JPanel m_rootPanel;


    private JRadioButton createLocationButton(final CredentialLocationType type,
        final ButtonGroup group, final ActionListener l) {
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
     * Constructor.
     *
     * @param model The settings model for this dialog
     * @param historyID The history id for this component
     */
    public DialogComponentCredentialLocation(final SettingsModelCredentialLocation model, final String historyID) {
        super(model);
        m_credentialLocationComponent =
                new DialogComponentFileChooser(model,
                    historyID,
                    JFileChooser.OPEN_DIALOG, true, "");
        m_customPanel = getCustomPanel();
        m_typeDefault = createLocationButton(CredentialLocationType.DEFAULT, m_locationType, this);
        m_typeCustom = createLocationButton(CredentialLocationType.CUSTOM, m_locationType, this);

        m_rootPanel = getRootPanel();

        getComponentPanel().setLayout(new GridBagLayout());
        getComponentPanel().add(m_rootPanel);

        m_userIdField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                updateModel();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                updateModel();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                updateModel();
            }
        });
    }

    private JPanel getRootPanel() {
        final JPanel credentialBox = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = NEUTRAL_INSET;
        credentialBox.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " " + m_label + " "));
        credentialBox.add(m_typeDefault, gbc);
        gbc.gridy++;
        credentialBox.add(m_typeCustom, gbc);
        gbc.gridy++;
        credentialBox.add(m_customPanel, gbc);

        final Dimension origSize = credentialBox.getPreferredSize();
        final Dimension preferredSize = m_customPanel.getPreferredSize();
        final Dimension maxSize = getMaxDim(preferredSize, origSize);
        credentialBox.setMinimumSize(maxSize);
        credentialBox.setPreferredSize(maxSize);
        return credentialBox;
    }

    private Dimension getMaxDim(final Dimension d1, final Dimension d2) {
        return new Dimension(Math.max(d1.width, d2.width), Math.max(d1.height, d2.height));
    }

    private JPanel getCustomPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        panel.add(new JLabel("User id: "), gbc);
        gbc.gridx = 1;
        gbc.insets = NEUTRAL_INSET;
        gbc.ipadx = 10;
        panel.add(m_userIdField, gbc);
        gbc.ipadx = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        panel.add(m_credentialLocationComponent.getComponentPanel(), gbc);
        return panel;
    }

    private void updateModel() {
        final CredentialLocationType type = CredentialLocationType.get(m_locationType.getSelection().getActionCommand());
        String userId = null;
        switch (type) {
            case DEFAULT:
                break;
            case CUSTOM:
                userId = m_userIdField.getText();
                break;
            default:
                throw new IllegalStateException("Unimplemented authentication type found");
        }
        final SettingsModelCredentialLocation model = (SettingsModelCredentialLocation)getModel();
        model.setValues(type, userId);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final SettingsModelCredentialLocation model = (SettingsModelCredentialLocation)getModel();
        setEnabledComponents(model.isEnabled());

        final Enumeration<AbstractButton> buttons = m_locationType.getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand().equals(model.getCredentialLocationType().getActionCommand())) {
                button.setSelected(true);
            }
        }

        if (model.getCredentialLocationType().equals(CredentialLocationType.CUSTOM)) {
            if (!m_userIdField.getText().equals(model.getUserId())) {
                updateNoListener(m_userIdField, model.getUserId());
            }
        }
        updatePanel();
    }

    private void updatePanel() {
        m_customPanel.setVisible(m_typeCustom.isSelected());
    }

    private static void updateNoListener(final JTextField txtField, final String text) {
        final AbstractDocument doc = (AbstractDocument)txtField.getDocument();
        DocumentListener[] listeners = doc.getDocumentListeners();
        for (DocumentListener listener : listeners) {
            doc.removeDocumentListener(listener);
        }
        txtField.setText(text);
        for (DocumentListener listener : listeners) {
            doc.addDocumentListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // No worries
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_userIdField.setEnabled(enabled);
        m_typeDefault.setEnabled(enabled);
        m_typeCustom.setEnabled(enabled);
        m_credentialLocationComponent.getModel().setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        // No worries
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        updateModel();
        updatePanel();
    }

}
