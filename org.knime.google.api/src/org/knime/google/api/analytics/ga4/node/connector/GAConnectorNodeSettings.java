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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.google.api.analytics.ga4.node.GAProperty;
import org.knime.google.api.analytics.ga4.node.connector.SettingsModelDuration.DurationAsMillis;
import org.knime.google.api.analytics.ga4.port.GAConnection;

/**
 * Settings for the Google Analytics 4 Connector node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class GAConnectorNodeSettings {

    final SettingsModelItem<GAProperty> m_analyticsPropertyId = SettingsModelGAProperty.create("ga4Property", null);

    /* Advanced settings */
    final SettingsModelDuration m_connTimeoutSec = SettingsModelDuration.create(GAConnection.KEY_CONNECT_TIMEOUT,
        GAConnection.DEFAULT_CONNECT_TIMEOUT, new DurationAsMillis(GAConnection.KEY_CONNECT_TIMEOUT));

    final SettingsModelDuration m_readTimeoutSec = SettingsModelDuration.create(GAConnection.KEY_READ_TIMEOUT,
        GAConnection.DEFAULT_READ_TIMEOUT, new DurationAsMillis(GAConnection.KEY_READ_TIMEOUT));

    final SettingsModelDuration m_retryMaxElapsedTimeSec = SettingsModelDuration.create(
        GAConnection.KEY_ERR_RETRY_MAX_ELAPSED_TIME, GAConnection.DEFAULT_ERR_RETRY_MAX_ELAPSED_TIME,
        new DurationAsMillis(GAConnection.KEY_ERR_RETRY_MAX_ELAPSED_TIME));

    /**
     * Constructor.
     */
    GAConnectorNodeSettings() {
        // nothing to do
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_analyticsPropertyId.loadSettingsFrom(settings);
        m_connTimeoutSec.loadSettingsFrom(settings);
        m_readTimeoutSec.loadSettingsFrom(settings);
        m_retryMaxElapsedTimeSec.loadSettingsFrom(settings);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        saveSettingsTo(settings);
    }

    void saveSettingsTo(final NodeSettingsWO settings) {
        m_analyticsPropertyId.saveSettingsTo(settings);
        m_connTimeoutSec.saveSettingsTo(settings);
        m_readTimeoutSec.saveSettingsTo(settings);
        m_retryMaxElapsedTimeSec.saveSettingsTo(settings);
    }

    void validateSettings(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        m_analyticsPropertyId.validateSettings(nodeSettings);
        m_connTimeoutSec.validateSettings(nodeSettings);
        m_readTimeoutSec.validateSettings(nodeSettings);
        m_retryMaxElapsedTimeSec.validateSettings(nodeSettings);
    }

}
