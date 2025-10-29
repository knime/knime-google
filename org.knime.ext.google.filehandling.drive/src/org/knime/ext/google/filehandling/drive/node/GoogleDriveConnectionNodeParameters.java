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

package org.knime.ext.google.filehandling.drive.node;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.CustomFileConnectionFolderReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.credentials.base.CredentialRef;
import org.knime.credentials.base.NoSuchCredentialException;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFSConnection;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFSConnectionConfig;
import org.knime.ext.google.filehandling.drive.fs.GoogleDriveFileSystem;
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

import com.google.auth.Credentials;
import com.google.auth.oauth2.OAuth2Credentials;

/**
 * Node parameters for Google Drive Connector.
 *
 * @author Kai Franze, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class GoogleDriveConnectionNodeParameters implements NodeParameters {

    @Widget(title = "Working directory", description = """
            Specify the working directory of the resulting file system connection, using the Path syntax explained \
            above. The working directory must be specified as an absolute path. A working directory allows downstream \
            nodes to access files/folders using relative paths, i.e. paths that do not have a leading slash. \
            The default working directory is the root "/".""")
    @CustomFileConnectionFolderReaderWidget(connectionProvider = FileSystemConnectionProvider.class)
    @Persist(configKey = GoogleDriveConnectionSettingsModel.KEY_WORKING_DIRECTORY)
    String m_workingDirectory = GoogleDriveFileSystem.PATH_SEPARATOR;

    @Advanced
    @Widget(title = "Connection timeout (seconds)", //
            description = "Timeout in seconds to establish a connection or 0 for an infinite timeout.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ConnectionTimeoutRef.class)
    @Persist(configKey = GoogleDriveConnectionSettingsModel.KEY_CONNECTION_TIMEOUT)
    int m_connectionTimeout = GoogleDriveFSConnectionConfig.DEFAULT_CONNECTION_TIMEOUT_SECONDS;

    @Advanced
    @Widget(title = "Read timeout (seconds)", //
            description = "Timeout in seconds to read data from connection or 0 for an infinite timeout.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @ValueReference(ReadTimeoutRef.class)
    @Persist(configKey = GoogleDriveConnectionSettingsModel.KEY_READ_TIMEOUT)
    int m_readTimeout = GoogleDriveFSConnectionConfig.DEFAULT_READ_TIMEOUT_SECONDS;

    static final class ConnectionTimeoutRef implements ParameterReference<Integer> {
    }

    static final class ReadTimeoutRef implements ParameterReference<Integer> {
    }

    static final class FileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

        private static final String ERROR_MSG = """
                Credential not available. Please re-execute the preceding authenticator node \
                and make sure it is connected.
                """;

        private Supplier<Integer> m_connectionTimeoutSupplier;

        private Supplier<Integer> m_readTimeoutSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_connectionTimeoutSupplier = initializer.computeFromValueSupplier(ConnectionTimeoutRef.class);
            m_readTimeoutSupplier = initializer.computeFromValueSupplier(ReadTimeoutRef.class);
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
            return () -> { // NOSONAR: Acceptable length of lambda expression
                final var credentials = parametersInput.getInPortObject(0) //
                        .filter(CredentialPortObject.class::isInstance) //
                        .map(CredentialPortObject.class::cast) //
                        .map(CredentialPortObject::toRef) //
                        .map(FileSystemConnectionProvider::toOAuth2Credentials) //
                        .orElseThrow(() -> new InvalidSettingsException(ERROR_MSG));
                final var config = toFSConnectionConfig(GoogleDriveFileSystem.PATH_SEPARATOR, credentials,
                        m_connectionTimeoutSupplier.get(), m_readTimeoutSupplier.get());
                return toFSConnection(config);
            };
        }

        private static OAuth2Credentials toOAuth2Credentials(final CredentialRef credentialRef) {
            try {
                return CredentialUtil.toOAuth2Credentials(credentialRef);
            } catch (IOException | NoSuchCredentialException e) { // NOSONAR: Exception not needed
                return null;
            }
        }

        private static GoogleDriveFSConnectionConfig toFSConnectionConfig(final String workingDirectory,
                final Credentials credentials, final int connectionTimeout, final int readTimeout) {
            final var config = new GoogleDriveFSConnectionConfig(workingDirectory, credentials);
            config.setConnectionTimeOut(Duration.ofSeconds(connectionTimeout));
            config.setReadTimeOut(Duration.ofSeconds(readTimeout));
            return config;
        }

        private static GoogleDriveFSConnection toFSConnection(final GoogleDriveFSConnectionConfig config)
                throws IOException {
            final var connection = new GoogleDriveFSConnection(config);
            GoogleDriveConnectionNodeModel.testConnection(connection);
            return connection;
        }
    }
}
