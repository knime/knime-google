/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 27, 2017 (oole): created
 */
package org.knime.google.api.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;

/**
 * The SettingsModel for the {@link DialogComponenCredentialLocation}
 *
 * @author Ole Ostergaard, KNIME GmbH
 */
public class SettingsModelCredentialLocation extends SettingsModelString {

    private String m_userId;
    private final static String USER_ID = "userId";

    private CredentialLocationType m_type;
    private final static String SELECTED_TYPE = "selectedType";

    private String m_configName;


    /**
     * Constructor.
     * @param configName The identifier the values are stored in in the {@link NodeSettings} object.
     */
    public SettingsModelCredentialLocation(final String configName) {
        super(configName, "");

        CheckUtils.checkArgument(StringUtils.isNotEmpty(configName), "The configName must be a non-empty string");
        m_userId = "sheetUser";
        m_type = CredentialLocationType.DEFAULT;
        m_configName = configName + "type";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SettingsModelCredentialLocation createClone() {
        return new SettingsModelCredentialLocation(m_configName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_googleCredentialLocation";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsForDialog(settings, specs);
        final Config config;
        try {
            config = settings.getConfig(m_configName);
            setValues(CredentialLocationType.valueOf(config.getString(SELECTED_TYPE, m_type.name())),
                config.getString(USER_ID, m_userId));
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsForDialog(settings);
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config = settings.getConfig(m_configName);
        final String type = config.getString(SELECTED_TYPE);
        final String userId = config.getString(USER_ID);
        final CredentialLocationType credentialType = CredentialLocationType.get(type);
        final String credentialLocation = settings.getString(super.getConfigName());
        switch(credentialType) {
            case DEFAULT:
                break;
            case CUSTOM:
                if (credentialLocation == null || credentialLocation.isEmpty()){
                    throw new InvalidSettingsException("Please provide a valid location");
                }
                break;
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettingsForModel(settings);
        Config config = settings.getConfig(m_configName);
        setValues(CredentialLocationType.valueOf(config.getString(SELECTED_TYPE)), config.getString(USER_ID));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        super.saveSettingsForModel(settings);
        Config config = settings.addConfig(m_configName);
        config.addString(USER_ID, m_userId);
        config.addString(SELECTED_TYPE, m_type.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * @param type Type of credential location that is selected
     * @param userId the given user id
     */
    public void setValues(final CredentialLocationType type, final String userId) {
        boolean changed = false;
        changed = setType(type) || changed;
        changed = setUserId(userId) || changed;
        if (changed) {
            notifyChangeListeners();
        }
    }

    private boolean setType(final CredentialLocationType type) {
        boolean sameValue = type.name().contentEquals(m_type.name());
        m_type = type;
        return !sameValue;
    }

    private boolean setUserId(final String userId) {
        boolean sameValue;
        if (userId == null) {
            sameValue = (m_userId == null);
        } else {
            sameValue = userId.equals(m_userId);
        }
        m_userId = userId;
        return !sameValue;
    }

    /**
     * Returns the user id.
     *
     * @return The user id
     */
    public String getUserId() {
        return m_userId;
    }

    /**
     * Returns the selected {@link CredentialLocationType}.
     *
     * @return The selected credential location
     */
    public CredentialLocationType getCredentialLocationType() {
        return m_type;
    }

    /**
     * Returns whether or not the default is chosen.
     *
     * @return whether or not the default is chosen
     */
    public boolean useDefault() {
        return m_type.equals(CredentialLocationType.DEFAULT);
    }

    /**
     * Returns the credential path which is set when the custom credential location is chosen.
     *
     * @return The credential path which is set when the custom credential location is chosen
     * @throws InvalidSettingsException If the path is invalid
     */
    public String getCredentialPath() throws InvalidSettingsException {
        Path path;
        try {
            path = FileUtil.resolveToPath(FileUtil.toURL(this.getStringValue()));
        } catch (IOException | URISyntaxException e) {
            throw new InvalidSettingsException("Not a valid path");
        }
        return path.toString();
    }

    /** Whether to use the default in node credential or the custom user id and credential location **/
    public enum CredentialLocationType implements ButtonGroupEnumInterface {
        /** Default credential location **/
        DEFAULT("Default", "The authentication credentials will be stored in the node settings."),
        /** Custom **/
        CUSTOM("Custom", "Specify a custom user id and a credential location");

        private String m_toolTip;
        private String m_text;

        private CredentialLocationType(final String text, final String toolTip) {
            m_text = text;
            m_toolTip = toolTip;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_text;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionCommand() {
            return name();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getToolTip() {
            return m_toolTip;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            return this.equals(DEFAULT);
        }

        /**
         *
         * @param actionCommand the action command
         * @return the @link {@link CredentialLocationType} for the action command
         */
        public static CredentialLocationType get(final String actionCommand) {
            return valueOf(actionCommand);
        }
    }

}
