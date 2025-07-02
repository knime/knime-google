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
 *   Nov 15, 2023 (Leon Wenzler, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.port;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.port.PortSpecViewFactory;
import org.knime.core.webui.node.port.PortView;
import org.knime.core.webui.node.port.PortViewFactory;
import org.knime.core.webui.page.Page;
import org.knime.google.api.analytics.ga4.node.GAProperty;

/**
 * Same structure as <tt>org.knime.credentials.base.internal.PortViewFactories</tt>, but for the
 * {@link GAConnectionPortObject}.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui classes
public final class GAPortViewFactories {

    /**
     * {@link PortViewFactory} for the credential port object view.
     */
    static final PortViewFactory<GAConnectionPortObject> PORT_VIEW_FACTORY = //
        GAPortViewFactories::createPortView;

    /**
     * {@link PortSpecViewFactory} for the credential port object spec view.
     */
    static final PortSpecViewFactory<GAConnectionPortObjectSpec> PORT_SPEC_VIEW_FACTORY = //
        GAPortViewFactories::createPortSpecView;

    private static PortView createPortView(final GAConnectionPortObject portObject) {
        return new PortView() {
            @Override
            public Page getPage() {
                return Page.create().fromString(() -> createHtmlContent(portObject.getSpec()))
                    .relativePath("index.html");
            }

            @SuppressWarnings("unchecked")
            @Override
            public Optional<InitialDataService<?>> createInitialDataService() {
                return Optional.empty();
            }

            @Override
            public Optional<RpcDataService> createRpcDataService() {
                return Optional.empty();
            }
        };
    }

    /**
     * @param pos The port object spec.
     */
    private static PortView createPortSpecView(final GAConnectionPortObjectSpec pos) {
        return new PortView() {
            @Override
            public Page getPage() {
                return Page.create().fromString(() -> createHtmlContent(pos)).relativePath("index.html");
            }

            @SuppressWarnings("unchecked")
            @Override
            public Optional<InitialDataService<?>> createInitialDataService() {
                return Optional.empty();
            }

            @Override
            public Optional<RpcDataService> createRpcDataService() {
                return Optional.empty();
            }
        };
    }

    private static String createHtmlContent(final GAConnectionPortObjectSpec pos) {
        final var sb = new StringBuilder();
        sb.append("<html><head><style>\n");
        try (final var in = GAPortViewFactories.class.getResourceAsStream("table.css")) {
            sb.append(IOUtils.toString(in, StandardCharsets.UTF_8));
        } catch (IOException ignored) { // NOSONAR ignore, should always work
        }
        sb.append("</style></head><body>\n");
        sb.append(renderPortViewData(pos.getConnection(), pos.getProperty()));
        sb.append("</body></html>\n");
        return sb.toString();
    }

    private static String renderPortViewData(final GAConnection connection, final GAProperty property) {
        final var sb = new StringBuilder();
        sb.append("<table>\n");
        // table header using "Property" and "Value"
        sb.append("<tr>\n");
        sb.append(List.of("Property", "Value").stream()//
            .map(h -> String.format("<th>%s</th>%n", h))//
            .collect(Collectors.joining()));
        sb.append("</tr>\n");
        // adding GAProperty
        sb.append("<tr>\n");
        sb.append("<td>%s</td><td>%s</td>%n".formatted("GA4 property ID", property.getPropertyId()));
        sb.append("</tr>\n");
        // adding GAConnection settings
        connection.toString().lines().forEach(line -> {
            final var columns = line.split(": "); //NOSONAR
            sb.append("<tr>\n");
            sb.append(Arrays.stream(columns)//
                .map(h -> String.format("<td>%s</td>%n", h))//
                .collect(Collectors.joining()));
            sb.append("</tr>\n");
        });
        sb.append("</table>\n");
        return sb.toString();
    }

    private GAPortViewFactories() {
    }
}
