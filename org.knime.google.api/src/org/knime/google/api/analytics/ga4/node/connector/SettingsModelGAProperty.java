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

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.google.api.analytics.ga4.node.GAProperty;
import org.knime.google.api.analytics.ga4.node.connector.SettingsModelItem.Cloner;
import org.knime.google.api.analytics.ga4.node.connector.SettingsModelItem.Validator;
import org.knime.google.api.analytics.ga4.node.util.Persistor;

/**
 * Settings model for a {@link GAProperty}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class SettingsModelGAProperty {

    private SettingsModelGAProperty() {
        // no-op
    }

    public static final class GAPropertyPersistor implements Persistor<GAProperty> {

        private static final String KEY = "analyticsPropertyId";

        @Override
        public GAProperty load(final ConfigRO cfg) throws InvalidSettingsException {
            return GAProperty.loadSettings(cfg, KEY);

        }

        @Override
        public void save(final GAProperty obj, final ConfigWO cfg) {
            obj.saveSettings(cfg, KEY);
        }

        @Override
        public String getModelTypeID() {
            return "SMID_GA_PROPERTY";
        }
    }

    public static final class GAPropertyValidator implements Validator<GAProperty> {

        @Override
        public InvalidSettingsException apply(final GAProperty prop) {
            if (prop == null) {
                return new InvalidSettingsException("Google Analytics Property is missing.");
            }
            final var id = prop.getPropertyId();
            if (StringUtils.isBlank(id)) {
                return new InvalidSettingsException("Google Analytics Property identifier must not be blank.");
            }
            if (!NumberUtils.isDigits(id)) {
                return new InvalidSettingsException("Google Analytics Property identifier must be numeric "
                    + "(i.e. contain only digits).");
            }
            return null;
        }

    }

    private static final class GAPropertyCloner implements Cloner<GAProperty> {
        @Override
        public SettingsModelItem<GAProperty> clone(final SettingsModelItem<GAProperty> existing,
                final Persistor<GAProperty> persistor, final Function<GAProperty, String> toStringFn,
                final Validator<GAProperty> validator) {
            return new SettingsModelItem<>(existing.getConfigName(), existing.getValue(), persistor, this, toStringFn,
                    validator);
        }
    }


    public static SettingsModelItem<GAProperty> create(final String configKey, final GAProperty initial) {
        return new SettingsModelItem<>(configKey, initial, new GAPropertyPersistor(),
                new GAPropertyCloner(), GAProperty::toString, new GAPropertyValidator());
    }
}
