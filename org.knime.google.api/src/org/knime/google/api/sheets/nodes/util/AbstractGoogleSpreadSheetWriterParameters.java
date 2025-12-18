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
 *   Jan 8, 2026 (magnus): created
 */
package org.knime.google.api.sheets.nodes.util;

import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

/**
 * Node parameters for Google Spreadsheet Writer nodes.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class AbstractGoogleSpreadSheetWriterParameters extends AbstractGoogleSpreadSheetParameters {

    /**
     * The write settings section.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    @Section(title = "Write Settings")
    @After(AfterButtonLayout.class)
    public interface WriteSettingsSection {

        /**
         * The middle part of the write settings section.
         *
         * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
         */
        interface Middle {
        }

        /**
         * The end part of the write settings section.
         *
         * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
         */
        @After(Middle.class)
        interface End {
        }

    }

    @Layout(WriteSettingsSection.Middle.class)
    @Persist(configKey = "writeColName")
    @Widget(title = "Add column header", description = """
            Here you determine whether the column names should be written in the first row.
            """)
    boolean m_addColumnHeader = true;

    @Layout(WriteSettingsSection.Middle.class)
    @Persist(configKey = "writeRowId")
    @Widget(title = "Add row header", description = """
            Here you determine whether the RowIDs should be written in the first column.
            """)
    boolean m_addRowHeader = true;

    @Layout(WriteSettingsSection.Middle.class)
    @Persist(configKey = "missingValue_BOOL")
    @Widget(title = "Fill in missing values", description = """
            By selecting this option, you can specify a string you want to substitute for missing values.
            If the option is left unchecked, the cells with missing values remain empty.
            """)
    @ValueReference(IsMissingValuePatternEnabled.class)
    boolean m_enableMissingValuePattern;

    static final class IsMissingValuePatternEnabled implements BooleanReference {
    }

    @Layout(WriteSettingsSection.Middle.class)
    @Persist(configKey = "missingValue")
    @Widget(title = "Missing value substitute", description = """
            The value which is substituted for missing values.
            """)
    @Effect(predicate = IsMissingValuePatternEnabled.class, type = EffectType.SHOW)
    String m_missingValuePattern;

    @Layout(WriteSettingsSection.Middle.class)
    @Persist(configKey = "writeRaw")
    @Widget(title = "Write raw (do not parse numbers, dates, hyperlinks, etc.)", description = """
            Values are written into the spreadsheet as-is ("raw"), i.e. they will <i>not</i> be parsed.
            Uncheck the option, if values should be written into the spreadsheet as if they were entered via
            the Google Sheets website. If unchecked, numbers will stay as numbers, but other strings may be
            converted to numbers, dates, etc. following the same rules that are applied when entering text
            into a cell via the Google Sheets website. For example, strings like
            <tt>=hyperlink("example.com", "example")</tt> will be parsed to hyperlinks if this option is
            unchecked.
            """)
    boolean m_writeRaw = true;

    @Layout(WriteSettingsSection.End.class)
    @Persist(configKey = "openAfterExecution")
    @Widget(title = "Open spreadsheet after execution", description = """
            Opens the spreadsheet after it has been written successfully. The spreadsheet will be opened in
            the system's default browser.
            """)
    boolean m_openAfterExecution;

    @Layout(WriteSettingsSection.End.class)
    @Persistor(AppenderColumnsSelectionPersistor.class)
    @Widget(title = "Exclude/Include columns", description = """
            Select the columns that will be written to the sheet file.
            If the columns in the input table change, they will automatically be excluded.
            """)
    @ChoicesProvider(AllColumnsProviderPort1.class)
    ColumnFilter m_appenderColumnsSelection;

    static final class AllColumnsProviderPort1 extends AllColumnsProvider {

        @Override
        public int getInputTableIndex() {
            return 1;
        }

    }

    static final class AppenderColumnsSelectionPersistor extends LegacyColumnFilterPersistor {

        protected AppenderColumnsSelectionPersistor() {
            super("columnFilter");
        }

    }

}
