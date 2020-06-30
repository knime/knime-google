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
 *   2020-03-24 (Alexander Bondaletov): created
 */
package org.knime.ext.google.filehandling.cloudstorage.node;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFSConnection;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;
import org.knime.google.api.data.GoogleApiConnection;
import org.knime.google.api.data.GoogleApiConnectionPortObjectSpec;

/**
 * Google Cloud Storage Connection node dialog.
 *
 * @author Alexander Bondaletov
 */
public class CloudStorageConnectorNodeDialog extends NodeDialogPane {

    private final ChangeListener m_workdirListener;
    private final CloudStorageConnectorSettings m_settings = new CloudStorageConnectorSettings();
    private final WorkingDirectoryChooser m_workingDirChooser = new WorkingDirectoryChooser("cloud-storage.workingDir",
            this::createFSConnection);

    private GoogleApiConnection m_connection;


    /**
     * Creates new instance.
     */
    protected CloudStorageConnectorNodeDialog() {
        m_workdirListener = e -> m_settings.getWorkingDirectoryModel()
                .setStringValue(m_workingDirChooser.getSelectedWorkingDirectory());

        addTab("Settings", createSettingsPanel());
        addTab("Advanced", createTimeoutsPanel());
    }

    private JComponent createSettingsPanel() {
        DialogComponentString projectIdInput = new DialogComponentString(m_settings.getProjectIdModel(), "Project ID",
                true, 50);
        projectIdInput.getComponentPanel().setLayout(new FlowLayout(FlowLayout.LEFT));
        projectIdInput.getComponentPanel()
                .setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Google Cloud project settings"),
                        BorderFactory.createEmptyBorder(0, 5, 0, 0)));

        Box box = new Box(BoxLayout.PAGE_AXIS);
        box.add(projectIdInput.getComponentPanel());
        box.add(createFilesystemSettingsPanel());
        return box;
    }

    private JComponent createFilesystemSettingsPanel() {
        DialogComponentBoolean normalizePath = new DialogComponentBoolean(m_settings.getNormalizePathsModel(),
                "Normalize paths");
        normalizePath.getComponentPanel().setLayout(new FlowLayout(FlowLayout.LEFT));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 10, 0, 0);
        panel.add(m_workingDirChooser, c);

        c.gridy += 1;
        c.insets = new Insets(0, 0, 0, 0);
        panel.add(normalizePath.getComponentPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy += 1;
        panel.add(Box.createVerticalGlue(), c);

        panel.setBorder(BorderFactory.createTitledBorder("File system settings"));
        return panel;
    }

    private JComponent createTimeoutsPanel() {
        DialogComponentNumber connectionTimeout = new DialogComponentNumber(m_settings.getConnectionTimeoutModel(), "",
                1);
        DialogComponentNumber readTimeout = new DialogComponentNumber(m_settings.getReadTimeoutModel(), "", 1);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Connection timeout in seconds:"), c);

        c.gridy = 1;
        panel.add(new JLabel("Read timeout in seconds:"), c);

        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(connectionTimeout.getComponentPanel(), c);

        c.gridy = 1;
        panel.add(readTimeout.getComponentPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy += 1;
        panel.add(Box.createVerticalGlue(), c);

        panel.setBorder(BorderFactory.createTitledBorder("Connection settings"));
        return panel;
    }

    private FSConnection createFSConnection() throws IOException {
        return new CloudStorageFSConnection(m_connection, m_settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        validateBeforeSaving();
        m_settings.saveSettingsTo(settings);
    }

    private void validateBeforeSaving() throws InvalidSettingsException {
        m_settings.validate();
        m_workingDirChooser.addCurrentSelectionToHistory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        try {
            m_settings.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // ignore
        }

        m_connection = ((GoogleApiConnectionPortObjectSpec) specs[0]).getGoogleApiConnection();

        settingsLoaded();
    }

    private void settingsLoaded() {
        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectoryModel().getStringValue());
        m_workingDirChooser.addListener(m_workdirListener);
    }

    @Override
    public void onClose() {
        m_workingDirChooser.removeListener(m_workdirListener);
        m_workingDirChooser.onClose();
    }
}
