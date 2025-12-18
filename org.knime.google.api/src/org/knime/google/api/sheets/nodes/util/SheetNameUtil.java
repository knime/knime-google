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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.google.api.sheets.nodes.util.SpreadSheetParameters.SpreadSheetIDRef;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

import com.google.api.services.sheets.v4.model.Sheet;

/**
 * Utility classes for sheet names.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
public final class SheetNameUtil {

    private SheetNameUtil() {
        // prevent instantiation
    }

    /**
     * Provider for sheet name state based on provided sheet names and a parameter reference for the sheet name.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public abstract static class SheetNameProvider implements StateProvider<String> {

        private Class<? extends ParameterReference<String>> m_sheetNameRefClass;

        /**
         * Constructor.
         *
         * @param sheetNameRefClass the class of the parameter reference for the sheet name
         */
        protected SheetNameProvider(final Class<? extends ParameterReference<String>> sheetNameRefClass) {
            m_sheetNameRefClass = sheetNameRefClass;
        }

        Supplier<List<String>> m_sheetNamesSupplier;

        Supplier<String> m_sheetNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_sheetNamesSupplier = initializer.computeFromProvidedState(SheetNamesProvider.class);
            m_sheetNameSupplier = initializer.getValueSupplier(m_sheetNameRefClass);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            final var currentSheetName = m_sheetNameSupplier.get();
            if (currentSheetName != null && !currentSheetName.isEmpty()) {
                return currentSheetName;
            }
            final var sheetNames = m_sheetNamesSupplier.get();
            return sheetNames == null || sheetNames.isEmpty() ? null : sheetNames.get(0);
        }

    }

    /**
     * Choices provider for sheet names.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public static final class SheetNamesChoicesProvider implements StringChoicesProvider {

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

    /**
     * State provider for sheet names.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public static final class SheetNamesProvider implements StateProvider<List<String>> {

        Supplier<String> m_spreadSheetIDSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_spreadSheetIDSupplier = initializer.computeFromValueSupplier(SpreadSheetIDRef.class);
        }

        @Override
        public List<String> computeState(final NodeParametersInput context) {
            final var spreadSheetID = m_spreadSheetIDSupplier.get();
            final var sheetConnection = NodesUtil.getUncheckedSheetConnection(context);
            if (spreadSheetID == null || spreadSheetID.isEmpty() || sheetConnection == null) {
                return List.of();
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

}
