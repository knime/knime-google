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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.google.api.analytics.ga4.docs.ExternalLinks;
import org.knime.google.api.analytics.ga4.node.GAProperty;
import org.knime.google.api.analytics.ga4.port.GAConnection;

/**
 * Settings for the Google Analytics 4 Connector node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class GAConnectorNodeSettings implements DefaultNodeSettings {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GAConnectorNodeSettings.class);

    @Persist(customPersistor = GAProperty.Persistor.class, configKey = "ga4Property")
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
    @ChoicesWidget(choices = AnalyticsPropertiesProvider.class)
    // will implicitly be handled as string
    GAProperty m_analyticsPropertyId;

    /* Advanced settings */

    @Persist(customPersistor = DurationPersistorAsMillis.class, configKey = GAConnection.KEY_CONNECT_TIMEOUT)
    @Widget(title = "Connect timeout (seconds)", description = """
                    Specify the timeout in seconds to establish a connection.
            """, advanced = true)
    @NumberInputWidget(min = 1)
    Duration m_connTimeoutSec = GAConnection.DEFAULT_CONNECT_TIMEOUT;

    @Persist(customPersistor = DurationPersistorAsMillis.class, configKey = GAConnection.KEY_READ_TIMEOUT)
    @Widget(title = "Read timeout (seconds)", description = """
                    Specify the timeout in seconds to read data from an already established connection.
            """, advanced = true)
    @NumberInputWidget(min = 1)
    Duration m_readTimeoutSec = GAConnection.DEFAULT_READ_TIMEOUT;

    @Persist(customPersistor = DurationPersistorAsMillis.class, configKey = GAConnection.KEY_ERR_RETRY_MAX_ELAPSED_TIME)
    @Widget(title = "Retry timeout (seconds)", description = """
                    Specify the total duration for which the same request is allowed to be retried in case of server
                    errors (5xx) and request timeouts (408), starting when the request is initially made.
            """, advanced = true)
    @NumberInputWidget(min = 1)
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
    GAConnectorNodeSettings(final DefaultNodeSettingsContext ctx) {
        //
    }

    /**
     * Synchronously fetch Google Analytics Property IDs from the API.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    static final class AnalyticsPropertiesProvider implements ChoicesProvider {

        @Override
        public String[] choices(final DefaultNodeSettingsContext ctx) {
            final var connSpec = GAConnectorNodeModel.getGoogleApiConnectionPortObjectSpec(ctx.getPortObjectSpecs());
            if (connSpec.isPresent()) {
                try {
                    final var d = Duration.ofSeconds(6);
                    // We intentionally use a very short duration since the user likely does not see
                    // what is currently going on (and we don't have access here to the user-provided values).
                    return new GAConnection(connSpec.get().getGoogleApiConnection(), d, d, d).accountSummaries()
                            .stream()
                            .flatMap(acc -> Optional.ofNullable(acc.getPropertySummaries()).orElse(List.of()).stream()
                                .map(p -> p.getProperty().replace("properties/", "")))
                            .collect(Collectors.toList())
                            .toArray(String[]::new);
                } catch (KNIMEException e) {
                    LOGGER.error("Failed to retrieve Google Analytics 4 Properties from Google Analytics API.", e);
                }
            }
            return new String[0];
        }

    }

    /**
     * Persistor for {@link Duration} values which are persisted in millisecond granularity.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    private static final class DurationPersistorAsMillis extends NodeSettingsPersistorWithConfigKey<Duration> {

        @Override
        public Duration load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return Duration.ofMillis(settings.getLong(getConfigKey()));
        }

        @Override
        public void save(final Duration duration, final NodeSettingsWO settings) {
            settings.addLong(getConfigKey(), duration.toMillis());
        }

    }
}
