package org.knime.google.api.analytics.nodes.connector;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * UI to customise different timeouts used when communicating with a server.
 * @deprecated
 */
@Deprecated(since = "5.4")
public class ConnectionTimeoutPanel extends JPanel {

    JSpinner m_connectionTimeoutSpinner = createSpinner((int)GoogleAnalyticsConnectorConfiguration.DEFAULT_CONNECT_TIMEOUT.getSeconds());
    JSpinner m_readTimeoutSpinner = createSpinner((int)GoogleAnalyticsConnectorConfiguration.DEFAULT_CONNECT_TIMEOUT.getSeconds());

    /**
     * Obtain an instance initialised with default values.
     */
    public ConnectionTimeoutPanel() {
        this.setBorder(BorderFactory.createTitledBorder("Connection timeouts (in seconds)")); // NOSONAR

        this.setLayout(new GridBagLayout()); // NOSONAR

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(5, 0, 5, 5);

        addLabeledSpinner("Connection timeout",
            "Timeout in seconds to establish a connection or 0 for an infinite timeout.",
            m_connectionTimeoutSpinner, gbc);

        addLabeledSpinner("Read timeout",
            "Timeout in seconds to read data from connection or 0 for an infinite timeout.",
            m_readTimeoutSpinner, gbc);

        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int)this.getPreferredSize().getHeight())); // NOSONAR
    }

    private void addLabeledSpinner(final String label, final String tooltip, final JSpinner spinner, final GridBagConstraints gbc) {
        gbc.gridy++;
        gbc.gridx = 0;

        gbc.weightx = 0;
        Box labelBox = Box.createHorizontalBox();
        labelBox.add(new JLabel(label));
        labelBox.add(Box.createHorizontalStrut(10));
        this.add(labelBox, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        this.add(spinner, gbc);

        labelBox.setToolTipText(tooltip);
        spinner.setToolTipText(tooltip);
    }

    private static JSpinner createSpinner(final int value) {
        JSpinner spinner = new JSpinner(
                new SpinnerNumberModel(value, 0, null, 5)
        );
        ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setColumns(4);
        return spinner;
    }

    public Duration getSelectedConnectionTimeout() {
        return getSelectedDurationOf(m_connectionTimeoutSpinner);
    }

    public Duration getSelectedReadTimeout() {
        return getSelectedDurationOf(m_readTimeoutSpinner);
    }

    private static Duration getSelectedDurationOf(final JSpinner spinner) {
        return Duration.ofSeconds(((Number)spinner.getValue()).intValue());
    }

    public void setSelectedConnectionTimeout(final Duration timeout) {
        m_connectionTimeoutSpinner.setValue(timeout.getSeconds());
    }

    public void setSelectedReadTimeout(final Duration timeout) {
        m_readTimeoutSpinner.setValue(timeout.getSeconds());
    }
}
