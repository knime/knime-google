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
 *   Jan 7, 2026 (magnus): created
 */
package org.knime.google.api.sheets.nodes.util;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.DesktopUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.google.api.sheets.nodes.util.AbstractGoogleSpreadSheetParameters.BeforeButtonLayout;
import org.knime.google.api.sheets.nodes.util.AbstractGoogleSpreadSheetParameters.ButtonLayout;
import org.knime.google.api.sheets.nodes.util.AbstractGoogleSpreadSheetParameters.SpreadSheetsRefresher;
import org.knime.google.api.sheets.nodes.util.NodesUtil.AreButtonsEnabled;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.AutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

import com.google.api.services.drive.model.File;

/**
 * Node parameters for Google Sheets spreadsheet selection.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class SpreadSheetParameters implements NodeParameters {

    /**
     * Default constructor.
     */
    public SpreadSheetParameters() {
        this(null, null);
    }

    /**
     * Constructor with initial values.
     *
     * @param spreadSheetId the spread sheet ID
     * @param spreadSheetName the spread sheet name
     */
    public SpreadSheetParameters(final String spreadSheetId, final String spreadSheetName) {
        m_spreadSheetId = spreadSheetId;
        m_spreadSheetName = spreadSheetName;
    }

    // Selects the spread sheet via name in the UI, but sets the ID in the back-end
    @Layout(BeforeButtonLayout.class)
    @Widget(title = "Spreadsheet", description = """
            The spreadsheet can be selected from the spreadsheets available on Google Drive.
            """)
    @ChoicesProvider(SpreadSheetChoicesProvider.class)
    @ValueProvider(SpreadSheetIDProvider.class)
    @ValueReference(SpreadSheetIDRef.class)
    String m_spreadSheetId;

    /**
     * The reference for the spread sheet ID.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    static final class SpreadSheetIDRef implements ParameterReference<String> {
    }

    static final class SpreadSheetIDProvider extends AutoGuessValueProvider<String> {

        SpreadSheetIDProvider() {
            super(SpreadSheetIDRef.class);
        }

        Supplier<List<StringChoice>> m_spreadSheetsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_spreadSheetsSupplier = initializer.computeFromProvidedState(SpreadSheetChoicesProvider.class);
            super.init(initializer);
        }

        @Override
        protected boolean isEmpty(final String value) {
            return value == null || value.isEmpty();
        }

        @Override
        protected String autoGuessValue(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return m_spreadSheetsSupplier.get().stream().findFirst().map(StringChoice::id).orElse(null);
        }

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

    // Spread sheet name is visible via display text from the spread sheet choices provider.
    @ValueReference(SpreadSheetNameRef.class)
    @ValueProvider(SpreadSheetNameProvider.class)
    String m_spreadSheetName;

    static final class SpreadSheetNameRef implements ParameterReference<String> {
    }

    static final class SpreadSheetNameProvider implements StateProvider<String> {

        Supplier<List<StringChoice>> m_spreadSheetsSupplier;

        Supplier<String> m_spreadSheetIdSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_spreadSheetsSupplier = initializer.computeFromProvidedState(SpreadSheetChoicesProvider.class);
            m_spreadSheetIdSupplier = initializer.computeFromValueSupplier(SpreadSheetIDRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) throws StateComputationFailureException {
            final var spreadSheets = m_spreadSheetsSupplier.get();
            if (spreadSheets == null || spreadSheets.isEmpty()) {
                return null;
            }
            final var spreadSheetID = m_spreadSheetIdSupplier.get();
            final var matchingSheet =
                spreadSheets.stream().filter(sheet -> sheet.id().equals(spreadSheetID)).findFirst();
            return matchingSheet.map(StringChoice::text).orElse(null);
        }

    }

    static final class SpreadSheetChoicesProvider implements StringChoicesProvider {

        Supplier<List<File>> m_spreadSheetsListSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_spreadSheetsListSupplier = initializer.computeFromProvidedState(SpreadSheetsRefresher.class);
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
                final var spreadsheetUrlString =
                    sheetConnection.getSheetsService().spreadsheets().get(spreadSheetID).execute().getSpreadsheetUrl();
                final var spreadsheetUrl = new URL(spreadsheetUrlString);
                DesktopUtil.browse(spreadsheetUrl);
            } catch (IOException | NoSuchCredentialException e) {
                throw ExceptionUtils.asRuntimeException(e);
            }

            return null;
        }

    }

    /**
     * Persistor for {@link SpreadSheetParameters}.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public static final class SpreadSheetParameterPersistor implements NodeParametersPersistor<SpreadSheetParameters> {

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

}
