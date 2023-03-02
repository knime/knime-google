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
 *   16 Jun 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.lang3.Functions.FailableConsumer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.google.api.analytics.ga4.node.query.GADateRange.GADateRangePersistor;


/**
 * Settings model for Google Analytics date ranges.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
class SettingsModelDateRanges extends SettingsModel {

    private final String m_configKey;
    private final FailableConsumer<List<GADateRange>, InvalidSettingsException> m_validator;

    private List<GADateRange> m_ranges;

    public SettingsModelDateRanges(final String configKey, final List<GADateRange> initial,
            final FailableConsumer<List<GADateRange>, InvalidSettingsException> validator) {
        m_configKey = configKey;
        m_validator = validator;
        m_ranges = new ArrayList<>();
        if (initial != null) {
            m_ranges.addAll(initial);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelDateRanges createClone() {
        return new SettingsModelDateRanges(m_configKey, m_ranges, m_validator);
    }


    @Override
    protected String getModelTypeID() {
        return "SMID_GA_DATERANGES";
    }

    @Override
    protected String getConfigName() {
        return m_configKey;
    }

    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        try {
            loadSettingsForModel(settings);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException("Loading date ranges failed: " + e.getMessage(), e);
        }

    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_validator.accept(loadFromSettings(settings));
    }

    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setRanges(loadFromSettings(settings));
    }

    private List<GADateRange> loadFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var cfg = settings.getConfig(m_configKey);
        final var ranges = new ArrayList<GADateRange>();
        var i = 0;
        while (true) {
            final var key = i + "";
            if (!hasElementConfig(cfg, key)) {
                break;
            }
            ranges.add(new GADateRangePersistor().load(cfg.getConfig(key)));
            i++;
        }
        return ranges;
    }

    private static boolean hasElementConfig(final Config cfg, final String key) {
        try {
            cfg.getConfig(key);
            return true;
        } catch (final InvalidSettingsException e) { // NOSONAR
            return false;
        }
    }

    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final var cfg = settings.addConfig(m_configKey);
        for (var i = 0; i < m_ranges.size(); i++) {
            final var key = i + "";
            new GADateRangePersistor().save(m_ranges.get(i), cfg.addConfig(key));
        }
    }

    public List<GADateRange> getRanges() {
        return Collections.unmodifiableList(m_ranges);
    }

    public int size() {
        return m_ranges.size();
    }

    void setRanges(final List<GADateRange> newValue) {
        var changed = false;
        if (newValue.size() != m_ranges.size()) {
            changed = true;
        } else {
            final var num = newValue.size();
            for (var i = 0; i < num; i++) {
                final var existing = m_ranges.get(i);
                final var newVal = newValue.get(i);
                changed = !existing.equals(newVal);
            }
        }
        m_ranges = newValue;
        if (changed) {
            notifyChangeListeners();
        }
    }



    @Override
    public String toString() {
        final var joiner = new StringJoiner(",", "SettingsModelDateRanges([", "])");
        if (m_ranges != null) {
            m_ranges.forEach(i -> joiner.add(i.toString()));
        }
        return joiner.toString();
    }

}
