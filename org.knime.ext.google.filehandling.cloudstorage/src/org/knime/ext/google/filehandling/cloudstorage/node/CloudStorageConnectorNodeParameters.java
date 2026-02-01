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

import java.time.Duration;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageConnectionConfig;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFSConnection;
import org.knime.ext.google.filehandling.cloudstorage.fs.CloudStorageFileSystem;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.google.api.credential.CredentialUtil;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

import com.google.auth.Credentials;

/**
 * Node parameters for Google Cloud Storage Connector.
 *
 * @author Alexander Bondaletov
 * @author AI Migration Pipeline
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
public class CloudStorageConnectorNodeParameters implements NodeParameters {

    @Section(title = "Connection")
    interface ConnectionSection {
    }

    @Section(title = "File System")
    @After(ConnectionSection.class)
    interface FileSystemSection {
    }

    @Advanced
    @Section(title = "Timeouts")
    @After(FileSystemSection.class)
    interface TimeoutsSection {
    }

    @Layout(ConnectionSection.class)
    @Widget(title = "Project ID", description = """
                Specifies the <a href="https://cloud.google.com/storage/docs/key-terms#projects">project</a> to which
                the Cloud Storage data belongs. See
                <a href="https://cloud.google.com/resource-manager/docs/creating-managing-projects\
                #identifying_projects"> Google Cloud Documentation</a> for more information.
            """)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @ValueReference(ProjectIdRef.class)
    String m_projectId = "";

    @Layout(FileSystemSection.class)
    @Widget(title = "Working directory", description = """
            Specifies the <i>working directory</i> using the path syntax explained above. The working directory must
            be specified as an absolute path. A working directory allows downstream nodes to access files/folders
            using <i>relative</i> paths, i.e. paths that do not have a leading slash. If not specified, the default
            working directory is "/".
            """)
    @FileSelectionWidget(SingleFileSelectionMode.FOLDER)
    @WithCustomFileSystem(connectionProvider = FileSystemConnectionProvider.class)
    @CustomValidation(WorkingDirectoryValidation.class)
    @ValueReference(WorkingDirectoryRef.class)
    String m_workingDirectory = CloudStorageFileSystem.PATH_SEPARATOR;

    static class WorkingDirectoryRef implements ParameterReference<String> {
    }

    static final class WorkingDirectoryValidation implements CustomValidationProvider<String> {
        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeFromValueSupplier(WorkingDirectoryRef.class);
        }

        @Override
        public ValidationCallback<String> computeValidationCallback(final NodeParametersInput parametersInput) {
            return WorkingDirectoryValidation::validateWorkingDirectory;
        }

        static void validateWorkingDirectory(final String workingDirectory) throws InvalidSettingsException {
            if (workingDirectory.isEmpty() || !workingDirectory.startsWith(CloudStorageFileSystem.PATH_SEPARATOR)) {
                throw new InvalidSettingsException("Working directory must be set to an absolute path.");
            }
        }
    }

    static final class FileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {
        private static final String ERROR_MSG = "Credential not available. Please re-execute the preceding "
                + "authenticator node and make sure it is connected.";

        private Supplier<String> m_projectIdSupplier;
        private Supplier<String> m_workingDirectorySupplier;
        private Supplier<Boolean> m_normalizePathsSupplier;
        private Supplier<Integer> m_connectionTimeoutSupplier;
        private Supplier<Integer> m_readTimeoutSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_projectIdSupplier = initializer.computeFromValueSupplier(ProjectIdRef.class);
            m_workingDirectorySupplier = initializer.computeFromValueSupplier(WorkingDirectoryRef.class);
            m_normalizePathsSupplier = initializer.computeFromValueSupplier(NormalizePathsRef.class);
            m_connectionTimeoutSupplier = initializer.computeFromValueSupplier(ConnectionTimeoutRef.class);
            m_readTimeoutSupplier = initializer.computeFromValueSupplier(ReadTimeoutRef.class);
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
            return () -> { // NOSONAR

                final var params = new CloudStorageConnectorNodeParameters();
                params.m_projectId = m_projectIdSupplier.get();
                params.m_workingDirectory = m_workingDirectorySupplier.get();
                params.m_connectionTimeout = m_connectionTimeoutSupplier.get();
                params.m_readTimeout = m_readTimeoutSupplier.get();
                params.m_normalizePaths = m_normalizePathsSupplier.get();

                try {
                    WorkingDirectoryValidation.validateWorkingDirectory(params.m_workingDirectory);
                } catch (InvalidSettingsException e) { // NOSONAR
                    params.m_workingDirectory = CloudStorageFileSystem.PATH_SEPARATOR;
                }

                params.validateOnConfigure();

                final var credSpec = parametersInput.getInPortSpec(0)//
                        .map(CredentialPortObjectSpec.class::cast)//
                        .orElseThrow(() -> new InvalidSettingsException(ERROR_MSG));

                try {
                    final var config = params.createFSConnectionConfig(CredentialUtil.toOAuth2Credentials(credSpec));
                    return new CloudStorageFSConnection(config);
                } catch (NoSuchCredentialException ex) {
                    throw ExceptionUtil.wrapAsIOException(ex);
                }
            };
        }
    }

    @Advanced
    @Layout(FileSystemSection.class)
    @Widget(title = "Normalize paths", description = """
            Determines if path normalization should be applied. Path normalization eliminates redundant components of a
            path, e.g. "/a/../b/./c" can be normalized to "/b/c". When these redundant components like "../" or "." are
            part of an existing object then normalization must be deactivated in order to access them properly.
            """)
    @ValueReference(NormalizePathsRef.class)
    boolean m_normalizePaths = true;

    @Layout(TimeoutsSection.class)
    @Widget(title = "Connection timeout in seconds", description = """
            Timeout in seconds to establish a connection or 0 for an infinite timeout.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ConnectionTimeoutRef.class)
    int m_connectionTimeout = CloudStorageConnectionConfig.DEFAULT_TIMEOUT_SECONDS;

    @Layout(TimeoutsSection.class)
    @Widget(title = "Read timeout in seconds", description = """
            Timeout in seconds to read data from an established connection or 0 for an infinite timeout.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ReadTimeoutRef.class)
    int m_readTimeout = CloudStorageConnectionConfig.DEFAULT_TIMEOUT_SECONDS;

    // Legacy field to avoid warnings when loading old workflows or flow variables
    // @Persist(configKey = "temp_file_path")
    // private String m_tempFilePath;

    static final class ProjectIdRef implements ParameterReference<String> {
    }

    static final class ConnectionTimeoutRef implements ParameterReference<Integer> {
    }

    static final class ReadTimeoutRef implements ParameterReference<Integer> {
    }

    static final class NormalizePathsRef implements ParameterReference<Boolean> {
    }

    void validateOnConfigure() throws InvalidSettingsException {
        if (m_projectId.isEmpty()) {
            throw new InvalidSettingsException("Project ID is not configured");
        }

        WorkingDirectoryValidation.validateWorkingDirectory(m_workingDirectory);
    }

    CloudStorageConnectionConfig createFSConnectionConfig(final Credentials credentials) {
        final var config = new CloudStorageConnectionConfig(m_workingDirectory, credentials);
        config.setProjectId(m_projectId);
        config.setNormalizePaths(m_normalizePaths);
        config.setConnectionTimeOut(Duration.ofSeconds(m_connectionTimeout));
        config.setReadTimeOut(Duration.ofSeconds(m_readTimeout));
        return config;
    }
}
