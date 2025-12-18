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

package org.knime.google.api.sheets.nodes.sheetupdater;

import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.google.api.sheets.nodes.util.AbstractGoogleSpreadSheetWriterParameters;
import org.knime.google.api.sheets.nodes.util.SheetNameUtil;
import org.knime.google.api.sheets.nodes.util.SheetNameUtil.SheetNamesChoicesProvider;
import org.knime.google.api.sheets.nodes.util.SpreadSheetParameters;
import org.knime.google.api.sheets.nodes.util.SpreadSheetParameters.SpreadSheetParameterPersistor;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

/**
 * Node parameters for Google Sheets Updater.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class GoogleSheetUpdaterParameters extends AbstractGoogleSpreadSheetWriterParameters {

    @PersistWithin({"spreadsheetChooser"})
    @Persistor(SpreadSheetParameterPersistor.class)
    SpreadSheetParameters m_spreadSheetIdentifierParameters = new SpreadSheetParameters();

    @Layout(AfterButtonLayout.class)
    @PersistWithin({"spreadsheetChooser"})
    @Persist(configKey = "firstSheet")
    @Widget(title = "Select first sheet", description = """
            When selected, the first sheet of the spreadsheet will be updated instead of the one selected from the
            drop-down menu.
            """)
    @ValueReference(UseFirstSheet.class)
    boolean m_firstSheet;

    static final class UseFirstSheet implements BooleanReference {
    }

    @Layout(AfterButtonLayout.class)
    @PersistWithin({"spreadsheetChooser"})
    @Persist(configKey = "sheetName")
    @Widget(title = "Sheet", description = """
            The sheet from the spreadsheet that should be updated.
            Available sheets can be selected from the drop-down menu.
            """)
    @ChoicesProvider(SheetNamesChoicesProvider.class)
    @ValueProvider(SheetNameProvider.class)
    @ValueReference(SheetNameRef.class)
    @Effect(predicate = UseFirstSheet.class, type = EffectType.HIDE)
    String m_sheetName;

    static final class SheetNameRef implements ParameterReference<String> {
    }

    @Layout(WriteSettingsSection.class)
    @Persist(configKey = "clearSheet")
    @Widget(title = "Clear sheet before writing", description = """
            When this option is selected, the sheet or the selected range of the sheet will be cleared before
            writing. This deletes the content in the specified sheet/range.
            """)
    @ValueReference(IsClearSheetEnabled.class)
    @Effect(predicate = IsAppendEnabled.class, type = EffectType.DISABLE)
    boolean m_clearSheet;

    static final class IsClearSheetEnabled implements BooleanReference {
    }

    @Layout(WriteSettingsSection.class)
    @Persist(configKey = "append")
    @Widget(title = "Append to sheet", description = """
            When this option is selected, the data table content will be appended to the selected sheet.
            """)
    @ValueReference(IsAppendEnabled.class)
    @Effect(predicate = IsClearSheetOrIsRangeEnabled.class, type = EffectType.DISABLE)
    boolean m_append;

    static final class IsAppendEnabled implements BooleanReference {
    }

    @Layout(WriteSettingsSection.class)
    @Persist(configKey = "range_BOOL")
    @Widget(title = "Select range", description = """
            Specify whether a range should be read from the sheet.
            """)
    @ValueReference(IsRangeEnabled.class)
    @Effect(predicate = IsAppendEnabled.class, type = EffectType.DISABLE)
    boolean m_enableRange;

    static final class IsRangeEnabled implements BooleanReference {
    }

    @Layout(WriteSettingsSection.class)
    @Persist(configKey = "range")
    @Widget(title = "Range", description = """
            The range that should be read from the sheet can be specified in A1 notation. (E.g. "A1:G20")
            """)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @Effect(predicate = IsAppendDisabledAndRangeEnabled.class, type = EffectType.SHOW)
    String m_range = "";

    static final class IsAppendDisabledAndRangeEnabled implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return not(i.getPredicate(IsAppendEnabled.class)).and(i.getPredicate(IsRangeEnabled.class));
        }

    }

    static final class IsClearSheetOrIsRangeEnabled implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsClearSheetEnabled.class).or(i.getPredicate(IsRangeEnabled.class));
        }

    }

    static final class SheetNameProvider extends SheetNameUtil.SheetNameProvider {

        protected SheetNameProvider() {
            super(SheetNameRef.class);
        }

    }

}
