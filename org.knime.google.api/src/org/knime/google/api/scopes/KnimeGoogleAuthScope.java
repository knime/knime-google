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
 *   Oct 2, 2018 (oole): created
 */
package org.knime.google.api.scopes;

import java.util.List;

/**
 * Interface that must be implemented for an {@link KnimeGoogleAuthScopeRegistry} extension.
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public interface KnimeGoogleAuthScope {

     /**
      * Returns the {@link KnimeGoogleAuthScope}'s ID.
      *
      * @return The {@link KnimeGoogleAuthScope}'s ID
      */
     public String getScopeID();

    /**
     * Returns the name of this KnimeGoogleAuthScope.
     *
     * @return The name of` this KnimeGoogleAuthScope
     */
    public String getAuthScopeName();

    /**
     * Returns a list of scopes needed for this KnimeGoogleAuthScope.
     * (e.g. {@code com.google.api.services.analytics.AnalyticsScopes.ANALYTICS})
     *
     * @return A list of scopes for this KnimeGoogleAuthScope
     */
    public List<String> getAuthScopes();

    /**
     * Returns the description of this KnimeGoogleAuthScope.
     *
     * @return The description of this KnimeGoogleAuthScope
     */
    public String getDescription();

    /**
     * Returns whether this scope is enabled for OAuth authentication.
     *
     * @return Whether this scope is enabled for OAuth authentication
     */
    public default boolean isEnabledForOAuth() {
        return true;
    }

    /**
     * Returns whether this scope is enabled for Service Account authentication.
     *
     * @return Whether this scope is enabled for Service Account authentication
     */
    public default boolean isEnabledForServiceAccount() {
        return true;
    }

    /**
     * @return Whether this scope requires providing custom OAuth Client ID
     */
    public default boolean isCustomClientIdRequired() {
        return false;
    }
}
