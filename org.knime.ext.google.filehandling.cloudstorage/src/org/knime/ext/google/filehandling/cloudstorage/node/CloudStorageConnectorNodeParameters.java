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
 * ------------------------------------------------------------------------
 */

package org.knime.ext.google.filehandling.cloudstorage.node;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.credentials.base.CredentialRef;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageConnectionConfig;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFSConnection;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFileSystem;
import org.knime.google.api.credential.CredentialUtil;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

import com.google.auth.Credentials;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.storage.StorageException;

/**
 * Node parameters for Google Cloud Storage Connector.
 *
 * @author Alexander Bondaletov
 * @author AI Migration Pipeline
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
public class CloudStorageConnectorNodeParameters implements NodeParameters {

    private static final String KEY_PROJECT_ID = "projectId";
    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";
    private static final String KEY_NORMALIZE_PATHS = "normalizePaths";
    private static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";
    private static final String KEY_READ_TIMEOUT = "readTimeout";

    @Widget(title = "Project ID", description = "Specifies the <a href=\"https://cloud.google.com/storage/"
            + "docs/key-terms#projects\">project</a> to which the Cloud Storage data belongs. See "
            + "<a  href=\"https://cloud.google.com/resource-manager/docs/creating-managing-projects"
            + "#identifying_projects\">Google Cloud Documentation</a> for more information.")
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @ValueReference(ProjectIdRef.class)
    @Persist(configKey = KEY_PROJECT_ID)
    String m_projectId = "";

    @Widget(title = "Working directory", description = "Specifies the <i>working directory</i> using the path "
            + "syntax explained above. The working directory must be specified as an absolute path. A working "
            + "directory allows downstream nodes to access files/folders using <i>relative</i> paths, i.e. paths"
            + " that do not have a leading slash. If not specified, the default working directory is \"/\".")
    @FileSelectionWidget(SingleFileSelectionMode.FOLDER)
    @WithCustomFileSystem(connectionProvider = FileSystemConnectionProvider.class)
    @Persist(configKey = KEY_WORKING_DIRECTORY)
    String m_workingDirectory = CloudStorageFileSystem.PATH_SEPARATOR;

    @Widget(title = "Normalize paths", description = "Determines if the path normalization "
            + "should be applied. Path normalization eliminates redundant"
            + " components of a path like, e.g. /a/../b/./c\" can be normalized to \"/b/c\". "
            + "When these redundant  components like \"../\" or \".\" are part of an existing object,"
            + " then normalization must be deactivated in order to access them properly.")
    @ValueReference(NormalizePathsRef.class)
    @Persist(configKey = KEY_NORMALIZE_PATHS)
    boolean m_normalizePaths = true;

    @Advanced
    @Widget(title = "Connection timeout in seconds", description = "Timeout in seconds"
            + " to establish a connection or 0 for an infinite timeout.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ConnectionTimeoutRef.class)
    @Persist(configKey = KEY_CONNECTION_TIMEOUT)
    int m_connectionTimeout = CloudStorageConnectionConfig.DEFAULT_TIMEOUT_SECONDS;

    @Advanced
    @Widget(title = "Read timeout in seconds", description = "Timeout in seconds to "
            + "read data from an established connection or 0 for an infinite timeout.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ReadTimeoutRef.class)
    @Persist(configKey = KEY_READ_TIMEOUT)
    int m_readTimeout = CloudStorageConnectionConfig.DEFAULT_TIMEOUT_SECONDS;

    // Legacy field to avoid warnings when loading old workflows or flow variables
    @Persist(configKey = "temp_file_path")
    private String m_tempFilePath;

    static final class ProjectIdRef implements ParameterReference<String> {}

    static final class ConnectionTimeoutRef implements ParameterReference<Integer> {}

    static final class ReadTimeoutRef implements ParameterReference<Integer> {}

    static final class NormalizePathsRef implements ParameterReference<Boolean> {}

    void load(final NodeSettingsRO settings){
        m_projectId = settings.getString(KEY_PROJECT_ID, "");
        m_workingDirectory = settings.getString(KEY_WORKING_DIRECTORY, CloudStorageFileSystem.PATH_SEPARATOR);
        m_normalizePaths = settings.getBoolean(KEY_NORMALIZE_PATHS, true);
        m_connectionTimeout = settings.getInt(KEY_CONNECTION_TIMEOUT,
            CloudStorageConnectionConfig.DEFAULT_TIMEOUT_SECONDS);
        m_readTimeout = settings.getInt(KEY_READ_TIMEOUT, CloudStorageConnectionConfig.DEFAULT_TIMEOUT_SECONDS);
    }

    void save(final NodeSettingsWO settings) {
        settings.addString(KEY_PROJECT_ID, m_projectId);
        settings.addString(KEY_WORKING_DIRECTORY, m_workingDirectory);
        settings.addBoolean(KEY_NORMALIZE_PATHS, m_normalizePaths);
        settings.addInt(KEY_CONNECTION_TIMEOUT, m_connectionTimeout);
        settings.addInt(KEY_READ_TIMEOUT, m_readTimeout);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_projectId.isEmpty()) {
            throw new InvalidSettingsException("Project ID is not configured");
        }
        if (m_workingDirectory.isEmpty() || !m_workingDirectory.startsWith(CloudStorageFileSystem.PATH_SEPARATOR)) {
            throw new InvalidSettingsException("Working directory must be set to an absolute path.");
        }
    }

    CloudStorageConnectionConfig createFSConnectionConfig(final Credentials credentials) {
        final var config = new CloudStorageConnectionConfig(m_workingDirectory, credentials);
        config.setNormalizePaths(m_normalizePaths);
        config.setProjectId(m_projectId);
        config.setConnectionTimeOut(Duration.ofSeconds(m_connectionTimeout));
        config.setReadTimeOut(Duration.ofSeconds(m_readTimeout));
        return config;
    }

    static final class FileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {
        private static final String ERROR_MSG = "Credential not available. Please re-execute the preceding "
                + "authenticator node and make sure it is connected.";

        private Supplier<String> m_projectIdSupplier;
        private Supplier<Integer> m_connectionTimeoutSupplier;
        private Supplier<Integer> m_readTimeoutSupplier;
        private Supplier<Boolean> m_normalizePathsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_projectIdSupplier = initializer.computeFromValueSupplier(ProjectIdRef.class);
            m_connectionTimeoutSupplier = initializer.computeFromValueSupplier(ConnectionTimeoutRef.class);
            m_readTimeoutSupplier = initializer.computeFromValueSupplier(ReadTimeoutRef.class);
            m_normalizePathsSupplier = initializer.computeFromValueSupplier(NormalizePathsRef.class);
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
            return () -> createConnection(parametersInput);
        }

        private CloudStorageFSConnection createConnection(final NodeParametersInput parametersInput)
                throws InvalidSettingsException {
            final String projectId = m_projectIdSupplier.get();
            if (projectId == null || projectId.isEmpty()) {
                throw new InvalidSettingsException("Project ID is not configured");
            }

            final var credentials = parametersInput.getInPortObject(0)
                    .filter(CredentialPortObject.class::isInstance)
                    .map(CredentialPortObject.class::cast)
                    .map(CredentialPortObject::toRef)
                    .map(FileSystemConnectionProvider::toOAuth2Credentials)
                    .orElseThrow(() -> new InvalidSettingsException(ERROR_MSG));

            final var config = new CloudStorageConnectionConfig(CloudStorageFileSystem.PATH_SEPARATOR, credentials);

            config.setProjectId(projectId);
            config.setConnectionTimeOut(Duration.ofSeconds(m_connectionTimeoutSupplier.get()));
            config.setReadTimeOut(Duration.ofSeconds(m_readTimeoutSupplier.get()));
            config.setNormalizePaths(m_normalizePathsSupplier.get());

            return toFSConnection(config);
        }

        private static CloudStorageFSConnection toFSConnection(final CloudStorageConnectionConfig config)
                throws InvalidSettingsException {
            final var connection = new CloudStorageFSConnection(config);
            try {
                ((CloudStorageFileSystem) connection.getFileSystem()).getClient().listBuckets(null);
                return connection;
            } catch (StorageException | IOException e) {

                try {
                    connection.close();
                } catch (StorageException | IOException closeEx) {
                    e.addSuppressed(closeEx);
                }
                throw new InvalidSettingsException(e.getMessage(), e);
            }
        }


        private static OAuth2Credentials toOAuth2Credentials(final CredentialRef credentialRef) {
            try {
                return CredentialUtil.toOAuth2Credentials(credentialRef);
            } catch (NoSuchCredentialException | IOException e) { // NOSONAR: handled by returning null
                return null;
            }
        }
    }
}
