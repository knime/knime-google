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
 *   07.02.2025 (loescher): created
 */
package org.knime.google.api.sheets.nodes.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.http.GenericUrl;

/**
 * Contains utilities to handle ranges in Google sheet nodes correctly
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
public final class RangeUtil {

    private RangeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Executes a query with a range in the last segment and additionally escapes some characters in that segment.
     * This is done as a workaround for a bug in the Google API server backend which seems to tread the path
     * parameter as a query parameter.
     *
     * @param <T> the return type of the query
     * @param request the unexecuted request to escape and execute
     * @return the request result
     * @throws IOException if there was an error executing the quest or parsing the result
     * @see AbstractGoogleClientRequest#execute()
     */
    public static <T> T escapedRangeExecute(final AbstractGoogleClientRequest<T> request) throws IOException {
        if (request.getMediaHttpUploader() != null) {
            throw new IllegalArgumentException("Cannot handle request with an uploader.");
        }

        final var req = request.buildHttpRequest();

        final var url = req.getUrl();

        final var urlParts = url.getPathParts();
        final var last = urlParts.remove(urlParts.size() -1);
        final var newUrl = new GenericUrl(url.toURL(), true); // mark as already escaped
        final var raw = URLEncoder.encode(last, StandardCharsets.UTF_8)
                .replace("+", "%20") // we don't know whether the path will be cont to be read as query param, be safe
                .replace("%3A", ":"); // colon may have special meaning (":append") so it we cannot escape it
        newUrl.appendRawPath("/" + raw);
        req.setUrl(newUrl);

        return req.execute().parseAs(request.getResponseClass());
    }

    /**
     * Quote a sheet name so that it is not interpreted as a range.
     *
     * @param name the name to quote
     * @return the quoted name
     */
    public static String quoteSheetName(final String name) {
        return String.format("'%s'", name);
    }
}
