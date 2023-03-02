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
 *   2 Mar 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.query;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.Functions.FailableConsumer;
import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;
import org.knime.google.api.analytics.ga4.node.query.SettingsModelList.ItemPersistor;

/**
 * Settings for the Google Analytics 4 query node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class GAQueryNodeSettings {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GAQueryNodeSettings.class);

    /** The Google Analytics Data API restriction for one API call. */
    static final int MAX_NUM_METRICS = 10;
    /** The Google Analytics Data API restriction for one API call. */
    static final int MAX_NUM_DIMENSIONS = 9;
    /** The Google Analytics Data API restriction for one API call. */
    static final int MAX_NUM_DATE_RANGES = 4;

    private static final ItemPersistor<String> ITEM_PERSISTOR = new ItemPersistor<String>() {
        @Override
        public String load(final Config cfg) throws InvalidSettingsException {
            return cfg.getString("name");
        }
        @Override
        public void save(final Config cfg, final String item) {
            cfg.addString("name", item);
        }
    };

    /** Metrics to query. */
    SettingsModelList<String> m_gaMetrics = SettingsModelList.create("gaMetrics", Collections.emptyList(),
        ITEM_PERSISTOR, metrics -> {
            CheckUtils.checkSetting(!metrics.isEmpty(), "The Google Analytics query needs at least one metric.");
            CheckUtils.checkSetting(metrics.size() <= MAX_NUM_METRICS, "There are too many metrics specified.\n"
                + "You can only query a maximum of " + MAX_NUM_METRICS + " metrics at once.\n"
                + "Use another Google Analytics Query node and combine their results, for example using a Joiner node."
            );
        });

    /** Dimensions to query metrics under. */
    SettingsModelList<String> m_gaDimensions = SettingsModelList.create("gaDimensions", Collections.emptyList(),
        ITEM_PERSISTOR, dimensions -> {
            CheckUtils.checkSetting(dimensions.size() <= MAX_NUM_DIMENSIONS, "There are too many dimensions specified."
                    + "\n"
                    + "Google Analytics supports only up to " + MAX_NUM_DIMENSIONS + " dimensions at once.");
        });

    private static final FailableConsumer<List<GADateRange>, InvalidSettingsException> NUMBER_VALIDATOR = ranges -> {
        CheckUtils.checkSettingNotNull(ranges, "At least one date range is required");
        CheckUtils.checkSetting(!ranges.isEmpty(), "At least one date range is required.");
        CheckUtils.checkSetting(ranges.size() <= MAX_NUM_DATE_RANGES,
              "There are too many date ranges specified."
              + "Google Analytics supports only up to %d date ranges at once."
              + "Use another Google Analytics Query node and combine there results, for example using a Concatenate"
              + "node.", MAX_NUM_DATE_RANGES);
    };

    private static final FailableConsumer<List<GADateRange>, InvalidSettingsException> RANGE_VALIDATOR = ranges -> { // NOSONAR
        final var names = new HashSet<String>(4);
        for (final var range : ranges) {
            final var name = range.m_rangeName;
            // empty name is ok, since the Analytics API will generate a range name
            if (name != null && StringUtils.isNotEmpty(name)) {
                range.validate();
                if (names.contains(name)) {
                    throw new InvalidSettingsException(String.format("Ambiguous date range name %s.", name));
                }
                names.add(name);
            }
        }
    };

    /** Date ranges to make an API call with. */
    SettingsModelDateRanges m_dateRanges = new SettingsModelDateRanges("dateRanges", null, ranges -> {
        NUMBER_VALIDATOR.accept(ranges);
        RANGE_VALIDATOR.accept(ranges);
    });

    SettingsModelBoolean m_includeDateRangeNameColumn = new SettingsModelBoolean("includeDateRangeNameColumn", true);

    SettingsModelString m_currencyCode = new SettingsModelString("currencyCode", null);

    SettingsModelBoolean m_keepEmptyRows = new SettingsModelBoolean("keepEmptyRows", false);

    SettingsModelBoolean m_returnResponseMetadata = new SettingsModelBoolean("returnResponseMetadata", false);

    SettingsModelBoolean m_returnPropertyQuota = new SettingsModelBoolean("returnPropertyQuota", false);

    /**
     * Validates the current state of the settings instance. Intended to be called from the model.
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_gaMetrics.validateSettings(settings);
        m_gaDimensions.validateSettings(settings);
        m_dateRanges.validateSettings(settings);
        m_includeDateRangeNameColumn.validateSettings(settings);
        m_currencyCode.validateSettings(settings);
        m_keepEmptyRows.validateSettings(settings);
        m_returnResponseMetadata.validateSettings(settings);
        m_returnPropertyQuota.validateSettings(settings);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_gaMetrics.loadSettingsFrom(settings);
        m_gaDimensions.loadSettingsFrom(settings);
        m_dateRanges.loadSettingsForModel(settings);
        m_includeDateRangeNameColumn.loadSettingsFrom(settings);
        m_currencyCode.loadSettingsFrom(settings);
        m_keepEmptyRows.loadSettingsFrom(settings);
        m_returnResponseMetadata.loadSettingsFrom(settings);
        m_returnPropertyQuota.loadSettingsFrom(settings);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        saveSettingsTo(settings);
    }

    void saveSettingsTo(final NodeSettingsWO settings) {
        m_gaMetrics.saveSettingsTo(settings);
        m_gaDimensions.saveSettingsTo(settings);
        m_dateRanges.saveSettingsTo(settings);
        m_includeDateRangeNameColumn.saveSettingsTo(settings);
        m_currencyCode.saveSettingsTo(settings);
        m_keepEmptyRows.saveSettingsTo(settings);
        m_returnResponseMetadata.saveSettingsTo(settings);
        m_returnPropertyQuota.saveSettingsTo(settings);
    }


    void loadSettingsForDialog(final NodeSettingsRO settings) {
        loadSettingsForDialog(settings, m_gaMetrics);
        loadSettingsForDialog(settings, m_gaDimensions);
        loadSettingsForDialog(settings, m_dateRanges);
        loadSettingsForDialog(settings, m_includeDateRangeNameColumn);
        loadSettingsForDialog(settings, m_currencyCode);
        loadSettingsForDialog(settings, m_keepEmptyRows);
        loadSettingsForDialog(settings, m_returnResponseMetadata);
        loadSettingsForDialog(settings, m_returnPropertyQuota);
    }

    private static <T extends SettingsModel> void loadSettingsForDialog(final NodeSettingsRO settings, final T model) {
        try {
            model.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }

}
