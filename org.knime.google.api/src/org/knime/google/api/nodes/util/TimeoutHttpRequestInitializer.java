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
 *   May 15, 2024 (bjoern): created
 */
package org.knime.google.api.nodes.util;

import java.io.IOException;
import java.time.Duration;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;

/**
 * {@link HttpRequestInitializer} that sets HTTP connect/read timeout. It also allows to wrap an inner
 * {@link HttpRequestInitializer}, e.g. one that adds credentials to the request.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class TimeoutHttpRequestInitializer implements HttpRequestInitializer {

    private final Duration m_connectTimeout;

    private final Duration m_readTimeout;

    private final HttpRequestInitializer m_wrapped;

    /**
     * Constructor.
     *
     * @param connectTimeout The HTTP connect timeout to use for each request.
     * @param readTimeout The HTTP read timeout to use for each request.
     */
    public TimeoutHttpRequestInitializer(final Duration connectTimeout, final Duration readTimeout) {
        this(connectTimeout, readTimeout, null);
    }

    /**
     * Constructor that allows to pass a {@link HttpRequestInitializer} to be wrapped.
     *
     * @param connectTimeout The HTTP connect timeout to use for each request.
     * @param readTimeout The HTTP read timeout to use for each request.
     * @param wrapped A {@link HttpRequestInitializer} to be wrapped.
     */
    public TimeoutHttpRequestInitializer(final Duration connectTimeout, final Duration readTimeout,
        final HttpRequestInitializer wrapped) {

        m_connectTimeout = connectTimeout;
        m_readTimeout = readTimeout;
        m_wrapped = wrapped;
    }

    @Override
    public void initialize(final HttpRequest request) throws IOException {
        if (m_wrapped != null) {
            m_wrapped.initialize(request);
        }
        request.setConnectTimeout((int)m_connectTimeout.toMillis());
        request.setReadTimeout((int)m_readTimeout.toMillis());
    }
}
