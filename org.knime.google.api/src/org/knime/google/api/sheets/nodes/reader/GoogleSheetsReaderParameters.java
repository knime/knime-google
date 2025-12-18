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

package org.knime.google.api.sheets.nodes.reader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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
import org.knime.google.api.sheets.nodes.reader.GoogleSheetsReaderParameters.SpreadSheetParameters.OpenInBrowserButtonRef;
import org.knime.google.api.sheets.nodes.reader.GoogleSheetsReaderParameters.SpreadSheetParameters.RefreshButtonRef;
import org.knime.google.api.sheets.nodes.reader.GoogleSheetsReaderParameters.SpreadSheetParameters.SpreadSheetIDRef;
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
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
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
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.model.Sheet;

/**
 * Node parameters for Google Sheets Reader.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class GoogleSheetsReaderParameters implements NodeParameters {

    @HorizontalLayout
    interface ButtonLayout {
    }

    @After(ButtonLayout.class)
    interface AfterButtonLayout {
    }

    @TextMessage(ConnectionMessageProvider.class)
    Void m_connectionSummary;

    @PersistWithin({"spreadsheet"})
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

        static final class SpreadSheetIDRef implements ParameterReference<String> {
        }

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
                    m_spreadSheetNameSupplier.get();
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
                    m_spreadSheetIDSupplier.get();
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
    @PersistWithin({"spreadsheet"})
    @Persist(configKey = "sheetName")
    @Widget(title = "Sheet", description = """
            The sheet from the spreadsheet that should be read.
            Available sheets can be selected from the drop-down menu.
            """)
    @ChoicesProvider(SheetNamesChoicesProvider.class)
    @ValueProvider(SheetNameProvider.class)
    @ValueReference(SheetNameRef.class)
    @Effect(predicate = UseFirstSheet.class, type = EffectType.DISABLE)
    String m_sheetName;

    static final class SheetNameRef implements ParameterReference<String> {
    }

    @Layout(AfterButtonLayout.class)
    @PersistWithin({"spreadsheet"})
    @Persist(configKey = "firstSheet")
    @Widget(title = "Select first sheet", description = """
            When selected, the first sheet of the spreadsheet will be read instead of the one selected from the
            drop-down menu.
            """)
    @ValueReference(UseFirstSheet.class)
    boolean m_firstSheet;

    static final class UseFirstSheet implements BooleanReference {
    }

    @Layout(AfterButtonLayout.class)
    @Persist(configKey = "readRange_BOOL")
    @Widget(title = "Select range", description = """
            Specify whether a range should be read from the sheet.
            """)
    @ValueReference(IsRangeEnabled.class)
    boolean m_enableRange;

    static final class IsRangeEnabled implements BooleanReference {
    }

    @Layout(AfterButtonLayout.class)
    @Persist(configKey = "readRange")
    @Widget(title = "Range", description = """
            The range that should be read from the sheet can be specified in A1 notation. (E.g. "A1:G20")
            """)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @Effect(predicate = IsRangeEnabled.class, type = EffectType.SHOW)
    String m_range = "";

    @Layout(AfterButtonLayout.class)
    @Persist(configKey = "hasColumnHeader")
    @Widget(title = "Read column names", description = """
            Specify whether the first row of the sheet should be read as column names.
            """)
    boolean m_hasColumnHeader = true;

    @Layout(AfterButtonLayout.class)
    @Persist(configKey = "hasRowHeader")
    @Widget(title = "Read row IDs", description = """
            Specify whether the first column of the sheet should be read as row IDs.
            """)
    boolean m_hasRowHeader = true;

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

    static final class SheetNameProvider implements StateProvider<String> {

        Supplier<List<String>> m_sheetNamesSupplier;

        Supplier<String> m_sheetNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_sheetNamesSupplier = initializer.computeFromProvidedState(SheetNamesProvider.class);
            m_sheetNameSupplier = initializer.getValueSupplier(SheetNameRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            final var sheetNames = m_sheetNamesSupplier.get();
            if (sheetNames == null || sheetNames.isEmpty()) {
                return m_sheetNameSupplier.get();
            }

            final var currentSheetName = m_sheetNameSupplier.get();
            if (currentSheetName != null) {
                return currentSheetName;
            }
            return sheetNames.get(0);
        }

    }

    static final class SheetNamesChoicesProvider implements StringChoicesProvider {

        Supplier<List<String>> m_sheetNamesSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_sheetNamesSupplier = initializer.computeFromProvidedState(SheetNamesProvider.class);
        }

        @Override
        public List<String> choices(final NodeParametersInput context) {
            final var sheetNames = m_sheetNamesSupplier.get();
            if (sheetNames == null) {
                return List.of();
            }
            return sheetNames;
        }

    }

    static final class SheetNamesProvider implements StateProvider<List<String>> {

        Supplier<String> m_spreadSheetIDSupplier;

        Supplier<String> m_sheetNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_spreadSheetIDSupplier = initializer.computeFromValueSupplier(SpreadSheetIDRef.class);
            m_sheetNameSupplier = initializer.getValueSupplier(SheetNameRef.class);
        }

        @Override
        public List<String> computeState(final NodeParametersInput context) {
            final var spreadSheetID = m_spreadSheetIDSupplier.get();
            if (spreadSheetID == null || spreadSheetID.isEmpty()) {
                return List.of(m_sheetNameSupplier.get());
            }

            final var sheetConnection = NodesUtil.getUncheckedSheetConnection(context);
            if (sheetConnection == null) {
                return List.of(m_sheetNameSupplier.get());
            }

            try {
                List<Sheet> sheets = sheetConnection.getSheetsService()
                        .spreadsheets().get(spreadSheetID).execute().getSheets();
                List<String> sheetNames = new ArrayList<>();
                sheets.forEach(sheet -> sheetNames.add(sheet.getProperties().getTitle()));
                return sheetNames;
            } catch (IOException | NoSuchCredentialException e) {
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

    static GoogleSheetsReaderModel getNodeModel() {
        NodeContext nodeContext = NodeContext.getContext();
        final var nodeContainer = nodeContext.getNodeContainer();
        final var nativeNodeContainer = (NativeNodeContainer)nodeContainer;
        final var nodeModel = nativeNodeContainer.getNodeModel();
        return (GoogleSheetsReaderModel)nodeModel;
    }

}
