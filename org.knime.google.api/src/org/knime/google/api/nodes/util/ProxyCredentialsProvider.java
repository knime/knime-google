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
 *   Jun 24, 2024 (lw): created
 */
package org.knime.google.api.nodes.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.proxy.GlobalProxyConfig;
import org.knime.core.util.proxy.search.GlobalProxySearch;

import com.google.api.services.drive.Drive;

/**
 * Basic credentials provider that first queries the {@link GlobalProxySearch},
 * then checks its stored {@link Credentials}.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
final class ProxyCredentialsProvider extends BasicCredentialsProvider {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ProxyCredentialsProvider.class);

    private static final URI DEFAULT_ROOT_URI = URI.create(Drive.DEFAULT_ROOT_URL);

    /**
     * Creates a {@link URI} based on an {@link AuthScope} instance.
     *
     * @param authScope scope
     * @return constructed URI, defaults to Google APIs address
     */
    private static URI createURIFromAuthScope(final AuthScope authScope) {
        if (authScope == AuthScope.ANY) {
            return DEFAULT_ROOT_URI;
        }
        final var httpHost = authScope.getOrigin() != null ? authScope.getOrigin()
            : new HttpHost(authScope.getHost(), authScope.getPort());
        try {
            return ProxyHttpRoutePlanner.createURIFromHttpHost(httpHost);
        } catch (URISyntaxException e) {
            LOGGER.warn(() -> "Could not create URI target for proxy search on input \"%s\", defaulting to target: %s"
                .formatted(httpHost, DEFAULT_ROOT_URI), e);
            return DEFAULT_ROOT_URI;
        }
    }

    @Override
    public Credentials getCredentials(final AuthScope authScope) {
        return GlobalProxySearch.getCurrentFor(createURIFromAuthScope(authScope)) //
            .map(GlobalProxyConfig::forApacheHttpClient) //
            .map(p -> p.getSecond().getCredentials(authScope)) //
            .filter(Objects::nonNull) //
            .orElse(super.getCredentials(authScope));
    }
}
