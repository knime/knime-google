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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.google.api.analytics.ga4.docs.ExternalLinks;
import org.knime.google.api.analytics.ga4.node.GAAccount;
import org.knime.google.api.analytics.ga4.node.GAProperty;
import org.knime.google.api.analytics.ga4.port.GAConnection;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Settings for the Google Analytics 4 Connector node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class GAConnectorNodeSettings implements NodeParameters {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GAConnectorNodeSettings.class);

    @Persistor(GAAccount.Persistor.class)
    @Widget(title = "Google Analytics 4 account", description = """
            <p>
            Specify the Google Analytics 4
            <a href="
            """ + ExternalLinks.EXPLAIN_ACCOUNT + """
            ">account</a> from which you want to fetch properties.
            </p>
            <p>
            If no accounts are listed, check that at least one analytics account has been created with
            the admin account.
            </p>
            <p><b>Note for setting via a flow variable:</b>
            The account is identified by its <i>numeric</i> ID which is visible below the account&apos;s name on
            <a href="https://analytics.google.com">analytics.google.com</a> under the
            &quot;Analytics Accounts&quot; navigation section.
            </p>
            """)
    @ChoicesProvider(AnalyticsAccountsProvider.class)
    @ValueReference(AnalyticsAccountIdRef.class)
    GAAccount m_analyticsAccountId; // will implicitly be handled as string

    interface AnalyticsAccountIdRef extends ParameterReference<GAAccount> {
    }

    @Persistor(GAProperty.Persistor.class)
    @Widget(title = "Google Analytics 4 property",
        description = """
                <p>
            Specify the Google Analytics 4
            <a href="
            """ + ExternalLinks.EXPLAIN_PROPERTY + """
            ">property</a> from which you want to query data.
            </p>
            <p>
            If no properties are listed, check that your account has access to at least one <i>Google Analytics 4
            </i> property. Universal Analytics properties are not supported.
            </p>
            <p><b>Note for setting via a flow variable:</b>
            The property is identified by its <i>numeric</i> ID which is visible below the property&apos;s name on
            <a href="https://analytics.google.com">analytics.google.com</a> under the
            &quot;Properties &amp; Apps &quot; navigation section.
            </p>
            """)
    @ChoicesProvider(AnalyticsAccountUpdateHandler.class)
    GAProperty m_analyticsPropertyId; // will implicitly be handled as string

    /* Advanced settings */

    @Persistor(ConnectTimeoutPersistor.class)
    @Widget(title = "Connect timeout (seconds)", description = """
                    Specify the timeout in seconds to establish a connection.
            """, advanced = true)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    Duration m_connTimeoutSec = GAConnection.DEFAULT_CONNECT_TIMEOUT;

    @Persistor(ReadTimeoutPersistor.class)
    @Widget(title = "Read timeout (seconds)", description = """
                    Specify the timeout in seconds to read data from an already established connection.
            """, advanced = true)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    Duration m_readTimeoutSec = GAConnection.DEFAULT_READ_TIMEOUT;

    @Persistor(RetryMaxElapsedTimePersistor.class)
    @Widget(title = "Retry timeout (seconds)", description = """
                    Specify the total duration for which the same request is allowed to be retried in case of server
                    errors (5xx) and request timeouts (408), starting when the request is initially made.
            """, advanced = true)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    Duration m_retryMaxElapsedTimeSec = GAConnection.DEFAULT_ERR_RETRY_MAX_ELAPSED_TIME;

    /**
     * Constructor for de-/serialization.
     */
    GAConnectorNodeSettings() {
        // for de-/serialization
    }

    /**
     * Constructor with context for autoconfiguration.
     *
     * @param ctx context for auto-configuration
     */
    GAConnectorNodeSettings(final NodeParametersInput ctx) { //NOSONAR
        //
    }

    /**
     * Asynchronously fetches the Google Analytics account IDs and names from the API. Uses pagination to collect all
     * accounts.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    static final class AnalyticsAccountsProvider implements StringChoicesProvider {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput ctx) {
            final var credentialRef = GAConnectorNodeModel.getCredentialRef(ctx.getInPortSpecs());
            if (credentialRef.isEmpty()) {
                return Collections.emptyList();
            }
            try {
                final var conn = new GAConnection(credentialRef.get(), GAConnection.DEFAULT_CONNECT_TIMEOUT,
                    GAConnection.DEFAULT_READ_TIMEOUT, GAConnection.DEFAULT_ERR_RETRY_MAX_ELAPSED_TIME);
                return GAConnectorNodeModel.fetchAllAccountIdsAndNames(conn).stream()//
                    .map(pair -> new StringChoice(pair.getFirst(), pair.getSecond()))//
                    .toList();
            } catch (KNIMEException e) {
                LOGGER.error("Failed to retrieve Google Analytics 4 accounts from Google Analytics API.", e);
            }
            return Collections.emptyList();
        }

    }

    /**
     * After receiving the update of the chosen GA account ID, asynchronously fetches the Google Analytics property IDs
     * and names from the API. Uses pagination to collect all properties.
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    static final class AnalyticsAccountUpdateHandler implements StringChoicesProvider {

        Supplier<GAAccount> m_analyticsAccountIdSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_analyticsAccountIdSupplier = initializer.computeFromValueSupplier(AnalyticsAccountIdRef.class);
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput ctx) throws WidgetHandlerException {
            final var analyticsAccountId = m_analyticsAccountIdSupplier.get();
            if (analyticsAccountId == null) {
                return Collections.emptyList();
            }
            final var credentialRef = GAConnectorNodeModel.getCredentialRef(ctx.getInPortSpecs());
            if (credentialRef.isEmpty()) {
                return Collections.emptyList();
            }
            try {
                final var conn = new GAConnection(credentialRef.get(), GAConnection.DEFAULT_CONNECT_TIMEOUT,
                    GAConnection.DEFAULT_READ_TIMEOUT, GAConnection.DEFAULT_ERR_RETRY_MAX_ELAPSED_TIME);
                final var accountId = analyticsAccountId.getAccountId();
                return GAConnectorNodeModel.fetchPropertiesForAccount(conn, accountId).stream()//
                    .map(pair -> new StringChoice(pair.getFirst(), pair.getSecond()))//
                    .toList();
            } catch (KNIMEException e) {
                LOGGER.error("Failed to retrieve Google Analytics 4 properties from Google Analytics API.", e);
            }
            return Collections.emptyList();
        }
    }

    /**
     * Persistor for {@link Duration} values which are persisted in millisecond granularity.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    private static abstract class DurationPersistorAsMillis implements NodeParametersPersistor<Duration> {

        private final String m_configKey;

        protected DurationPersistorAsMillis(final String configKey) {
            m_configKey = configKey;
        }

        @Override
        public Duration load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return Duration.ofMillis(settings.getLong(m_configKey));
        }

        @Override
        public void save(final Duration duration, final NodeSettingsWO settings) {
            settings.addLong(m_configKey, duration.toMillis());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{m_configKey}};
        }

    }

    private static final class ConnectTimeoutPersistor extends DurationPersistorAsMillis {
        public ConnectTimeoutPersistor() {
            super(GAConnection.KEY_CONNECT_TIMEOUT);
        }
    }

    private static final class ReadTimeoutPersistor extends DurationPersistorAsMillis {
        public ReadTimeoutPersistor() {
            super(GAConnection.KEY_READ_TIMEOUT);
        }
    }

    private static final class RetryMaxElapsedTimePersistor extends DurationPersistorAsMillis {
        public RetryMaxElapsedTimePersistor() {
            super(GAConnection.KEY_ERR_RETRY_MAX_ELAPSED_TIME);
        }
    }
}
