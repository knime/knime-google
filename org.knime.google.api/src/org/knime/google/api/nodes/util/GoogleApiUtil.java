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
 *   Oct 30, 2023 (bjoern): created
 */
package org.knime.google.api.nodes.util;

import java.time.Duration;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

/**
 * Provides a {@link HttpTransport} and {@link JsonFactory} for use in the Google nodes.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class GoogleApiUtil {

    /**
     * Default timeout for establishing HTTP connections.
     */
    public static final Duration DEFAULT_HTTP_CONNECT_TIMEOUT = Duration.ofMinutes(1);

    /**
     * Default timeout for reading from HTTP connections.
     */
    public static final Duration DEFAULT_HTTP_READ_TIMEOUT = Duration.ofMinutes(2);

    private static final HttpTransport DEFAULT_HTTP_TRANSPORT = new ApacheHttpTransport( //
        ApacheHttpTransport.newDefaultHttpClientBuilder() //
        .setRoutePlanner(ProxyHttpRoutePlanner.INSTANCE) //
        .setDefaultCredentialsProvider(ProxyCredentialsProvider.INSTANCE) //
        .setConnectionReuseStrategy(ProxyConnectionReuseStrategy.INSTANCE) //
        .build());

    private static final JsonFactory JSON_FACTORY = new GsonFactory();

    private static final HttpRequestInitializer DEFAULT_HTTP_REQUEST_INITIALIZER =
        new TimeoutHttpRequestInitializer(DEFAULT_HTTP_CONNECT_TIMEOUT, DEFAULT_HTTP_READ_TIMEOUT);

    private GoogleApiUtil() {
    }

    /**
     * @return the default {@link HttpTransport} instance
     */
    public static HttpTransport getHttpTransport() {
        return DEFAULT_HTTP_TRANSPORT;
    }

    /**
     *
     * @return the default {@link HttpRequestInitializer} instance to use
     */
    public static HttpRequestInitializer getDefaultHttpRequestInitializer() {
        return DEFAULT_HTTP_REQUEST_INITIALIZER;
    }

    /**
     * @param connectTimeout The HTTP connect timeout to use.
     * @param readTimeout The HTTP read timeout to use.
     * @return a {@link HttpRequestInitializer} instance that sets HTTP connect/read timeouts.
     */
    public static HttpRequestInitializer getHttpRequestInitializerWithTimeouts(final Duration connectTimeout,
        final Duration readTimeout) {
        return new TimeoutHttpRequestInitializer(connectTimeout, readTimeout);
    }

    /**
     * @return the {@link JsonFactory} instance that should be used by all nodes.
     */
    public static JsonFactory getJsonFactory() {
        return JSON_FACTORY;
    }
}
