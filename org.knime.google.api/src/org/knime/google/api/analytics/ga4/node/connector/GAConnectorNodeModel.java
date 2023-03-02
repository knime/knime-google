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
 *   24 Feb 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.connector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
import org.knime.core.node.util.CheckUtils;
import org.knime.google.api.analytics.ga4.port.GAConnection;
import org.knime.google.api.analytics.ga4.port.GAConnectionPortObject;
import org.knime.google.api.analytics.ga4.port.GAConnectionPortObjectSpec;
import org.knime.google.api.data.GoogleApiConnectionPortObject;
import org.knime.google.api.data.GoogleApiConnectionPortObjectSpec;

/**
 * Google Analytics connector node model for use with
 * <a href="https://support.google.com/analytics/answer/10089681">Google Analytics 4 properties</a>.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class GAConnectorNodeModel extends NodeModel {

    private static final int GOOGLE_API_CONNECTION_PORT = 0;

    private GAConnectorNodeSettings m_settings = new GAConnectorNodeSettings();

    /**
     * Constructor.
     */
    protected GAConnectorNodeModel() {
        super(new PortType[] {GoogleApiConnectionPortObject.TYPE}, new PortType[] { GAConnectionPortObject.TYPE });
    }


    @Override
    protected void validateSettings(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        m_settings.validateSettings(nodeSettings);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final var connSpec = GAConnectionPortObjectSpec
                .getGoogleApiConnectionPortObjectSpec(inSpecs, GOOGLE_API_CONNECTION_PORT)
                .orElseThrow(() -> new InvalidSettingsException("Google API connection is missing."));
        CheckUtils.checkSettingNotNull(m_settings.m_analyticsPropertyId.getValue(),
            "Google Analytics Property ID is missing.");

        final var conn = new GAConnection(connSpec.getGoogleApiConnection(), m_settings.m_connTimeoutSec.getValue(),
            m_settings.m_readTimeoutSec.getValue(), m_settings.m_retryMaxElapsedTimeSec.getValue());

        return new PortObjectSpec[] {new GAConnectionPortObjectSpec(conn, m_settings.m_analyticsPropertyId.getValue())};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final var apiConnPO = GAConnectionPortObject
                .getGoogleApiConnectionPortObject(inObjects, GOOGLE_API_CONNECTION_PORT)
                .orElseThrow(() -> new Exception("Google API connection is missing"));
        final var conn = new GAConnection(apiConnPO.getGoogleApiConnection(), m_settings.m_connTimeoutSec.getValue(),
            m_settings.m_readTimeoutSec.getValue(), m_settings.m_retryMaxElapsedTimeSec.getValue());
        final var props = conn.accountSummaries().stream()
                .flatMap(acc -> Optional.ofNullable(acc.getPropertySummaries()).orElse(List.of()).stream()
                    .map(p -> p.getProperty().replace("properties/", ""))).count();
        if (props == 0) {
            throw new Exception("None of the available accounts contains a Google Analytics 4 property.");
        }
        final var spec = new GAConnectionPortObjectSpec(conn, m_settings.m_analyticsPropertyId.getValue());
        return new PortObject[] { new GAConnectionPortObject(spec) };
    }

    public static Optional<GoogleApiConnectionPortObjectSpec> getGoogleApiConnectionPortObjectSpec(
            final PortObjectSpec[] pos) {
        return GAConnectionPortObjectSpec.getGoogleApiConnectionPortObjectSpec(pos, GOOGLE_API_CONNECTION_PORT);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // not needed
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // not needed
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsForModel(settings);
    }

    @Override
    protected void reset() {
        // not needed
    }

}
