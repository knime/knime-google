/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * ------------------------------------------------------------------------
 * 
 * History
 *   Mar 19, 2014 ("Patrick Winter"): created
 */
package org.knime.google.api.analytics.nodes.connector;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.google.api.analytics.data.GoogleAnalyticsConnection;
import org.knime.google.api.data.GoogleApiConnection;

/**
 * Configuration of the GoogleAnalyticsConnector node.
 * 
 * @author "Patrick Winter", University of Konstanz
 */
public class GoogleAnalyticsConnectorConfiguration {

    private static final String CFG_PROFILE_ID = "profile_id";

    private String m_profileId = "";

    /**
     * @return the profileId
     */
    public String getProfileId() {
        return m_profileId;
    }

    /**
     * @param profileId the profileId to set
     */
    public void setProfileId(final String profileId) {
        m_profileId = profileId;
    }

    /**
     * @param settings The settings object to save in
     */
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_PROFILE_ID, m_profileId);
    }

    /**
     * @param settings The settings object to load from
     * @throws InvalidSettingsException If the settings are invalid
     */
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_profileId = settings.getString(CFG_PROFILE_ID);
    }

    /**
     * @param settings The settings object to load from
     */
    public void loadInDialog(final NodeSettingsRO settings) {
        m_profileId = settings.getString(CFG_PROFILE_ID, "");
    }

    /**
     * @param googleApiConnection The GoogleApiConnection to use
     * @return GoogleAnalyticsConnection based on this configuration
     * @throws InvalidSettingsException If the current configuration is not valid
     */
    public GoogleAnalyticsConnection createGoogleAnalyticsConnection(final GoogleApiConnection googleApiConnection)
            throws InvalidSettingsException {
        if (m_profileId.isEmpty()) {
            throw new InvalidSettingsException("No profile ID selected");
        }
        return new GoogleAnalyticsConnection(googleApiConnection, "KNIME-Google-Analytics-Connector", m_profileId);
    }

}
