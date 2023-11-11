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
 *   2020-10-28 (Vyacheslav Soldatov): created
 */
package org.knime.ext.google.filehandling.drive.node;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.credentials.base.CredentialRef;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFSConnection;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.google.api.credential.CredentialUtil;

/**
 * Google Drive Connection node dialog.
 *
 * @author Vyacheslav Soldatov <vyacheslav@redfield.se>
 */
final class GoogleDriveConnectionNodeDialog extends NodeDialogPane {
    private static final String WORKING_DIR_HISTORY_ID = "googleDrive.workingDir";

    private final GoogleDriveConnectionSettingsModel m_settings;

    private CredentialRef m_credentialRef;

    private final WorkingDirectoryChooser m_workingDirChooser = new WorkingDirectoryChooser(WORKING_DIR_HISTORY_ID,
            this::createFSConnection);

    /**
     * Creates new instance.
     */
    public GoogleDriveConnectionNodeDialog() {
        m_settings = new GoogleDriveConnectionSettingsModel();

        addTab("Settings", createSettingsPanel());
        addTab("Advanced", createAdvancedPanel());
    }

    private JComponent createSettingsPanel() {
        final JPanel panel = new JPanel();
        final BoxLayout parentLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(parentLayout);

        panel.add(createFileSystemSettingsPanel());

        return panel;
    }

    private Component createFileSystemSettingsPanel() {
        final JPanel panel = new JPanel();
        final BoxLayout parentLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(parentLayout);
        panel.setBorder(createTitledBorder("File System settings"));

        panel.add(m_workingDirChooser);
        return panel;
    }

    /**
     * @param title
     *            border title.
     * @return titled border.
     */
    private static Border createTitledBorder(final String title) {
        return new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), title);
    }

    private FSConnection createFSConnection() throws IOException {
        try {
            m_settings.validate();

            final var creds = CredentialUtil.toOAuth2Credentials(m_credentialRef);
            final var connection = new GoogleDriveFSConnection(m_settings.toFSConnectionConfig(creds));
            GoogleDriveConnectionNodeModel.testConnection(connection);

            return connection;
        } catch (InvalidSettingsException | NoSuchCredentialException e) {
            throw ExceptionUtil.wrapAsIOException(e);
        }
    }

    private JComponent createAdvancedPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        addGbcRow(panel, 0,
                "Connection timeout (seconds): ",
                new DialogComponentNumber(m_settings.getConnectionTimeoutModel(), "", 1));

        addGbcRow(panel, 1,
                "Read timeout (seconds): ", new DialogComponentNumber(m_settings.getReadTimeoutModel(), "", 1));

        addVerticalFiller(panel, 8, 3);

        return panel;
    }

    private static void addVerticalFiller(final JPanel panel, final int row, final int columnCount) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = columnCount;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);
    }

    private static void addGbcRow(final JPanel panel, final int row, final String label, final DialogComponent comp) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gbc);

        gbc.gridx++;
        panel.add(comp.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        preSettingsSave();

        m_settings.validate();
        m_settings.save(settings);
    }

    private void preSettingsSave() {
        m_settings.getWorkingDirectoryModel().setStringValue(m_workingDirChooser.getSelectedWorkingDirectory());
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO input, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        m_credentialRef = ((CredentialPortObjectSpec) specs[0]).toRef();

        try {
            m_settings.load(input);
        } catch (final InvalidSettingsException e) { // NOSONAR can be ignored
        }
    }

    @Override
    public void onOpen() {
        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectory());
    }

    @Override
    public void onClose() {
        m_workingDirChooser.onClose();
    }
}
