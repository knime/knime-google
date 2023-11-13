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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.credentials.base.CredentialRef;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.google.api.analytics.ga4.port.GAConnection;
import org.knime.google.api.analytics.ga4.port.GAConnectionPortObject;
import org.knime.google.api.analytics.ga4.port.GAConnectionPortObjectSpec;

/**
 * Google Analytics connector node model for use with
 * <a href="https://support.google.com/analytics/answer/10089681">Google Analytics 4 properties</a>.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class GAConnectorNodeModel extends WebUINodeModel<GAConnectorNodeSettings> {

    private static final int CREDENTIAL_INPUT_PORT = 0;

    /**
     * @param configuration
     */
    protected GAConnectorNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, GAConnectorNodeSettings.class);
    }

    @Override
    protected void validateSettings(final GAConnectorNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_analyticsPropertyId != null) {
            CheckUtils.checkSetting(StringUtils.isNotBlank(settings.m_analyticsPropertyId.m_propertyId()),
                "Google Analytics property identifier must not be blank.");
            CheckUtils.checkSetting(NumberUtils.isDigits(settings.m_analyticsPropertyId.m_propertyId()),
                "Google Analytics property identifier must be numeric (i.e. contain only digits).");
        }
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final GAConnectorNodeSettings modelSettings)
            throws InvalidSettingsException {

        final var credentialRef = getCredentialRef(inSpecs);

        // we do not require the analytics ID to be non-null
        // only the property ID is needed for the output port object
        CheckUtils.checkSettingNotNull(modelSettings.m_analyticsPropertyId,
            "Google Analytics property ID is missing.");

        final var conn = new GAConnection(credentialRef, modelSettings.m_connTimeoutSec, //NOSONAR
            modelSettings.m_readTimeoutSec, modelSettings.m_retryMaxElapsedTimeSec);

        return new PortObjectSpec[] {new GAConnectionPortObjectSpec(conn, modelSettings.m_analyticsPropertyId)};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec,
            final GAConnectorNodeSettings modelSettings) throws Exception {

        final var credentialRef = getCredentialRef(inObjects);

        final var conn = new GAConnection(credentialRef, modelSettings.m_connTimeoutSec,
            modelSettings.m_readTimeoutSec, modelSettings.m_retryMaxElapsedTimeSec);

        if (!fetchPropertyValidity(conn, modelSettings.m_analyticsPropertyId.getPropertyId())) {
            throw KNIMEException.of(createMessageBuilder()//
                .withSummary("No Google Analytics property could be found for the selected property ID.")//
                .build().orElseThrow());
        }

        final var spec = new GAConnectionPortObjectSpec(conn, modelSettings.m_analyticsPropertyId);
        return new PortObject[] { new GAConnectionPortObject(spec) };
    }

    static CredentialRef getCredentialRef(final PortObject[] portObjects) {
        return ((CredentialPortObject) portObjects[CREDENTIAL_INPUT_PORT]).toRef();
    }

    static CredentialRef getCredentialRef(final PortObjectSpec[] portObjectSpecs) {
        return ((CredentialPortObjectSpec) portObjectSpecs[CREDENTIAL_INPUT_PORT]).toRef();
    }

    static List<Pair<String, String>> fetchAllAccountIdsAndNames(final GAConnection connection) throws KNIMEException {
        return connection.accountSummaries().stream()//
            .map(acc -> Pair.create(acc.getAccount().replace(GAConnection.ACCOUNTS_PREFIX, ""), acc.getDisplayName()))//
            .toList();
    }

    static List<Pair<String, String>> fetchPropertiesForAccount(final GAConnection connection, final String accountId)
        throws KNIMEException {
        return connection.propertiesForAccount(accountId).stream()//
            .map(p -> Pair.create(p.getName().replace(GAConnection.PROPERTIES_PREFIX, ""), p.getDisplayName()))//
            .toList();
    }

    static boolean fetchPropertyValidity(final GAConnection connection, final String propertyId) throws KNIMEException {
        try {
            return connection.property(propertyId) != null;
        } catch (KNIMEException e) { // NOSONAR
            if (e.getCause() instanceof NoSuchCredentialException) {
                // message of no-such-credentials contains instructions for the user
                throw e;
            }
            return false;
        }
    }

}
