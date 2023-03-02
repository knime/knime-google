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
 *   19 Jun 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node.query;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.lang3.Functions.FailableConsumer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.google.api.analytics.ga4.node.util.PersistedSettingsModel;
import org.knime.google.api.analytics.ga4.node.util.Persistor;

/**
 * List-based settings model.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @param <I> item type
 */
final class SettingsModelList<I> extends PersistedSettingsModel<List<I>> {

    interface ItemPersistor<I> {
        I load(Config cfg) throws InvalidSettingsException;
        void save(Config cfg, I item);
    }

    private final String m_configKey;
    private final FailableConsumer<List<I>, InvalidSettingsException> m_validator;

    private SettingsModelList(final String configKey, final List<I> initialValue, final Persistor<List<I>> persistor,
            final FailableConsumer<List<I>, InvalidSettingsException> validator) {
        super(initialValue, persistor, ro -> ro.getConfig(configKey), wo -> wo.addConfig(configKey));
        m_configKey = configKey;
        m_validator = validator;
    }

    public static <I> SettingsModelList<I> create(final String configKey, final List<I> initialValue,
            final ItemPersistor<I> itemPersistor, final FailableConsumer<List<I>, InvalidSettingsException> validator) {
        return new SettingsModelList<>(configKey, initialValue, new ListPersistor<>(itemPersistor), validator);
    }

    private static final class ListPersistor<I> implements Persistor<List<I>> {

        private final ItemPersistor<I> m_itemPersistor;

        ListPersistor(final ItemPersistor<I> itemPersistor) {
            m_itemPersistor = itemPersistor;
        }

        @Override
        public List<I> load(final ConfigRO cfg) throws InvalidSettingsException {
            final List<I> items = new ArrayList<>();
            var i = 0;
            while (true) {
                final var key = i + "";
                if (!hasElementConfig(cfg, key)) {
                    break;
                }
                items.add(m_itemPersistor.load(cfg.getConfig(key)));
                i++;
            }
            return items;
        }

        private static boolean hasElementConfig(final ConfigRO cfg, final String key) {
            try {
                cfg.getConfig(key);
                return true;
            } catch (final InvalidSettingsException e) { // NOSONAR
                return false;
            }
        }

        @Override
        public void save(final List<I> items, final ConfigWO cfg) {
            for (var i = 0; i < items.size(); i++) {
                final var key = i + "";
                final var item = items.get(i);
                m_itemPersistor.save(cfg.addConfig(key), item);
            }
        }

        @Override
        public String getModelTypeID() {
            return "SMID_list";
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    protected SettingsModelList<I> createClone() {
        return new SettingsModelList<>(getConfigName(), getValue(), m_persistor, m_validator);
    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }


    @Override
    public String toString() {
        final var joiner = new StringJoiner(",", "SettingsModelList([", "])");
        final var items = getValue();
        if (items != null) {
            items.forEach(i -> joiner.add(i.toString()));
        }
        return joiner.toString();
    }

    @Override
    protected void validateForModel(final ConfigRO cfg) throws InvalidSettingsException {
        m_validator.accept(m_persistor.load(cfg));
    }

    @Override
    protected String getConfigName() {
        return m_configKey;
    }

}
