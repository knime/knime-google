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
 *   Jul 1, 2024 (lw): created
 */
package org.knime.google.api.nodes.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.DefaultClientConnectionReuseStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Connection reuse strategy that always denies reusing the connection when a proxy was used,
 * as that data (especially credentials) are not matched when checking whether a connection
 * can be reused by the {@link HttpClientConnectionManager} of the Apache HTTP client.
 * <p>
 * We cannot simply listen on changes in proxy settings because the reuse/keep-alive property
 * is always checked immediately *after* an HTTP request, not before one. We can never know if
 * proxy settings have changed.
 * </p>
 *
 * @author @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
final class ProxyConnectionReuseStrategy extends DefaultClientConnectionReuseStrategy {

    @SuppressWarnings("hiding")
    static final ProxyConnectionReuseStrategy INSTANCE = new ProxyConnectionReuseStrategy();

    /**
     * Hides constructor.
     */
    private ProxyConnectionReuseStrategy() {
    }

    @Override
    public boolean keepAlive(final HttpResponse response, final HttpContext context) {
        // only keep alive if no proxy was used, as the user can dynamically change
        // proxy settings, then we always want use newly configured connections
        final var route = (HttpRoute) context.getAttribute(HttpClientContext.HTTP_ROUTE);
        return (route == null || route.getProxyHost() == null) && super.keepAlive(response, context);
    }
}
