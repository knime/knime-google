/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 5, 2017 (oole): created
 */
package org.knime.google.api.sheets.nodes.connectorinteractive;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.FileUtil;
import org.knime.google.api.sheets.data.GoogleSheetsInteractiveAuthentication;
import org.knime.google.api.util.SettingsModelCredentialLocation;

/**
 * This class holds the settings for the GoogleInterActiveServiceProvider
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
final class GoogleInteractiveServiceProviderSettings {

    private static final String DEFAULT_USERID = "sheetUser";

    private final String m_configName = "googleSheetsInteractive";
    private static final String BYTE_FILE = "byteFile";



    private String m_credentialTempFolder = null;

    private String m_storedCredential = null;

    private final SettingsModelCredentialLocation m_credentialLocationModel =
            new SettingsModelCredentialLocation("credentialLocation", "${user.home}/knime");

    /**
     * Returns the {@link SettingsModelString} for the credential location
     *
     * @return The {@link SettingsModelString} for the credential location
     */
    protected SettingsModelCredentialLocation getCredentialLocationModel() {
        return m_credentialLocationModel;
    }

    /**
     * Returns the user id.
     *
     * @return The user id
     */
    public String getUserString(){
        String userId;
        if (inNodeCredential()) {
            userId = DEFAULT_USERID;
        } else {
            userId = m_credentialLocationModel.getUserId();
        }
        return userId;
    }

    /**
     * Returns the credential location.
     *
     * @return The credential location
     * @throws InvalidSettingsException
     */
    public String getCredentialLocation() throws InvalidSettingsException {
        String path;
        if (inNodeCredential()) {
            File credentialFolder = null;
            try {
                credentialFolder = FileUtil.createTempDir("sheets");
                m_credentialTempFolder = credentialFolder.getPath();
                if (m_storedCredential != null) {
                    GoogleSheetsInteractiveAuthentication.createTempFromByteFile(credentialFolder, m_storedCredential);
                }
                path = credentialFolder.getPath();
            } catch (IOException | URISyntaxException e) {
                throw new InvalidSettingsException("Could not create temporary Credentials file");
            }
        } else {
            path = m_credentialLocationModel.getCredentialPath();
        }
        return path;
    }


    /**
     * Returns the StoredCredential file as a byte string.
     *
     * @return The stored credential as a byte string
     * @throws InvalidSettingsException If the user is not yet authenticated.
     */
    protected String getEncodedStoredCredential() throws InvalidSettingsException {
            return m_storedCredential;
    }

    /**
     * Returns the credential location.
     *
     * @param credentialFolder  The folder containing the StoredCredential
     *
     * @return The credential location
     * @throws IOException
     * @throws URISyntaxException
     */
    private Path getStoredCredentialPath(final File credentialFolder) throws IOException, URISyntaxException {
        return new File(credentialFolder, GoogleSheetsInteractiveAuthentication.STORAGE_CREDENTIAL).toPath();
    }


    /**
     * This method has to be called in the {@link NodeModel}.
     *
     * @param settings The node settings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        saveAuth(settings);
        m_credentialLocationModel.saveSettingsTo(settings);
    }

    /**
     * This method has to be called in the {@link NodeModel}.
     *
     * @param settings The node settings
     * @throws InvalidSettingsException If the settings are invalid
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config = settings.getConfig(m_configName);
        config.getString(BYTE_FILE);
        m_credentialLocationModel.validateSettings(settings);
    }

    /**
     * This method has to be called in the {@link NodeModel}.
     *
     * @param settings The node settings
     * @throws InvalidSettingsException If the settings are invalid
     */
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config = settings.getConfig(m_configName);
        m_storedCredential = config.getString(BYTE_FILE);
        m_credentialLocationModel.loadSettingsFrom(settings);
    }

    /**
     * @throws IOException
     * @throws URISyntaxException
     */
    public void setByteString() throws IOException, URISyntaxException {
        if (inNodeCredential()) {
            m_storedCredential = Base64.getEncoder().encodeToString(
                Files.readAllBytes(getStoredCredentialPath(new File(m_credentialTempFolder))));
        } else {
            removeInNodeCredentials();
        }
    }

    /**
     * Sets the byte String to null.
     */
    public void removeInNodeCredentials() {
        m_storedCredential = null;
    }

    /**
     * Saves the settings for the authentication. Should be used in the authentication dialog.
     *
     * @param settings
     */
    public void saveAuth(final NodeSettingsWO settings) {
        if (!inNodeCredential()) {
            removeInNodeCredentials();
        }
        Config config = settings.addConfig(m_configName);
        config.addString(BYTE_FILE, m_storedCredential);
    }

    /**
     * Loads the settings for the authentication.
     *
     * @param settings
     * @throws InvalidSettingsException If the settings can not be loaded
     */
    public void loadAuth(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config = settings.getConfig(m_configName);
        m_storedCredential = config.getString(BYTE_FILE);
    }

    /**
     * Returns whether or not the credential is stored in-node.
     *
     * @return Whether or not the credential is stored in-node
     */
    public boolean inNodeCredential() {
        return m_credentialLocationModel.useDefault();
    }


}
