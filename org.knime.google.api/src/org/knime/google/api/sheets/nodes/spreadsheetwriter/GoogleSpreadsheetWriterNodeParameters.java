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
 * ------------------------------------------------------------------------
 */

package org.knime.google.api.sheets.nodes.spreadsheetwriter;

import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelOptionalStringPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelStringPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Google Sheets Writer.
 *
 * @author Ali Asghar Marvi, KNIME GmbH, Konstanz, Germany
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class GoogleSpreadsheetWriterNodeParameters implements NodeParameters {

    @Section(title = "Spreadsheet Settings")
    interface SpreadsheetSettings {
    }

    @Section(title = "Write Settings")
    @After(SpreadsheetSettings.class)
    interface WriteSettings {
    }

    @Section(title = "Outputs")
    @After(WriteSettings.class)
    interface ColumnAndOptionsSelection {
    }

    static final class SpreadsheetNamePersistor extends SettingsModelStringPersistor {
        SpreadsheetNamePersistor() {
            super("spreadsheetName");
        }
    }

    @Layout(SpreadsheetSettings.class)
    @Widget(title = "Spreadsheet name", description = "The name of the spreadsheet to be created.")
    @TextInputWidget(placeholder = "Enter spreadsheet name")
    @Persistor(SpreadsheetNamePersistor.class)
    String m_spreadsheetName = "";

    static final class SheetNamePersistor extends SettingsModelStringPersistor {
        SheetNamePersistor() {
            super("sheetName");
        }
    }

    @Layout(SpreadsheetSettings.class)
    @Widget(title = "Sheet name", description = "The name of the sheet to which the table should be written.")
    @TextInputWidget(placeholder = "Enter sheet name")
    @Persistor(SheetNamePersistor.class)
    String m_sheetName = "";

    static final class AddColumnHeaderPersistor extends SettingsModelBooleanPersistor {
        AddColumnHeaderPersistor() {
            super("writeColName");
        }
    }

    @Layout(WriteSettings.class)
    @Widget(title = "Add column header",
        description = "Here you determine whether the column names should be written in the first row.")
    @Persistor(AddColumnHeaderPersistor.class)
    boolean m_addColumnHeader = true;

    static final class AddRowHeaderPersistor extends SettingsModelBooleanPersistor {
        AddRowHeaderPersistor() {
            super("writeRowId");
        }
    }

    @Layout(WriteSettings.class)
    @Widget(title = "Add row header",
        description = "Here you determine whether the row ID's should be written in the first column.")
    @Persistor(AddRowHeaderPersistor.class)
    boolean m_addRowHeader = true;

    static final class MissingValuePersistor extends SettingsModelOptionalStringPersistor {
        MissingValuePersistor() {
            super("missingValue");
        }
    }

    @Layout(WriteSettings.class)
    @Widget(title = "For missing values write", description = """
            By selecting this option, you can specify a string you want to substitute for missing values.
            If the option is left unchecked, the cells with missing values remain empty.
            """)
    @Persistor(MissingValuePersistor.class)
    Optional<String> m_missingValueReplacement = Optional.empty();

    static final class WriteRawPersistor extends SettingsModelBooleanPersistor {
        WriteRawPersistor() {
            super("writeRaw");
        }
    }

    @Layout(WriteSettings.class)
    @Widget(title = "Write raw values", description = """
            Values are written into the spreadsheet as-is ("raw"), i.e. numbers, dates, hyperlinks, etc. will not
            be parsed. Uncheck the option, if values should be written into the spreadsheet as if they were entered
            via the Google Sheets website. If unchecked, numbers will stay as numbers, but other strings may be
            converted to numbers, dates, etc. following the same rules that are applied when entering text into a
            cell via the Google Sheets website. For example, strings like =hyperlink("example.com", "example")
            will be parsed to hyperlinks if this option is unchecked.
            """)
    @Persistor(WriteRawPersistor.class)
    boolean m_writeRaw = true;

    static final class OpenAfterExecutionPersistor extends SettingsModelBooleanPersistor {
        OpenAfterExecutionPersistor() {
            super("openAfterExecution");
        }
    }

    @Layout(ColumnAndOptionsSelection.class)
    @Widget(title = "Open spreadsheet after execution", description = """
            Opens the spreadsheet after it has been written successfully.
            The spreadsheet will be opened in the systems's default browser.
            """)
    @Persistor(OpenAfterExecutionPersistor.class)
    boolean m_openAfterExecution;

    /**
     * Custom column provider that uses the second input port (index 1) for the data table.
     */
    static final class AllColumnsFromSecondPortProvider extends AllColumnsProvider {
        @Override
        public int getInputTableIndex() {
            return 1; // Second input port for the data table
        }
    }

    /**
     * Custom persistor for column filter that maps to the legacy SettingsModelColumnFilter2.
     */
    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterPersistor() {
            super("columnFilter");
        }
    }

    @Layout(ColumnAndOptionsSelection.class)
    @Widget(title = "Exclude/Include columns",
        description = "Select the columns that will be written to the sheet file. "
            + "If the columns in the input table change, they will automatically be excluded.")
    @ColumnFilterWidget(choicesProvider = AllColumnsFromSecondPortProvider.class)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_columnFilter = new ColumnFilter();
}
