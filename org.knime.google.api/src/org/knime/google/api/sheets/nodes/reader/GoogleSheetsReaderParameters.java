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

import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.google.api.sheets.nodes.util.AbstractGoogleSpreadSheetParameters;
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
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

/**
 * Node parameters for Google Sheets Reader.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class GoogleSheetsReaderParameters extends AbstractGoogleSpreadSheetParameters {

    @PersistWithin({"spreadsheet"})
    @Persistor(SpreadSheetParameterPersistor.class)
    SpreadSheetParameters m_spreadSheetIdentifierParameters = new SpreadSheetParameters();

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
    @PersistWithin({"spreadsheet"})
    @Persist(configKey = "sheetName")
    @Widget(title = "Sheet", description = """
            The sheet from the spreadsheet that should be read.
            Available sheets can be selected from the drop-down menu.
            """)
    @ChoicesProvider(SheetNamesChoicesProvider.class)
    @ValueProvider(SheetNameProvider.class)
    @ValueReference(SheetNameRef.class)
    @Effect(predicate = UseFirstSheet.class, type = EffectType.HIDE)
    String m_sheetName;

    static final class SheetNameRef implements ParameterReference<String> {
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
    @Widget(title = "Read RowIDs", description = """
            Specify whether the first column of the sheet should be read as RowIDs.
            """)
    boolean m_hasRowHeader = true;

    static final class SheetNameProvider extends SheetNameUtil.SheetNameProvider {

        protected SheetNameProvider() {
            super(SheetNameRef.class);
        }

    }

}
