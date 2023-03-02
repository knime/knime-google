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
 *   13 Jun 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.query;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

/**
 * Swing-based dialog for the Google Analytics Connection node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class GAQueryDialog extends DefaultNodeSettingsPane {

    private final GAQueryNodeSettings m_settings;

    private final DialogComponentList<String> m_metrics;

    private final DialogComponentList<String> m_dimensions;

    private final DialogComponentDateRangeArray m_dateRanges;

    // advanced

    private final DialogComponentString m_countryCode;

    private final DialogComponentBoolean m_includeDateRangeNameColumn;

    private final DialogComponentBoolean m_keepEmptyRows;

    private final DialogComponentBoolean m_returnPropertyQuota;

    private final DialogComponentBoolean m_returnResponseMetadata;

    GAQueryDialog() {
        m_settings = new GAQueryNodeSettings();

        m_metrics = new DialogComponentList<>(m_settings.m_gaMetrics, "Metrics (1 to 10)", s -> s, 1,
                GAQueryNodeSettings.MAX_NUM_METRICS);
        addDialogComponent(m_metrics);

        m_dimensions = new DialogComponentList<>(m_settings.m_gaDimensions, "Dimensions (up to 9)", s -> s, 0,
                GAQueryNodeSettings.MAX_NUM_DIMENSIONS);
        addDialogComponent(m_dimensions);

        m_dateRanges = new DialogComponentDateRangeArray(m_settings.m_dateRanges, "Date Ranges (1 to 4)",
            GAQueryNodeSettings.MAX_NUM_DATE_RANGES);
        addDialogComponent(m_dateRanges);

        createNewTab("Advanced Settings");

        m_includeDateRangeNameColumn = new DialogComponentBoolean(m_settings.m_includeDateRangeNameColumn,
            "Include date range name column");
        addDialogComponent(m_includeDateRangeNameColumn);

        m_countryCode = new DialogComponentString(m_settings.m_currencyCode, "Country code");
        addDialogComponent(m_countryCode);
        m_keepEmptyRows = new DialogComponentBoolean(m_settings.m_keepEmptyRows, "Keep empty rows");
        addDialogComponent(m_keepEmptyRows);
        m_returnPropertyQuota = new DialogComponentBoolean(m_settings.m_returnPropertyQuota, "Return property quota");
        addDialogComponent(m_returnPropertyQuota);
        m_returnResponseMetadata = new DialogComponentBoolean(m_settings.m_returnResponseMetadata,
            "Return response metadata");
        addDialogComponent(m_returnResponseMetadata);

    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);
    }

}
