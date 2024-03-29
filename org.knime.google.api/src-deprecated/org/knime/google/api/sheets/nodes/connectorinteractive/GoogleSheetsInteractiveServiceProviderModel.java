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
 *   Aug 21, 2017 (oole): created
 */
package org.knime.google.api.sheets.nodes.connectorinteractive;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.google.api.sheets.data.GoogleSheetsConnection;
import org.knime.google.api.sheets.data.GoogleSheetsConnectionPortObject;
import org.knime.google.api.sheets.data.GoogleSheetsConnectionPortObjectSpec;

/**
 * The model to the GoogleSheetsConnector node.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
@Deprecated
final class GoogleSheetsInteractiveServiceProviderModel extends NodeModel {

    private GoogleInteractiveServiceProviderSettings m_settings = getSettings();

    /**
     * Returns the settings for this node for this node.
     *
     * @return The settings for this node
     */
    protected static GoogleInteractiveServiceProviderSettings getSettings() {
        return new GoogleInteractiveServiceProviderSettings();
    }

    /**
     * Constructor of the node model.
     */
    protected GoogleSheetsInteractiveServiceProviderModel() {
        super(new PortType[]{},
                new PortType[]{GoogleSheetsConnectionPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
            return new PortObject[]{new GoogleSheetsConnectionPortObject(createSpec())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createSpec()};
    }

    /**
     * Returns the specs given the user, credential path and execution state.
     *
     * @return The spec of this node, containing the GoogleApiConnection for the current configuration
     * @throws InvalidSettingsException If the current configuration is not valid
     * @throws IOException If the current configuration is not valid
     * @throws URISyntaxException
     * @throws GeneralSecurityExceptionGeneralSecurityException If the current configuration is not valid
     */
    private GoogleSheetsConnectionPortObjectSpec createSpec()
            throws InvalidSettingsException {
        if (m_settings.inNodeCredential() && m_settings.getEncodedStoredCredential() == null) {
            throw new InvalidSettingsException("Not authenticated. Authenticate in the node dialog.");
        }
        try {
            return new GoogleSheetsConnectionPortObjectSpec(new GoogleSheetsConnection(m_settings.getCredentialLocation(),
                    m_settings.getUserString(), m_settings.getLocationType()));
        } catch (IOException e) {
            throw new InvalidSettingsException("(Re-)Authenticate using the settings dialog.\n");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
    CanceledExecutionException {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
    CanceledExecutionException {
        // not used
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // not used
    }
}
