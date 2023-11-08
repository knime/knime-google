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
package org.knime.google.api.analytics.ga4.port;

import java.util.Objects;
import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ViewUtils;
import org.knime.google.api.analytics.ga4.node.GAProperty;

/**
 * Specification for the {@link GAConnectionPortObject}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class GAConnectionPortObjectSpec extends AbstractSimplePortObjectSpec {

    /**
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class Serializer extends AbstractSimplePortObjectSpecSerializer<GAConnectionPortObjectSpec> {}

    private GAConnection m_connection;

    /** Key for storing the Property information. */
    private static final String KEY_GA_PROPERTY = "googleAnalyticsProperty";
    private GAProperty m_property;

    /** Constructor for (de-)serialization. */
    public GAConnectionPortObjectSpec() {
        // public no-args constructor required by De-/Serializer
    }

    /**
     * Constructor using the given connection, possibly {@code null}.
     *
     * @param connection connection to use or {@code null} if no connection is present
     * @param property Google Analytics property to connect to or {@code null} if no property is present
     */
    public GAConnectionPortObjectSpec(final GAConnection connection,
        final GAProperty property) {

        m_connection = Objects.requireNonNull(connection);
        m_property = Objects.requireNonNull(property);
    }

    /**
     * Gets the Google Analytics connection if available.
     * @return connection or empty if the connection is not available
     */
    public GAConnection getConnection() {
        return m_connection;
    }

    /**
     * Get the Google Analytics Property if available.
     * @return property or empty if property is not available
     */
    public GAProperty getProperty() {
        return m_property;
    }

    String getSummary() {
        final var sb = new StringBuilder();
        if (m_property != null) {
            sb.append("Google Analytics 4 Property: %s%n".formatted(m_property.getPropertyId()));
        } else {
            sb.append("Google Analytics Property unavailable.");
        }
        if (m_connection != null) {
            sb.append("Google Analytics Connection:\n").append(m_connection.toString());
        } else {
            sb.append("Google Analytics connection unavailable.");
        }
        return sb.toString();
    }

    @Override
    protected void save(final ModelContentWO model) {
        m_connection.saveTo(model);
        m_property.saveSettings(model, KEY_GA_PROPERTY);
    }

    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        m_connection = new GAConnection(model);
        m_property = GAProperty.loadSettings(model, KEY_GA_PROPERTY);
    }

    @Override
    public JComponent[] getViews() {
        final var sb = new StringBuilder("<html>")
                .append(getSummary().replace("\n", "<br>"))
                .append("</html>");
        final var f = ViewUtils.getInFlowLayout(new JLabel(sb.toString()));
        f.setName("Connection");
        return new JComponent[]{ f };
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof GAConnectionPortObjectSpec otherSpec) {
            return super.equals(otherSpec)
                    && m_connection != null
                    && m_property != null
                    && otherSpec.m_connection != null
                    && otherSpec.m_property != null
                    && m_connection.equals(otherSpec.m_connection) && m_property.equals(otherSpec.m_property);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode())
                .append(m_connection).append(m_property).toHashCode();
    }

    /**
     * Convenience method to obtain a Google Analytics connection port object spec from the input specs at the given
     * offset.
     *
     * If the index is not occupied by any port object spec (possibly since the array is null or empty, or there is a
     * null value at the specified index) and empty optional is returned.
     *
     * If the existing port object spec is not of type {@link GAConnectionPortObjectSpec}, an exception is thrown.
     *
     * @param inSpecs input port object specs, possibly null or empty
     * @param portIdx index to look at
     * @return a Google Analytics connection port object spec, or empty optional if no spec is available
     * @throws IllegalArgumentException if the existing port object spec is not of type
     *             {@link GAConnectionPortObjectSpec}
     */
    public static Optional<GAConnectionPortObjectSpec> getGoogleAnalyticsConnectionPortObjectSpec(
            final PortObjectSpec[] inSpecs, final int portIdx) {
        if (inSpecs == null || inSpecs.length == 0 || inSpecs[portIdx] == null) {
            return Optional.empty();
        }
        return Optional.of(CheckUtils.checkCast(inSpecs[portIdx],
            GAConnectionPortObjectSpec.class, IllegalArgumentException::new,
            "Input Port Object Spec is not a Google Analytics Connection."));
    }
}
