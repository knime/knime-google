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
 *   14.02.2025 (loescher): created
 */
package org.knime.google.api.sheets.nodes.util;

import java.util.Set;
import java.util.function.BiConsumer;

import org.knime.core.node.NodeModel;

/**
 * Contains utilities to handle functionality shared by multiple Google Sheets Nodes
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
public final class NodesUtil {

    private static final String VAR_SPREADSHEET_NAME = "spreadsheetName";
    private static final String VAR_SPREADSHEET_ID = "spreadsheetId";
    private static final String VAR_SHEET_NAME = "sheetName";

    private NodesUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Pushes the given information to the flow variable stack making the name unique if it already exists.
     *
     * @param spreadsheetName The spreadsheet name
     * @param spreadsheetId spreadsheeId
     * @param sheetName sheetName
     * @param variableNames all names currently in use
     * @param push the {@link NodeModel#peekFlowVariableString(String)} method of the calling node
     *
     */
    public static void pushFlowVariables(final String spreadsheetName, final String spreadsheetId,
        final String sheetName, final Set<String> variableNames, final BiConsumer<String, String> push) {

        var suffix = "";
        if (contains(variableNames, "")) {
            var i = 1;
            while (contains(variableNames, "_" + i)) {
                i++;
            }
            suffix = "_" + i;
        }

        push.accept(VAR_SPREADSHEET_NAME + suffix, spreadsheetName);
        push.accept(VAR_SPREADSHEET_ID + suffix, spreadsheetId);
        push.accept(VAR_SHEET_NAME + suffix, sheetName);
    }

    private static boolean contains(final Set<String> variableNames, final String suffix) {
        return variableNames.contains(VAR_SPREADSHEET_NAME + suffix) || //
                variableNames.contains(VAR_SPREADSHEET_ID + suffix) || //
                variableNames.contains(VAR_SHEET_NAME + suffix);
    }

}
