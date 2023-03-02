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
 *   13 Jun 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.connector;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.google.api.analytics.ga4.node.GAProperty;
import org.knime.google.api.analytics.ga4.port.GAConnection;
import org.knime.google.api.data.GoogleApiConnectionPortObjectSpec;

import com.google.api.services.analyticsadmin.v1beta.model.GoogleAnalyticsAdminV1betaPropertySummary;

/**
 * Swing-based dialog for the Google Analytics Connection node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class GAConnectorDialog extends DefaultNodeSettingsPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GAConnectorDialog.class);

    private final GAConnectorNodeSettings m_settings;
    private final DialogComponentItemView<GAProperty, PropertyMetadata, GAPropertyView> m_property;
    private final DialogComponentTimeout m_connectTimeout;
    private final DialogComponentTimeout m_readTimeout;
    private final DialogComponentTimeout m_retry;

    GAConnectorDialog() {
        m_settings = new GAConnectorNodeSettings();

        m_property = new DialogComponentItemView<GAProperty, PropertyMetadata, GAPropertyView>(
                m_settings.m_analyticsPropertyId, "Google Analytics 4 property", GAPropertyView::new,
                new GAPropertyViewCellRenderer());
        addDialogComponent(m_property);

        createNewTab("Advanced Settings");
        m_connectTimeout = new DialogComponentTimeout(m_settings.m_connTimeoutSec, "Connect timeout (seconds)");
        addDialogComponent(m_connectTimeout);
        m_readTimeout = new DialogComponentTimeout(m_settings.m_readTimeoutSec, "Read timeout (seconds)");
        addDialogComponent(m_readTimeout);
        m_retry = new DialogComponentTimeout(m_settings.m_retryMaxElapsedTimeSec, "Retry timeout (seconds)");
        addDialogComponent(m_retry);
    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        final var connSpec = GAConnectorNodeModel.getGoogleApiConnectionPortObjectSpec(specs);
        if (connSpec.isPresent()) {
            new PropertyFetcher(connSpec.get(), props -> m_property.addElements(props)).execute();
        }
    }

    static class PropertyMetadata {
        final String m_account;
        final String m_displayName;

        PropertyMetadata(final String account, final String displayName) {
            m_account = account;
            m_displayName = displayName;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
                }
            if (obj == this) {
                return true;
                }
            if (obj.getClass() != getClass()) {
              return false;
            }
            final var rhs = (PropertyMetadata) obj;
            return new EqualsBuilder()
                          .append(m_account, rhs.m_account)
                          .append(m_displayName, rhs.m_displayName)
                          .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(13, 37).append(m_account).append(m_displayName).toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("account", m_account).append("displayName", m_displayName).toString();
        }
    }

    /**
     * Class to hold some optional meta info about the property.
     */
    static class GAPropertyView implements ItemView<GAProperty, PropertyMetadata>{
        final GAProperty m_property;
        PropertyMetadata m_metadata;

        /**
         * Constructor.
         * @param property non-null property
         * @param account nullable account name
         * @param displayName nullable display name
         */
        GAPropertyView(final GAProperty property, final PropertyMetadata metadata) {
            m_property = Objects.requireNonNull(property);
            m_metadata = metadata;
        }

        /**
         * Parses the formatted strings from a property summary returned by the Google Analytics Admin API into an item
         * that we can display in the dialog.
         *
         * @param summary Google Analytics Admin API Property summary
         * @return property item
         */
        static GAPropertyView parseSummary(final GoogleAnalyticsAdminV1betaPropertySummary summary) {
            final var property = summary.getProperty().replace("properties/", "");
            final var account = summary.getParent().replace("accounts/", "");
            return new GAPropertyView(GAProperty.of(property), new PropertyMetadata(account, summary.getDisplayName()));
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(13, 31).append(m_property).append(m_metadata).toHashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
              return false;
            }
            final var rhs = (GAPropertyView) obj;
            return new EqualsBuilder()
                          .append(m_property, rhs.m_property)
                          .append(m_metadata, rhs.m_metadata)
                          .isEquals();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("propertyId", m_property.getPropertyId()).append("metadata", m_metadata).toString();
        }

        @Override
        public GAProperty getItem() {
            return m_property;
        }

        @Override
        public Optional<PropertyMetadata> getViewMetadata() {
            return Optional.ofNullable(m_metadata);
        }
    }

    private static final class GAPropertyViewCellRenderer
            implements ItemViewCellRenderer<GAProperty, PropertyMetadata, GAPropertyView> {
        @Override
        public String getLabel(final GAPropertyView view) {
            final var viewMeta = view.getViewMetadata();
            if (viewMeta.isPresent()) {
                return viewMeta.get().m_displayName;
            }
            return view.m_property.getPropertyId();
        }

        @Override
        public String getToolTipText(final GAPropertyView view) {
            final var viewMeta = view.getViewMetadata();
            if (viewMeta.isPresent()) {
                final var sb = new StringBuilder().append("<html>");
                final var meta = viewMeta.get();

                sb.append("<b>Account:</b> ").append(meta.m_account);
                sb.append("<br>");
                // show the ID in the tool tip if the display name was shown in the label
                sb.append("<b>Property:</b> ").append(view.m_property.getPropertyId());

                return sb.append("</html>").toString();
            }
            return null;
        }
    }

    private final class PropertyFetcher extends SwingWorkerWithContext<GAPropertyView[], Void> {

        private final GoogleApiConnectionPortObjectSpec m_spec;
        private Consumer<GAPropertyView[]> m_callback;

        PropertyFetcher(final GoogleApiConnectionPortObjectSpec spec, final Consumer<GAPropertyView[]> callback) {
            m_spec = Objects.requireNonNull(spec);
            m_callback = Objects.requireNonNull(callback);
        }

        @Override
        protected GAPropertyView[] doInBackgroundWithContext() throws Exception {
            final var d = Duration.ofSeconds(6);
            return new GAConnection(m_spec.getGoogleApiConnection(), d, d, d).accountSummaries()
                    .stream()
                    .flatMap(acc -> Optional.ofNullable(acc.getPropertySummaries())
                        .orElse(Collections.emptyList()).stream())
                    .map(GAPropertyView::parseSummary)
                    .collect(Collectors.toList())
                    .toArray(GAPropertyView[]::new);
        }

        @Override
        protected void doneWithContext() {
            try {
                m_callback.accept(get());
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                LOGGER.warn(String.format("Could not fetch properties from Google Analytics API: %s", e.getMessage()),
                    e);
            }
        }
    }


}
