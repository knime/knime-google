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
 */
package org.knime.google.cloud.storage.node.connector;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Google Cloud Storage Connector Node Dialog.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class GoogleCSConnectionNodeDialog extends NodeDialogPane {

    private final GoogleCSConnectionNodeSettings m_settings = new GoogleCSConnectionNodeSettings();

    private final DialogComponentString m_projectId =
        new DialogComponentString(m_settings.getProjectIdModel(), "Project identifier:", true, 40);

	GoogleCSConnectionNodeDialog() {
        final JPanel generalPanel = new JPanel(new BorderLayout());
        final Box generalBox = Box.createVerticalBox();
        generalBox.add(m_projectId.getComponentPanel());
        generalPanel.add(generalBox, BorderLayout.NORTH);
        addTab("General", generalPanel);
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
	    m_settings.validateDeeper();
		m_settings.saveSettingsTo(settings);

        // We need to do this to trigger validation in some of the dialog components and produce an error
        // when OK or Apply is clicked while invalid values have been entered.
        // Settings models such as SettingsModelString don't accept invalid values. Hence if someone
        // enters an invalid value in the dialog component it will not be put into the settings model.
        // Hence settings model and dialog component go out of sync. If we just save the settings model,
        // it will just save the previous valid value and the dialog will close  with out error
		m_projectId.saveSettingsTo(settings);
	}

	@Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {

	    try {
	        m_settings.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage());
        }
	}
}
