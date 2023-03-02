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
 *   16 Jun 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.connector;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.Duration;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog component for a timeout based on a duration settings model.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class DialogComponentTimeout extends DialogComponent {

    private final JLabel m_label;

    private final JSpinner m_seconds;

    private SpinnerNumberModel m_model;

    private static final int MIN_TIMEOUT = 0;
    private static final int MAX_TIMEOUT = Integer.MAX_VALUE;

    DialogComponentTimeout(final SettingsModelDuration settingsModel, final String label) {
        super(settingsModel);
        m_model = new SpinnerNumberModel(settingsModel.getValue().toSeconds(), MIN_TIMEOUT, MAX_TIMEOUT, 1);
        m_seconds = new JSpinner(m_model);
        m_label = new JLabel(label);

        m_seconds.addChangeListener(e -> settingsModel.set(Duration.ofSeconds(m_model.getNumber().intValue())));
        settingsModel.addChangeListener(e -> updateComponent());

        final var panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());

        final var gbc = new GridBagConstraints();
        gbc.ipadx = 5;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_label, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(m_seconds, gbc);
    }

    @Override
    protected void updateComponent() {
        final var settingsModel = (SettingsModelDuration)getModel();
        setEnabledComponents(settingsModel.isEnabled());
        final var currSettings = settingsModel.getValue().toSeconds();
        final var currModel = ((Number)m_model.getValue()).intValue();
        if (currSettings == currModel) {
            return;
        }
        m_model.setValue(currSettings);
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        final var seconds = ((SettingsModelDuration)getModel()).getValue().toSeconds();
        if (seconds < MIN_TIMEOUT || seconds > MAX_TIMEOUT) {
            getComponentPanel().setBorder(BorderFactory.createLineBorder(Color.RED));
            throw new InvalidSettingsException(
                String.format("Timeout of \"%d\" seconds is outside of allowed range: [%d, %d].",
                    seconds, MIN_TIMEOUT, MAX_TIMEOUT));
        }
        getComponentPanel().setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // nothing to do
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_seconds.setEnabled(enabled);
        m_label.setEnabled(enabled);
    }

    @Override
    public void setToolTipText(final String text) {
        m_seconds.setToolTipText(text);
    }

}