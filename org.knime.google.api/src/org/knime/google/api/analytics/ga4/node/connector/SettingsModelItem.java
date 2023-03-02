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
package org.knime.google.api.analytics.ga4.node.connector;

import java.util.function.Function;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.google.api.analytics.ga4.node.util.PersistedSettingsModel;
import org.knime.google.api.analytics.ga4.node.util.Persistor;

/**
 * Settings model for an item that can be persisted, validated and stringified.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @param <I> item type
 */
final class SettingsModelItem<I> extends PersistedSettingsModel<I> {

    interface Validator<I> extends Function<I, InvalidSettingsException> {}

    interface Cloner<I> {
        SettingsModelItem<I> clone(SettingsModelItem<I> existing, Persistor<I> persistor,
            Function<I, String> toStringFn, Validator<I> validator);
    }

    private final String m_configKey;

    private final Cloner<I> m_cloner;
    private final Function<I, String> m_toStringFn;
    private final Validator<I> m_validator;

    SettingsModelItem(final String configKey, final I initialValue, final Persistor<I> persistor,
            final Cloner<I> cloner, final Function<I, String> toStringFn,
            final Validator<I> validator) {
        super(initialValue, persistor, ro -> ro.getConfig(configKey), wo -> wo.addConfig(configKey));
        m_configKey = configKey;
        m_cloner = cloner;
        m_toStringFn = toStringFn;
        m_validator = validator;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected SettingsModelItem<I> createClone() {
        return m_cloner.clone(this, m_persistor, m_toStringFn, m_validator);
    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    @Override
    protected void validateForModel(final ConfigRO cfg) throws InvalidSettingsException {
        m_persistor.load(cfg);
    }

    @Override
    public String toString() {
        return m_toStringFn.apply(getValue());
    }

    void validate() throws InvalidSettingsException {
        final var ex = m_validator.apply(getValue());
        if (ex != null) {
            throw ex;
        }
    }

    @Override
    protected String getConfigName() {
        return m_configKey;
    }

}
