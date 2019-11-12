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
 */
package org.knime.database.extension.bigquery.agent;

import static java.util.Objects.requireNonNull;
import static org.knime.database.util.CsvFiles.getCharsetFrom;

import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.core.node.ExecutionMonitor;
import org.knime.database.agent.loader.DBLoadTableFromFileParameters;
import org.knime.database.agent.loader.DBLoader;
import org.knime.database.connection.ConnectionProvider;
import org.knime.database.extension.bigquery.BigQueryProject;
import org.knime.database.model.DBTable;
import org.knime.database.session.DBSessionReference;

import com.google.auth.Credentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.CsvOptions;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobStatus;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.WriteChannelConfiguration;

/**
 * Google BigQuery data loader.
 *
 * @author Noemi Balassa
 */
public class BigQueryDBLoader implements DBLoader {

    private final DBSessionReference m_sessionReference;

    /**
     * Constructs a {@link BigQueryDBLoader} object.
     *
     * @param sessionReference the reference to the agent's session.
     */
    public BigQueryDBLoader(final DBSessionReference sessionReference) {
        m_sessionReference = requireNonNull(sessionReference, "sessionReference");
    }

    @Override
    public void load(final ExecutionMonitor executionMonitor, final Object parameters) throws Exception {
        @SuppressWarnings("unchecked")
        final DBLoadTableFromFileParameters<BigQueryLoaderSettings> loadParameters =
            (DBLoadTableFromFileParameters<BigQueryLoaderSettings>)parameters;
        final BigQueryLoaderSettings additionalSettings = loadParameters.getAdditionalSettings()
            .orElseThrow(() -> new IllegalArgumentException("Missing additional settings."));
        final ConnectionProvider connectionProvider = m_sessionReference.get().getConnectionProvider();
        final BigQueryProject project = connectionProvider.getController(BigQueryProject.class)
            .orElseThrow(() -> new SQLException("BigQuery project information is not available."));
        final FormatOptions formatOptions;
        switch (additionalSettings.getFileFormat()) {
            case CSV:
                final FileWriterSettings fileWriterSettings = additionalSettings.getFileWriterSettings()
                    .orElseThrow(() -> new IllegalArgumentException("Missing file writer settings."));
                formatOptions = CsvOptions.newBuilder().setEncoding(getCharsetFrom(fileWriterSettings))
                    .setFieldDelimiter(fileWriterSettings.getColSeparator())
                    .setQuote(fileWriterSettings.getQuoteBegin()).build();
                break;
            case PARQUET:
                formatOptions = FormatOptions.parquet();
                break;
            default:
                throw new IllegalArgumentException("Unsupported file format: " + additionalSettings.getFileFormat());
        }
        final JobStatus jobStatus;
        // The connection is borrowed only for consistency between the nodes.
        try (Connection connection = connectionProvider.getConnection(executionMonitor)) {
            final BigQuery bigQuery = connectionProvider.getController(Credentials.class)
                .map(credentials -> BigQueryOptions.newBuilder().setCredentials(credentials) // Forced line break.
                    .setProjectId(project.getId()).build())
                .orElseGet(() -> BigQueryOptions.newBuilder() // Forced line break.
                    .setProjectId(project.getId()).build()) // Forced line break.
                .getService();
            final DBTable table = loadParameters.getTable();
            final TableId tableId = TableId.of(table.getSchemaName(), table.getName());
            final WriteChannelConfiguration writeChannelConfiguration =
                WriteChannelConfiguration.of(tableId, formatOptions);
            // The location is not set because the dataset of the table is required. See:
            // https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/get
            // https://cloud.google.com/bigquery/docs/locations#specifying_your_location
            // E.g. JobId.newBuilder().setLocation("EU").build();
            final JobId jobId = JobId.of();
            executionMonitor.checkCanceled();
            executionMonitor.setMessage(
                "Uploading data to BigQuery (this might take some time without progress changes)");
            try (TableDataWriteChannel writer = bigQuery.writer(jobId, writeChannelConfiguration)) {
                try (OutputStream stream = Channels.newOutputStream(writer)) {
                    Files.copy(Paths.get(loadParameters.getFilePath()), stream);
                }
                jobStatus = writer.getJob().waitFor().getStatus();
            }
            executionMonitor.setProgress(1, "Upload finished");
        }
        final BigQueryError error = jobStatus.getError();
        if (error != null) {
            final StringBuilder buffer = new StringBuilder("BigQuery data loading failure.");
            final String message = error.getMessage();
            if (message != null) {
                buffer.append("\nMessage: ").append(message);
            }
            final String reason = error.getReason();
            if (reason != null) {
                buffer.append("\nReason: ").append(reason);
            }
            final List<BigQueryError> executionErrors = jobStatus.getExecutionErrors();
            if (executionErrors != null) {
                int i = 0;
                for (BigQueryError executionError : executionErrors) {
                    buffer.append("\nError ").append(++i).append(": ").append(executionError.getMessage());
                    final String executionErrorReason = executionError.getReason();
                    if (executionErrorReason != null) {
                        buffer.append("\nReason: ").append(executionErrorReason);
                    }
                }
            }
            throw new SQLException(buffer.toString());
        }
    }

}
