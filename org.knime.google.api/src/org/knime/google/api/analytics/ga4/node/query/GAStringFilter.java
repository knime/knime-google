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
 *   4 May 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.query;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Filter dimension values based on string comparison.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui* classes
final class GAStringFilter implements DefaultNodeSettings {

    static final String[][] CONFIG_PATHS = new String[][]{{"matchType"}, {"value"}};

    @Widget(title = "String matching", description = """
            Specify how the string value is matched with the dimension value.
            <ul>
                <li><b>Exact:</b> the string value must match the dimension value exactly
                </li>
                <li><b>Begins with:</b> the dimension value must begin with the string value
                </li>
                <li><b>Ends with:</b> the dimension value must end with the string value
                </li>
                <li><b>Contains:</b> the dimension value must contain the string value
                </li>
                <li><b>Regular expression (full match):</b> the string value is interpreted as a regular expression and
                    must match the dimension value completely
                </li>
                <li><b>Regular expression (partial match):</b> the string value is interpreted as a regular expression
                    and must match the dimension value at least partially
                </li>
            </ul>
            """)
    MatchType m_matchType = MatchType.CONTAINS;

    @Widget(title = "String value", description = "Specify the string value to match to the dimension value.")
    String m_value;

    /**
     * String matching types as supported by Google Analytics 4 Data API.
     */
    enum MatchType {
        @Label("Exact")
        EXACT,
        @Label("Begins with")
        BEGINS_WITH,
        @Label("Ends with")
        ENDS_WITH,
        @Label("Contains")
        CONTAINS,
        /**
         * For instance {@code .*dam} fully matches {@code Amsterdam} and {@code Rotterdam} but not {@code Amsterdam2}
         */
        @Label("Regular expression (full match)") // , description = "Matches if the whole string matches the regular expression.")
        FULL_REGEXP,
        /**
         * For instance {@code (dap|omb)} partially matches {@code Budapest} and {@code Colombo}.
         */
        @Label("Regular expression (partial match)") // , description = "Matches if part of the string matches the regular expression.")
        PARTIAL_REGEXP;
    }

    GAStringFilter() {
        // ser/de
    }

    GAStringFilter(final MatchType matchType, final String value) {
        m_matchType = Objects.requireNonNull(matchType);
        m_value = Objects.requireNonNull(value);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(m_matchType, "Match type is missing.");
        CheckUtils.checkSetting(StringUtils.isNotBlank(m_value), "Value is missing.");
    }


}
