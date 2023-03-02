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
package org.knime.google.api.analytics.ga4.node.util;

import java.util.function.Function;

import org.apache.commons.lang3.Functions.FailableFunction;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Settings model using a custom persistor.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @param <T> type of stored value
 */
public abstract class PersistedSettingsModel<T> extends SettingsModel {

    /**
     * Persistor for the settings model.
     */
    protected final Persistor<T> m_persistor;

    private T m_current;

    private FailableFunction<NodeSettingsRO, ConfigRO, InvalidSettingsException> m_readConfig;

    private Function<NodeSettingsWO, ConfigWO> m_writeConfig;

    /**
     * Constructor.
     * @param initialValue initial value
     * @param persistor persistor for the settings model
     * @param readConfig function that reads a config object from the node settings
     * @param writeConfig function that writes the config to the node settings
     */
    protected PersistedSettingsModel(final T initialValue, final Persistor<T> persistor,
            final FailableFunction<NodeSettingsRO, ConfigRO, InvalidSettingsException> readConfig,
            final Function<NodeSettingsWO, ConfigWO> writeConfig) {
        m_current = initialValue;
        m_persistor = persistor;
        m_readConfig = readConfig;
        m_writeConfig = writeConfig;
    }

    /**
     * Gets the current value.
     * @return current value
     */
    public T getValue() {
        return m_current;
    }

    /**
     * Sets the value and notifies change listeners if the value actually changed.
     * @param item new value
     */
    public final void set(final T item) {
        final var changed = !(m_current == null ? (item == null) : m_current.equals(item));
        m_current = item;
        if (changed) {
            notifyChangeListeners();
        }
    }

    /**
     * Read the expected values from the settings object, without assigning them to the internal variables!
     * (Is not called when the model was disabled at the time the settings were saved.)
     * See {@link #validateSettingsForModel(NodeSettingsRO)}.
     *
     * @param cfg
     * @throws InvalidSettingsException
     */
    protected abstract void validateForModel(final ConfigRO cfg) throws InvalidSettingsException;

    @Override
    protected final void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        validateForModel(m_readConfig.apply(settings));
    }

    @Override
    protected final void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        try {
            set(m_persistor.load(m_readConfig.apply(settings)));
        } catch (InvalidSettingsException e) { // NOSONAR
            // keep current settings
        }
    }

    @Override
    protected final void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        set(m_persistor.load(m_readConfig.apply(settings)));
    }

    @Override
    protected final void saveSettingsForModel(final NodeSettingsWO settings) {
        m_persistor.save(m_current, m_writeConfig.apply(settings));
    }

    @Override
    protected final String getModelTypeID() {
        return m_persistor.getModelTypeID();
    }
}
