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
 *   10 Mar 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * A representation of a Google Analytics 4 property, which is identified by a numeric ID.
 * The existence of an instance of this class does not imply that the property exists with Google Analytics.
 * In other words, when creating an instance, <em>no API calls</em> are made to verify the property identified
 * by the ID actually exists, is accessible, or is indeed representing a Google Analytics 4 property (and not, e.g.
 * a Universal Analytics property).
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class GAProperty {
    // Note: the property id is a String since it is never used as a numeric type but the
    // Google Analytics Data/Admin API requires it to be numeric

    private final String m_propertyId;

    /**
     *  @param propertyId numeric Google Analytics 4 Property ID (<b>without</b> prefix, e.g. "p/" or "property/")
     */
    private GAProperty(final String propertyId) {
        m_propertyId = propertyId;
    }


    /**
     * Creates a Google Analytics 4 property.
     *
     * @param propertyId Numeric identifier (without prefix).
     * @return Google Analytics property
     */
    public static GAProperty of(final String propertyId) {
        return new GAProperty(propertyId);
    }

    /**
     * Returns the numeric ID of the property (without "property/" prefix).
     * @return the propertyId
     */
    public String getPropertyId() {
        return m_propertyId;
    }

    /**
     * Loads the Google Analytics 4 property from the given config.
     *
     * @param cfg config to load from
     * @param key to find the property under
     * @return valid representation of a Google Analytics 4 property
     * @throws InvalidSettingsException if the model did not provide a valid GA property
     */
    public static GAProperty loadSettings(final ConfigRO cfg, final String key)
            throws InvalidSettingsException {
        return new GAProperty(cfg.getString(key));
    }

    /**
     * Saves the property into the config.
     *
     * @param cfg config to save into
     * @param key the key to save the property under
     */
    public void saveSettings(final ConfigWO cfg, final String key) {
        cfg.addString(key, getPropertyId());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 37).append(m_propertyId).toHashCode();
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
        final var rhs = (GAProperty) obj;
        return new EqualsBuilder()
                      .append(m_propertyId, rhs.m_propertyId)
                      .isEquals();
    }
}
