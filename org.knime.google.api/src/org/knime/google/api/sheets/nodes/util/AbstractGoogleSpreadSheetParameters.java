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

import java.util.List;

import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.google.api.sheets.nodes.util.NodesUtil.AreButtonsEnabled;
import org.knime.google.api.sheets.nodes.util.NodesUtil.ConnectionMessageProvider;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.message.TextMessage;

import com.google.api.services.drive.model.File;

/**
 * Abstract base class for Google Sheet node parameters which provides common refresh button functionality.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public abstract class AbstractGoogleSpreadSheetParameters implements NodeParameters {

    /**
     * Layout for the widgets before the buttons.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public interface BeforeButtonLayout {
    }

    /**
     * Layout for the buttons.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    @HorizontalLayout
    @After(BeforeButtonLayout.class)
    public interface ButtonLayout {
    }

    /**
     * Layout for the widgets after the buttons.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    @After(ButtonLayout.class)
    public interface AfterButtonLayout {
    }

    @Layout(BeforeButtonLayout.class)
    @TextMessage(ConnectionMessageProvider.class)
    Void m_connectionSummary;

    @Layout(ButtonLayout.class)
    @Widget(title = "Refresh", description = """
            Refreshes the list of available spreadsheets from Google Drive.
            """)
    @SimpleButtonWidget(ref = RefreshButtonRef.class)
    @Effect(predicate = AreButtonsEnabled.class, type = EffectType.ENABLE)
    Void m_refreshButton;

    /**
     * The button reference for the refresh button.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public static final class RefreshButtonRef implements ButtonReference {
    }

    /**
     * State provider that refreshes the list of available spreadsheets.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public static final class SpreadSheetsRefresher implements StateProvider<List<File>> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            initializer.computeOnButtonClick(RefreshButtonRef.class);
        }

        @Override
        public List<File> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            try {
                final var sheetConnection = NodesUtil.getCheckedSheetConnection(parametersInput);
                final var nodeContainer = NodeContext.getContext().getNodeContainer();
                final var nativeNodeContainer = (NativeNodeContainer)nodeContainer;
                final var nodeModel = nativeNodeContainer.getNodeModel();
                final var nodeModelWithCache = (GoogleSpreadSheetCacheNodeModel)nodeModel;
                return nodeModelWithCache.computeSpreadSheetsEntry(sheetConnection);
            } catch (Exception e) { //NOSONAR
                return List.of();
            }
        }

    }

}
