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
 *   Aug 21, 2017 (oole): created
 */

package org.knime.google.api.sheets.data;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.google.api.data.GoogleApiConnection;

import com.google.api.services.analytics.Analytics;
import com.google.api.services.sheets.v4.Sheets;

/**
 * A connection to the Google Sheets API.
 *
 * Use the {@link Analytics} object returned by {@link #getSheetsService()} to communicate with the Google Analytics API.
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
public final class GoogleSheetsConnection {

    private static final String ALL_WILDCARD = "~all";

    private static final String CFG_APPLICATION_NAME = "applicationName";

    private static final String CFG_SIMPLE = "simpleAuth";

    private GoogleApiConnection m_connection;

    private String m_applicationName;

    private Sheets m_sheets;

    private boolean m_simpleAuth;


    /**
     * Creates a new {@link GoogleSheetsConnection} using the given {@link GoogleApiConnection}.
     *
     * @param connection The used GoogleApiConnection
     * @param applicationName Name of this application as it is shown to the Google API
     */
    public GoogleSheetsConnection(final GoogleApiConnection connection, final String applicationName) {
        m_connection = connection;
        m_applicationName = applicationName;
        m_sheets =
                new Sheets.Builder(m_connection.getHttpTransport(), m_connection.getJsonFactory(),
                        m_connection.getCredential()).setApplicationName(m_applicationName).build();
        m_simpleAuth = false;
    }

    /**
     * Creates a new {@link GoogleSheetsConnection} using O2Auth web authentication.
     * This creates a pop-up to confirm access to the necessary scopes.
     *
     * @param applicationName Name of this application as it is shown to the Google API
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public GoogleSheetsConnection(final String applicationName, final boolean config) throws IOException, GeneralSecurityException {
        m_applicationName = applicationName;
        m_sheets = GoogleSheetsInteractiveAuthentication.getSheetsService(m_applicationName, config);
        m_simpleAuth = true;
    }



    /**
     * Restores a {@link GoogleSheetsConnection} from a saved model (used by the framework).
     *
     * @param model The model containing the connection information
     * @throws InvalidSettingsException If the model was invalid
     */
    public GoogleSheetsConnection(final ModelContentRO model) throws InvalidSettingsException {
        try {
            m_simpleAuth = model.getBoolean(CFG_SIMPLE);
            m_applicationName = model.getString(CFG_APPLICATION_NAME);
            if (m_applicationName.contains("Simple")) {
                m_sheets = GoogleSheetsInteractiveAuthentication.getSheetsService(m_applicationName, false);
            } else {
                m_connection = new GoogleApiConnection(model);
                m_sheets =
                        new Sheets.Builder(m_connection.getHttpTransport(), m_connection.getJsonFactory(),
                                m_connection.getCredential()).setApplicationName(m_applicationName).build();
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidSettingsException(e);
        }
    }

    /**
     * @return The {@link Analytics} object used to communicate with the Google Analytics API
     */
    public Sheets getSheetsService() {
        return m_sheets;
    }


    /**
     * @param model The model to save the current configuration in
     */
    public void save(final ModelContentWO model) {
        if (!m_simpleAuth) {
            m_connection.save(model);
        }
        model.addBoolean(CFG_SIMPLE, m_simpleAuth);
        model.addString(CFG_APPLICATION_NAME, m_applicationName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GoogleSheetsConnection)) {
            return false;
        }
        GoogleSheetsConnection con = (GoogleSheetsConnection)obj;
        EqualsBuilder eb = new EqualsBuilder();
        eb.append(m_connection, con.m_connection);
        eb.append(m_applicationName, con.m_applicationName);
        return eb.isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(m_connection);
        hcb.append(m_applicationName);
        return hcb.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!m_simpleAuth) {
            sb.append(m_connection.toString() + "\n");
        } else {
            sb.append("Simple Authentication \n");
        }
        sb.append("Application name:\n" + m_applicationName + "\n");
        return sb.toString();
    }

}
