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
 *   14 Nov 2023 (Leon Wenzler, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.google.api.analytics.ga4.node;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A representation of a Google Analytics 4 account, which is identified by a numeric ID. The existence of an instance
 * of this class does not imply that the account exists with Google Analytics. In other words, when creating an
 * instance, <em>no API calls</em> are made to verify the account identified by the ID actually exists, is accessible,
 * or is indeed representing a Google Analytics 4 account.
 *
 * @param m_accountId numeric Google Analytics 4 account ID
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui classes
public record GAAccount(String m_accountId) {

    /** Key for storing the account ID. */
    private static final String KEY_GA_ACCOUNT_ID = "analyticsAccountId";

    /**
     * Creates a Google Analytics 4 account.
     *
     * @param accountId Numeric identifier (without prefix).
     * @return Google Analytics account
     */
    @JsonCreator
    public static GAAccount of(final String accountId) {
        return new GAAccount(accountId);
    }

    /**
     * Returns the numeric ID of the account.
     *
     * @return the accountId
     */
    @JsonValue
    public String getAccountId() {
        return m_accountId;
    }

    /**
     * Loads the Google Analytics 4 account from the given config.
     *
     * @param cfg config to load from
     * @param key to find the account under
     * @return valid representation of a Google Analytics 4 account
     * @throws InvalidSettingsException if the model did not provide a valid GA account
     */
    public static GAAccount loadSettings(final ConfigRO cfg, final String key) throws InvalidSettingsException {
        if (!cfg.containsKey(key)) {
            return null;
        }
        final var id = cfg.getConfig(key).getString(KEY_GA_ACCOUNT_ID);
        return GAAccount.of(id);
    }

    /**
     * Saves the account into the config.
     *
     * @param cfg config to save into
     * @param key the key to save the account under
     */
    public void saveSettings(final ConfigWO cfg, final String key) {
        cfg.addConfig(key).addString(KEY_GA_ACCOUNT_ID, getAccountId());
    }

    /**
     * Persistor for the {@link GAAccount Google Analytics 4 account identifier}.
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    public static final class Persistor extends NodeSettingsPersistorWithConfigKey<GAAccount> {

        @Override
        public GAAccount load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return GAAccount.loadSettings(settings, getConfigKey());
        }

        @Override
        public void save(final GAAccount prop, final NodeSettingsWO settings) {
            if (prop != null) {
                prop.saveSettings(settings, getConfigKey());
            }
        }
    }
}
