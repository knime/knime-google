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
 *   14 Jun 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.connector;

import java.time.Duration;
import java.util.Objects;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.google.api.analytics.ga4.node.util.PersistedSettingsModel;
import org.knime.google.api.analytics.ga4.node.util.Persistor;

/**
 * Settings model for {@link Duration} values which are persisted using a custom persistor, e.g. as milliseconds.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class SettingsModelDuration extends PersistedSettingsModel<Duration> {

    private Duration m_minValue;
    private Duration m_maxValue;
    private String m_configKey;

    SettingsModelDuration(final String configKey, final Duration duration, final Duration minValue,
            final Duration maxValue, final Persistor<Duration> persistor) {
        super(Objects.requireNonNull(duration), persistor, ro -> ro, wo -> wo);
        m_minValue = minValue;
        m_maxValue = maxValue;
        m_configKey = configKey;
    }

    static SettingsModelDuration create(final String configKey, final Duration defaultValue,
            final Persistor<Duration> persistor) {
        return new SettingsModelDuration(configKey, defaultValue, Duration.ofSeconds(0),
            Duration.ofSeconds(Integer.MAX_VALUE), persistor);
    }

    static SettingsModelDuration create(final String configKey, final Duration defaultValue,
            final Duration minValue, final Duration maxValue, final Persistor<Duration> persistor) {
        return new SettingsModelDuration(configKey, defaultValue, minValue, maxValue, persistor);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected SettingsModelDuration createClone() {
        return new SettingsModelDuration(getConfigName(), getValue(), m_minValue, m_maxValue, m_persistor);
    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        validateBounds(getValue());
        saveSettingsForModel(settings);
    }

    private void validateBounds(final Duration value) throws InvalidSettingsException {
        if (value.compareTo(m_minValue) < 0 || value.compareTo(m_maxValue) > 0) {
            throw new InvalidSettingsException(String.format("Duration \"%s\" outside of allowed range: [%s, %s]",
                value.toSeconds(), m_minValue.toSeconds(), m_maxValue.toSeconds()));
        }
    }

    @Override
    protected void validateForModel(final ConfigRO cfg) throws InvalidSettingsException {
        validateBounds(m_persistor.load(cfg));
    }

    @Override
    public String toString() {
        return "SettingsModelDuration{" + getValue().toString() + "}";
    }

    /**
     * Field persistor implementation saving a {@link Duration} as numeric in milliseconds granularity.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    static final class DurationAsMillis implements Persistor<Duration> {

        private final String m_configKey;

        public DurationAsMillis(final String configKey) {
            m_configKey = configKey;
        }

        @Override
        public Duration load(final ConfigRO config) throws InvalidSettingsException {
            return Duration.ofMillis(config.getLong(m_configKey));
        }

        @Override
        public void save(final Duration duration, final ConfigWO config) {
            config.addLong(m_configKey, duration.toMillis());
        }

        @Override
        public String getModelTypeID() {
            return "SMID_DURATION_MILLIS";
        }
    }

    @Override
    protected String getConfigName() {
        return m_configKey;
    }

}