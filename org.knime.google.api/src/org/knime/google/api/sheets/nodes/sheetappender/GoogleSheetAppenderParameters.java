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

package org.knime.google.api.sheets.nodes.sheetappender;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.util.DesktopUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.google.api.sheets.nodes.sheetappender.GoogleSheetAppenderParameters.SpreadSheetParameters.OpenInBrowserButtonRef;
import org.knime.google.api.sheets.nodes.sheetappender.GoogleSheetAppenderParameters.SpreadSheetParameters.RefreshButtonRef;
import org.knime.google.api.sheets.nodes.sheetappender.GoogleSheetAppenderParameters.SpreadSheetParameters.SpreadSheetIDRef;
import org.knime.google.api.sheets.nodes.util.NodesUtil;
import org.knime.google.api.sheets.nodes.util.NodesUtil.AreButtonsEnabled;
import org.knime.google.api.sheets.nodes.util.NodesUtil.ConnectionMessageProvider;
import org.knime.google.api.sheets.nodes.util.NodesUtil.DoNotPersistString;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

import com.google.api.services.drive.model.File;

/**
 * Node parameters for Google Sheets Appender.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class GoogleSheetAppenderParameters implements NodeParameters {

    @HorizontalLayout
    interface ButtonLayout {
    }

    @After(ButtonLayout.class)
    interface AfterButtonLayout {
    }

    @Section(title = "Write Settings")
    @After(AfterButtonLayout.class)
    interface WriteSettingsSection {
    }

    @TextMessage(ConnectionMessageProvider.class)
    Void m_connectionSummary;

    @PersistWithin({"spreadsheetChooser"})
    @Persistor(SpreadSheetParameterPersistor.class)
    SpreadSheetParameters m_spreadSheetIdentifierParameters = new SpreadSheetParameters();

    static final class SpreadSheetParameterRef implements ParameterReference<SpreadSheetParameters> {
    }

    static final class SpreadSheetParameters implements NodeParameters {

        SpreadSheetParameters() {
            this(null, null);
        }

        SpreadSheetParameters(final String spreadSheetId, final String spreadSheetName) {
            m_spreadSheetId = spreadSheetId;
            m_spreadSheetName = spreadSheetName;
        }

        // Selects the spread sheet via name in the UI, but sets the ID in the back-end
        @Widget(title = "Spreadsheet", description = """
                The spreadsheet can be selected from the spreadsheets available on Google Drive.
                """)
        @ChoicesProvider(SpreadSheetChoicesProvider.class)
        @ValueReference(SpreadSheetIDRef.class)
        @ValueProvider(SpreadSheetIDProvider.class)
        String m_spreadSheetId;

        @Layout(ButtonLayout.class)
        @Widget(title = "Refresh", description = """
                Refreshes the list of available spreadsheets from Google Drive.
                """)
        @SimpleButtonWidget(ref = RefreshButtonRef.class)
        @Effect(predicate = AreButtonsEnabled.class, type = EffectType.ENABLE)
        Void m_refreshButton;

        static final class RefreshButtonRef implements ButtonReference {
        }

        @Layout(ButtonLayout.class)
        @Widget(title = "Open in browser", description = """
                Opens the selected spreadsheet in the browser.
                """)
        @SimpleButtonWidget(ref = OpenInBrowserButtonRef.class)
        @ValueProvider(OpenInBrowserButtonAction.class)
        @Effect(predicate = AreButtonsEnabled.class, type = EffectType.ENABLE)
        Void m_openInBrowserButton;

        static final class OpenInBrowserButtonRef implements ButtonReference {
        }

        static final class SpreadSheetIDRef implements ParameterReference<String> {
        }

        @Persistor(DoNotPersistString.class)
        @ValueReference(SpreadSheetRef.class)
        @ValueProvider(SpreadSheetProvider.class)
        String m_spreadSheet;

        static final class SpreadSheetRef implements ParameterReference<String> {
        }

        // Spread sheet name is visible via display text from the spread sheet choices provider.
        @ValueReference(SpreadSheetNameRef.class)
        @ValueProvider(SpreadSheetNameProvider.class)
        String m_spreadSheetName;

        static final class SpreadSheetNameRef implements ParameterReference<String> {
        }

        static final class SpreadSheetNameProvider implements StateProvider<String> {

            Supplier<String> m_spreadSheetSupplier;

            Supplier<String> m_spreadSheetNameSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_spreadSheetSupplier = initializer.computeFromValueSupplier(SpreadSheetRef.class);
                m_spreadSheetNameSupplier = initializer.getValueSupplier(SpreadSheetNameRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput context) throws StateComputationFailureException {
                final var spreadSheet = m_spreadSheetSupplier.get();
                if (spreadSheet == null || spreadSheet.isEmpty()) {
                    return m_spreadSheetNameSupplier.get();
                }
                return NodesUtil.getSpreadSheetIDAndName(spreadSheet).getRight();
            }

        }

        static final class SpreadSheetIDProvider implements StateProvider<String> {

            Supplier<String> m_spreadSheetSupplier;

            Supplier<String> m_spreadSheetIDSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_spreadSheetSupplier = initializer.computeFromValueSupplier(SpreadSheetRef.class);
                m_spreadSheetIDSupplier = initializer.getValueSupplier(SpreadSheetIDRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput context) throws StateComputationFailureException {
                final var spreadSheet = m_spreadSheetSupplier.get();
                if (spreadSheet == null || spreadSheet.isEmpty()) {
                    return m_spreadSheetIDSupplier.get();
                }
                return NodesUtil.getSpreadSheetIDAndName(spreadSheet).getLeft();
            }

        }

        static final class SpreadSheetProvider implements StateProvider<String> {

            Supplier<List<StringChoice>> m_spreadSheetsSupplier;

            Supplier<String> m_spreadSheetIdSupplier;

            Supplier<String> m_spreadSheetNameSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_spreadSheetsSupplier = initializer.computeFromProvidedState(SpreadSheetChoicesProvider.class);
                m_spreadSheetIdSupplier = initializer.getValueSupplier(SpreadSheetIDRef.class);
                m_spreadSheetNameSupplier = initializer.getValueSupplier(SpreadSheetNameRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput context) throws StateComputationFailureException {
                final var spreadSheetID = m_spreadSheetIdSupplier.get();
                final var spreadSheetName = m_spreadSheetNameSupplier.get();
                if (spreadSheetID != null && !spreadSheetID.isEmpty()
                        && spreadSheetName != null && !spreadSheetName.isEmpty()) {
                    return "%s (%s)".formatted(spreadSheetName, spreadSheetID);
                }
                final var spreadSheets = m_spreadSheetsSupplier.get();
                if (spreadSheets == null || spreadSheets.isEmpty()) {
                    return null;
                }
                return "%s (%s)".formatted(spreadSheets.get(0).text(), spreadSheets.get(0).id());
            }

        }

        static final class SpreadSheetChoicesProvider implements StringChoicesProvider {

            Supplier<List<File>> m_spreadSheetsListSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_spreadSheetsListSupplier =
                        initializer.computeFromProvidedState(SpreadSheetsRefresher.class);
            }

            @Override
            public List<StringChoice> computeState(final NodeParametersInput context) {
                final var spreadSheets = m_spreadSheetsListSupplier.get();
                if (spreadSheets == null || spreadSheets.isEmpty()) {
                    return List.of();
                }
                return spreadSheets.stream().map(file -> new StringChoice(file.getId(), file.getName())).toList();
            }

        }

    }

    @Layout(AfterButtonLayout.class)
    @Persist(configKey = "sheetName")
    @Widget(title = "Sheet name", description = """
            The name of the sheet to which the table should be written.
            """)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    String m_sheetName = "";

    @Layout(WriteSettingsSection.class)
    @Persist(configKey = "writeColName")
    @Widget(title = "Add column header", description = """
            Here you determine whether the column names should be written in the first row.
            """)
    boolean m_addColumnHeader = true;

    @Layout(WriteSettingsSection.class)
    @Persist(configKey = "writeRowId")
    @Widget(title = "Add row header", description = """
            Here you determine whether the row ID's should be written in the first column.
            """)
    boolean m_addRowHeader = true;

    @Layout(WriteSettingsSection.class)
    @Persist(configKey = "missingValue_BOOL")
    @Widget(title = "Fill in missing values", description = """
            By selecting this option, you can specify a string you want to substitute for missing values.
            If the option is left unchecked, the cells with missing values remain empty.
            """)
    @ValueReference(IsMissingValuePatternEnabled.class)
    boolean m_enableMissingValuePattern;

    static final class IsMissingValuePatternEnabled implements BooleanReference {
    }

    @Layout(WriteSettingsSection.class)
    @Persist(configKey = "missingValue")
    @Widget(title = "Missing value substitute", description = """
            The value which is substituted for missing values.
            """)
    @Effect(predicate = IsMissingValuePatternEnabled.class, type = EffectType.SHOW)
    String m_missingValuePattern;

    @Layout(WriteSettingsSection.class)
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

    @Layout(WriteSettingsSection.class)
    @Persist(configKey = "createUniqueSheetName")
    @Widget(title = "Create unique sheet name", description = """
            The node will create a unique sheet name based on the given sheet name.
            (Example: Should 'SheetOne' already exist, the unique sheet name will be 'SheetOne (#1)')
            """)
    @ValueReference(CreateUniqueSheetName.class)
    boolean m_createUniqueSheetName = true;

    static final class CreateUniqueSheetName implements BooleanReference {
    }

    @Layout(WriteSettingsSection.class)
    @Persist(configKey = "openAfterExecution")
    @Widget(title = "Open spreadsheet after execution", description = """
            Opens the spreadsheet after it has been written successfully. The spreadsheet will be opened in
            the system's default browser.
            """)
    boolean m_openAfterExecution;

    @Layout(WriteSettingsSection.class)
    @Persistor(AppenderColumnsSelectionPersistor.class)
    @Widget(title = "Exclude/Include columns", description = """
            Select the columns that will be written to the sheet file.
            If the columns in the input table change, they will automatically be excluded.
            """)
    @ChoicesProvider(AllColumnsProviderPort1.class)
    ColumnFilter m_appenderColumnsSelection;

    static final class SpreadSheetsRefresher implements StateProvider<List<File>> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            initializer.computeOnButtonClick(RefreshButtonRef.class);
        }

        @Override
        public List<File> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var sheetConnection = NodesUtil.getCheckedSheetConnection(parametersInput);
            try {
                return getNodeModel().computeSpreadSheetsEntry(sheetConnection);
            } catch (ExecutionException e) {
                throw ExceptionUtils.asRuntimeException(e);
            }
        }

    }

    static final class OpenInBrowserButtonAction implements StateProvider<Void> {

        Supplier<String> m_spreadSheetIDSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(OpenInBrowserButtonRef.class);
            m_spreadSheetIDSupplier = initializer.getValueSupplier(SpreadSheetIDRef.class);
        }

        @Override
        public Void computeState(final NodeParametersInput context) throws StateComputationFailureException {
            final var spreadSheetID = m_spreadSheetIDSupplier.get();
            if (spreadSheetID == null || spreadSheetID.isEmpty()) {
                throw new StateComputationFailureException();
            }
            final var sheetConnection = NodesUtil.getCheckedSheetConnection(context);

            try {
                final var spreadsheetUrlString = sheetConnection.getSheetsService().spreadsheets()
                    .get(spreadSheetID).execute().getSpreadsheetUrl();
                final var spreadsheetUrl = new URL(spreadsheetUrlString);
                DesktopUtil.browse(spreadsheetUrl);
            } catch (IOException | NoSuchCredentialException e) {
                throw ExceptionUtils.asRuntimeException(e);
            }

            return null;
        }

    }

    static final class AllColumnsProviderPort1 extends AllColumnsProvider {

        @Override
        public int getInputTableIndex() {
            return 1;
        }

    }

    static final class SpreadSheetParameterPersistor implements NodeParametersPersistor<SpreadSheetParameters> {

        @Override
        public SpreadSheetParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var spreadSheetID = settings.getString("spreadsheetId");
            final var spreadSheetName = settings.getString("spreadsheetName");
            return new SpreadSheetParameters(spreadSheetID, spreadSheetName);
        }

        @Override
        public void save(final SpreadSheetParameters param, final NodeSettingsWO settings) {
            settings.addString("spreadsheetId", param.m_spreadSheetId);
            settings.addString("spreadsheetName", param.m_spreadSheetName);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"spreadsheetId"}, {"spreadsheetName"}};
        }

    }

    static final class AppenderColumnsSelectionPersistor extends LegacyColumnFilterPersistor {

        protected AppenderColumnsSelectionPersistor() {
            super("columnFilter");
        }

    }

    static GoogleSheetAppenderModel getNodeModel() {
        NodeContext nodeContext = NodeContext.getContext();
        final var nodeContainer = nodeContext.getNodeContainer();
        final var nativeNodeContainer = (NativeNodeContainer)nodeContainer;
        final var nodeModel = nativeNodeContainer.getNodeModel();
        return (GoogleSheetAppenderModel)nodeModel;
    }

}
