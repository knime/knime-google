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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

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
import org.knime.credentials.base.CredentialCache;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.google.api.credential.GoogleCredential;
import org.knime.google.api.nodes.util.PathUtil;
import org.knime.google.api.nodes.util.ServiceAccountCredentialsUtil;

/**
 * The model of the GoogleApiConnector node.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class GoogleApiConnectorModel extends NodeModel {

    private GoogleApiConnectorConfiguration m_config = new GoogleApiConnectorConfiguration();

    private UUID m_credentialCacheKey;

    /**
     * Constructor of the node model.
     */
    protected GoogleApiConnectorModel() {
        super(new PortType[0], new PortType[]{CredentialPortObject.TYPE});
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_config.validateOnConfigure();
        return new PortObjectSpec[]{ new CredentialPortObjectSpec(GoogleCredential.TYPE, null)};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        final var keyFilePath = PathUtil.resolveToLocalPath(m_config.getKeyFileLocation());
        final var serviceCreds =
            ServiceAccountCredentialsUtil.loadFromP12(m_config.getServiceAccountEmail(), keyFilePath)//
                .createScoped(m_config.getScopes());
        m_credentialCacheKey = CredentialCache.store(new GoogleCredential(serviceCreds));

        return new PortObject[]{
            new CredentialPortObject(new CredentialPortObjectSpec(GoogleCredential.TYPE, m_credentialCacheKey))};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not used
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not used
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.save(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        GoogleApiConnectorConfiguration config = new GoogleApiConnectorConfiguration();
        config.loadInModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        GoogleApiConnectorConfiguration config = new GoogleApiConnectorConfiguration();
        config.loadInModel(settings);
        m_config = config;
    }

    @Override
    protected void onDispose() {
        reset();
    }

    @Override
    protected void reset() {
        if (m_credentialCacheKey != null) {
            CredentialCache.delete(m_credentialCacheKey);
            m_credentialCacheKey = null;
        }
    }
}