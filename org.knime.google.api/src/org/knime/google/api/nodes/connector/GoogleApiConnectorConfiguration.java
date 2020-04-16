/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 19, 2014 ("Patrick Winter"): created
 */
package org.knime.google.api.nodes.connector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.knime.google.api.data.GoogleApiConnection;

/**
 * Configuration of the GoogleApiConnector node.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public class GoogleApiConnectorConfiguration {

    private static final String CFG_SERVICE_ACCOUNT_EMAIL = "service_account_email";

    private static final String CFG_KEY_FILE_LOCATION = "key_file_location";

    private static final String CFG_SCOPES = "scopes";

    private String m_serviceAccountEmail = "";

    private String m_keyFileLocation = "";

    private String[] m_scopes = new String[0];

    /**
     * @return the serviceAccountEmail
     */
    public String getServiceAccountEmail() {
        return m_serviceAccountEmail;
    }

    /**
     * @param serviceAccountEmail the serviceAccountEmail to set
     */
    public void setServiceAccountEmail(final String serviceAccountEmail) {
        m_serviceAccountEmail = serviceAccountEmail;
    }

    /**
     * @return the keyFileLocation
     */
    public String getKeyFileLocation() {
        return m_keyFileLocation;
    }

    /**
     * @param keyFileLocation the keyFileLocation to set
     */
    public void setKeyFileLocation(final String keyFileLocation) {
        m_keyFileLocation = keyFileLocation;
    }

    /**
     * @return the scopes
     */
    public String[] getScopes() {
        return m_scopes;
    }

    /**
     * @param scopes the scopes to set
     */
    public void setScopes(final String[] scopes) {
        m_scopes = scopes;
    }

    /**
     * @param settings The settings object to save in
     */
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_SERVICE_ACCOUNT_EMAIL, m_serviceAccountEmail);
        settings.addString(CFG_KEY_FILE_LOCATION, m_keyFileLocation);
        settings.addStringArray(CFG_SCOPES, m_scopes);
    }

    /**
     * @param settings The settings object to load from
     * @throws InvalidSettingsException If the settings are invalid
     */
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_serviceAccountEmail = settings.getString(CFG_SERVICE_ACCOUNT_EMAIL);
        m_keyFileLocation = settings.getString(CFG_KEY_FILE_LOCATION);
        m_scopes = settings.getStringArray(CFG_SCOPES);
    }

    /**
     * @param settings The settings object to load from
     */
    public void loadInDialog(final NodeSettingsRO settings) {
        m_serviceAccountEmail = settings.getString(CFG_SERVICE_ACCOUNT_EMAIL, "");
        m_keyFileLocation = settings.getString(CFG_KEY_FILE_LOCATION, "");
        m_scopes = settings.getStringArray(CFG_SCOPES, new String[0]);
    }

    /**
     * Creates the API connection object, if possible. It will create an empty Optional if the key file lives on a
     * remote server (either KNIME server or http/ftp/scp) and the argument flag is false (use case: during node
     * configuration we shouldn't do expensive I/O).
     *
     * @param isDuringExecute The flag as describe above.
     * @return The spec of this node, containing the GoogleApiConnection for the current configuration, if possible
     * @throws InvalidSettingsException If the current configuration is not valid
     */
    public Optional<GoogleApiConnection> createGoogleApiConnection(final boolean isDuringExecute)
            throws InvalidSettingsException {
        CheckUtils.checkSetting(StringUtils.isNotEmpty(m_serviceAccountEmail), "The service account email is missing");
        CheckUtils.checkSetting(StringUtils.isNotEmpty(m_keyFileLocation), "The key file location is missing");
        String keyFileLocation = m_keyFileLocation;
        try {
            URL keyFileURL = FileUtil.toURL(keyFileLocation);
            Path path = FileUtil.resolveToPath(keyFileURL);
            if (path != null) {
                keyFileLocation = path.toString();
            } else {
                if (!isDuringExecute) {
                    return Optional.empty();
                }
                String simpleFileBaseName = FilenameUtils.getBaseName(keyFileURL.getFile()); // /foo/bar.key -> 'bar'
                String fileExtension = FilenameUtils.getExtension(keyFileURL.getFile());     // /foo/bar.key -> 'key'
                File keyFileTemp = FileUtil.createTempFile(simpleFileBaseName, fileExtension);
                try (InputStream in = FileUtil.openStreamWithTimeout(keyFileURL)) {
                    FileUtils.copyInputStreamToFile(in, keyFileTemp);
                }
                keyFileLocation = keyFileTemp.getAbsolutePath();
            }
        } catch (URISyntaxException | InvalidPathException | IOException e) {
            throw new InvalidSettingsException(e);
        }
        try {
            return Optional.of(new GoogleApiConnection(m_serviceAccountEmail, keyFileLocation, m_scopes));
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidSettingsException(e);
        }
    }

}