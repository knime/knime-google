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

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.util.CheckUtils;
import org.knime.google.api.analytics.ga4.node.GAProperty;
import org.knime.google.api.data.GoogleApiConnectionPortObject;

/**
 * Port object representing a Google Analytics 4 API connection. For the actual connection class,
 * see {@link GAConnection}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class GAConnectionPortObject extends AbstractSimplePortObject {

    /** Type of the port object. */
    @SuppressWarnings("hiding")
    public static final PortType TYPE =
            PortTypeRegistry.getInstance().getPortType(GAConnectionPortObject.class);

    /**
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class Serializer extends AbstractSimplePortObjectSerializer<GAConnectionPortObject> {}

    private GAConnectionPortObjectSpec m_spec;

    /** Constructor for (de-)serialization. */
    public GAConnectionPortObject() {
        // public no-args constructor required by De-/Serializer
    }

    /**
     * Constructor.
     * @param spec spec for the port object
     */
    public GAConnectionPortObject(final GAConnectionPortObjectSpec spec) {
        m_spec = Objects.requireNonNull(spec);
    }

    @Override
    public String getSummary() {
        return m_spec.getSummary();
    }

    @Override
    public GAConnectionPortObjectSpec getSpec() {
        return m_spec;
    }

    /**
     * Get the Google Analytics connection if available.
     * @return connection or empty if the connection is unavailable
     */
    public GAConnection getConnection() {
        return m_spec.getConnection();
    }

    /**
     * Get the Google Analytics Property if available.
     * @return property or empty if property is not available
     */
    public GAProperty getProperty() {
        return m_spec.getProperty();
    }

    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec) throws CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException {
        m_spec = (GAConnectionPortObjectSpec)spec;
    }

    @Override
    public JComponent[] getViews() {
        return m_spec.getViews();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof GAConnectionPortObject otherPO) {
            return m_spec.equals(otherPO.m_spec);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return m_spec.hashCode();
    }

    /**
     * Convenience method to obtain a Google API connection port object from the input port objects at the given
     * offset.
     *
     * If the index is not occupied by any port object (possibly since the array is null or empty, or there is a
     * null value at the specified index) and empty optional is returned.
     *
     * If the existing port object is not of type {@link GoogleApiConnectionPortObject}, an exception is thrown.
     *
     * @param inObjects input port objects, possibly null or empty
     * @param portIdx index to look at
     * @return a Google API connection port object, or empty optional if no port object is available
     * @throws IllegalArgumentException if the existing port object is not of type
     *             {@link GoogleApiConnectionPortObject}
     */
    public static Optional<GoogleApiConnectionPortObject> getGoogleApiConnectionPortObject(
            final PortObject[] inObjects, final int portIdx) {
        if (inObjects == null || inObjects.length == 0 || inObjects[portIdx] == null) {
            return Optional.empty();
        }
        return Optional.of(CheckUtils.checkCast(inObjects[portIdx],
            GoogleApiConnectionPortObject.class, IllegalArgumentException::new,
            "Input Port Object is not a Google API connection."));
    }


}
