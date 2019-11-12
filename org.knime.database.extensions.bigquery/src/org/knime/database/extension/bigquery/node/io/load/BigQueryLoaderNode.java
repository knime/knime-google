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
package org.knime.database.extension.bigquery.node.io.load;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.delete;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.knime.base.filehandling.remote.files.RemoteFileFactory.createRemoteFile;
import static org.knime.core.util.FileUtil.createTempFile;
import static org.knime.database.util.CsvFiles.writeCsv;
import static org.knime.datatype.mapping.DataTypeMappingDirection.EXTERNAL_TO_KNIME;
import static org.knime.datatype.mapping.DataTypeMappingDirection.KNIME_TO_EXTERNAL;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.bigdata.fileformats.parquet.datatype.mapping.ParquetType;
import org.knime.bigdata.fileformats.parquet.datatype.mapping.ParquetTypeMappingService;
import org.knime.bigdata.fileformats.parquet.writer.ParquetKNIMEWriter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.RowInput;
import org.knime.database.DBTableSpec;
import org.knime.database.agent.loader.DBLoadTableFromFileParameters;
import org.knime.database.agent.loader.DBLoader;
import org.knime.database.agent.metadata.DBMetadataReader;
import org.knime.database.datatype.mapping.DBTypeMappingRegistry;
import org.knime.database.extension.bigquery.agent.BigQueryLoaderFileFormat;
import org.knime.database.extension.bigquery.agent.BigQueryLoaderSettings;
import org.knime.database.model.DBColumn;
import org.knime.database.model.DBTable;
import org.knime.database.node.component.PreferredHeightPanel;
import org.knime.database.node.io.load.impl.UnconnectedCsvLoaderNode;
import org.knime.database.node.io.load.impl.UnconnectedCsvLoaderNodeComponents;
import org.knime.database.node.io.load.impl.UnconnectedCsvLoaderNodeSettings;
import org.knime.database.port.DBDataPortObject;
import org.knime.database.port.DBDataPortObjectSpec;
import org.knime.database.port.DBSessionPortObject;
import org.knime.database.port.DBSessionPortObjectSpec;
import org.knime.database.session.DBSession;
import org.knime.datatype.mapping.DataTypeMappingConfiguration;

/**
 * Implementation of the loader node for the Google BigQuery database.
 *
 * @author Noemi Balassa
 */
public class BigQueryLoaderNode extends UnconnectedCsvLoaderNode {
    private static final Map<String, ParquetType> BIG_QUERY_SQL_TO_PARQUET_TYPE_MAPPING;
    static {
        final Map<String, ParquetType> map = new HashMap<String, ParquetType>();
        map.put("BOOLEAN", new ParquetType(PrimitiveTypeName.BOOLEAN));
        map.put("INT64", new ParquetType(PrimitiveTypeName.INT64));
        map.put("NUMERIC", new ParquetType(PrimitiveTypeName.INT64, OriginalType.DECIMAL));
        map.put("DATE", new ParquetType(PrimitiveTypeName.INT32, OriginalType.DATE));
        map.put("DATETIME", new ParquetType(PrimitiveTypeName.INT32, OriginalType.DATE));
        map.put("TIMESTAMP", new ParquetType(PrimitiveTypeName.INT96));
        map.put("FLOAT64", new ParquetType(PrimitiveTypeName.DOUBLE));
        map.put("BYTES", new ParquetType(PrimitiveTypeName.BINARY));
        map.put("STRING", new ParquetType(PrimitiveTypeName.BINARY, OriginalType.UTF8));
        BIG_QUERY_SQL_TO_PARQUET_TYPE_MAPPING = unmodifiableMap(map);
    }

    private static final List<Charset> CHARSETS = unmodifiableList(asList(UTF_8, ISO_8859_1));
    //report progress only every x rows
    private static final long PROGRESS_THRESHOLD = 1000;

    private static Box createBox(final boolean horizontal) {
        final Box box;
        if (horizontal) {
            box = new Box(BoxLayout.X_AXIS);
        } else {
            box = new Box(BoxLayout.Y_AXIS);
        }
        return box;
    }

    private static JPanel createPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static void onFileFormatSelectionChange(final BigQueryLoaderNodeComponents components) {
        final Optional<BigQueryLoaderFileFormat> optionalFileFormat =
            BigQueryLoaderFileFormat.optionalValueOf(components.getFileFormatSelectionModel().getStringValue());
        components.getFileFormatModel()
            .setEnabled(optionalFileFormat.isPresent() && optionalFileFormat.get() == BigQueryLoaderFileFormat.CSV);
    }

    private static void writeParquet(final RowInput rowInput, final Path file, final DBTable table,
        final DBSession session, final ExecutionMonitor executionMonitor) throws Exception {
        executionMonitor.checkCanceled();
        final DataTableSpec inputTableSpec = rowInput.getDataTableSpec();
        long i = 0;
        long rowCnt = -1;
        if (rowInput instanceof DataTableRowInput) {
            rowCnt = ((DataTableRowInput)rowInput).getRowCount();
        }
        try (ParquetKNIMEWriter writer = new ParquetKNIMEWriter(createRemoteFile(file.toUri(), null, null),
            inputTableSpec, CompressionCodecName.UNCOMPRESSED.name(), -1,
            createParquetTypeMappingConfiguration(inputTableSpec, table, session, executionMonitor))) {
            for (DataRow row = rowInput.poll(); row != null; row = rowInput.poll()) {
                if (i % PROGRESS_THRESHOLD == 0) {
                    // set the progress
                    executionMonitor.checkCanceled();
                    final long finalI = i;
                    if (rowCnt <= 0) {
                        executionMonitor.setMessage(() -> "Writing row " + finalI);
                    } else {
                        final long finalRowCnt = rowCnt;
                        executionMonitor.setProgress(i / (double)rowCnt, () -> "Writing row " + finalI + " of "
                                + finalRowCnt);
                    }
                }
                writer.writeRow(row);
                i++;
            }
        }
        executionMonitor.setProgress(1, "Temporary Parquet file has been written.");
    }

    private static DataTypeMappingConfiguration<ParquetType> createParquetTypeMappingConfiguration(
        final DataTableSpec inputTableSpec, final DBTable targetTable, final DBSession session,
        final ExecutionMonitor executionMonitor) throws CanceledExecutionException, SQLException {
        final DBTableSpec targetTableSpec =
            session.getAgent(DBMetadataReader.class).getDBTableSpec(executionMonitor, targetTable);
        final ParquetTypeMappingService typeMappingService = ParquetTypeMappingService.getInstance();
        final DataTypeMappingConfiguration<ParquetType> result =
            typeMappingService.createMappingConfiguration(KNIME_TO_EXTERNAL);
        int columnIndex = 0;
        for (final DBColumn column : targetTableSpec) {
            final String targetColumnTypeName = column.getColumnTypeName();
            final ParquetType parquetType = BIG_QUERY_SQL_TO_PARQUET_TYPE_MAPPING.get(targetColumnTypeName);
            if (parquetType == null) {
                throw new SQLException(
                    "Parquet type could not be found for the database type: " + targetColumnTypeName);
            }
            final DataType inputColumnType = inputTableSpec.getColumnSpec(columnIndex++).getType();
            result.addRule(inputColumnType,
                typeMappingService.getConsumptionPathsFor(inputColumnType).stream()
                    .filter(path -> path.getConsumerFactory().getDestinationType().equals(parquetType)).findFirst()
                    .orElseThrow(() -> new SQLException("Consumption path could not be found from " + inputColumnType
                        + " through " + parquetType + " to " + targetColumnTypeName + '.')));
        }
        return result;
    }

    @Override
    protected void buildDialog(final DialogBuilder builder, final List<DialogComponent> dialogComponents,
        final UnconnectedCsvLoaderNodeComponents customComponents) {
        final BigQueryLoaderNodeComponents bigQueryCustomComponents = (BigQueryLoaderNodeComponents)customComponents;
        final JPanel optionsPanel = createPanel();
        final Box optionsBox = createBox(false);
        optionsPanel.add(optionsBox);
        final JPanel tableNameComponentPanel = new PreferredHeightPanel();
        tableNameComponentPanel.add(bigQueryCustomComponents.getTableNameComponent().getComponentPanel());
        optionsBox.add(tableNameComponentPanel);
        optionsBox.add(Box.createVerticalStrut(0));
        final JPanel fileFormatSelectionComponentPanel = new PreferredHeightPanel();
        fileFormatSelectionComponentPanel
            .add(bigQueryCustomComponents.getFileFormatSelectionComponent().getComponentPanel());
        optionsBox.add(fileFormatSelectionComponentPanel);
        optionsBox.add(Box.createVerticalGlue());
        builder.addTab(Integer.MAX_VALUE, "Options", optionsPanel, true);
        final JPanel advancedPanel = createPanel();
        final Box advancedBox = createBox(false);
        advancedPanel.add(advancedBox);
        advancedBox.add(bigQueryCustomComponents.getFileFormatComponent().getComponentPanel());
        builder.addTab(Integer.MAX_VALUE, "Advanced", advancedPanel, true);
        bigQueryCustomComponents.getFileFormatSelectionModel()
            .addChangeListener(event -> onFileFormatSelectionChange(bigQueryCustomComponents));
    }

    @Override
    protected DBDataPortObjectSpec configureModel(final PortObjectSpec[] inSpecs,
        final List<SettingsModel> settingsModels, final UnconnectedCsvLoaderNodeSettings customSettings)
        throws InvalidSettingsException {
        final DBSessionPortObjectSpec sessionPortObjectSpec = (DBSessionPortObjectSpec)inSpecs[1];
        validateColumns(false, createModelConfigurationExecutionMonitor(sessionPortObjectSpec.getDBSession()),
            (DataTableSpec)inSpecs[0], sessionPortObjectSpec, customSettings.getTableNameModel().toDBTable());
        return super.configureModel(inSpecs, settingsModels, customSettings);
    }

    @Override
    protected BigQueryLoaderNodeComponents createCustomDialogComponents(final DialogDelegate dialogDelegate) {
        return new BigQueryLoaderNodeComponents(dialogDelegate, CHARSETS);
    }

    @Override
    protected BigQueryLoaderNodeSettings createCustomModelSettings(final ModelDelegate modelDelegate) {
        return new BigQueryLoaderNodeSettings(modelDelegate);
    }

    @Override
    protected List<DialogComponent> createDialogComponents(final UnconnectedCsvLoaderNodeComponents customComponents) {
        return asList(customComponents.getTableNameComponent(),
            ((BigQueryLoaderNodeComponents)customComponents).getFileFormatSelectionComponent(),
            customComponents.getFileFormatComponent());
    }

    @Override
    protected List<SettingsModel> createSettingsModels(final UnconnectedCsvLoaderNodeSettings customSettings) {
        return asList(customSettings.getTableNameModel(),
            ((BigQueryLoaderNodeSettings)customSettings).getFileFormatSelectionModel(),
            customSettings.getFileFormatModel());
    }

    @Override
    protected DBDataPortObject load(final ExecutionParameters<UnconnectedCsvLoaderNodeSettings> parameters)
        throws Exception {
        final BigQueryLoaderNodeSettings customSettings = (BigQueryLoaderNodeSettings)parameters.getCustomSettings();
        final BigQueryLoaderFileFormat fileFormat =
            BigQueryLoaderFileFormat.optionalValueOf(customSettings.getFileFormatSelectionModel().getStringValue())
                .orElseThrow(() -> new InvalidSettingsException("No file format is selected."));
        final ExecutionContext executionContext = parameters.getExecutionContext();
        final DBSessionPortObject sessionPortObject = parameters.getSessionPortObject();
        final DBTable table = customSettings.getTableNameModel().toDBTable();
        validateColumns(false, executionContext, parameters.getRowInput().getDataTableSpec(), sessionPortObject, table);
        executionContext.setProgress(0.1);
        final DBSession session = sessionPortObject.getDBSession();
        // Create and write to the temporary file
        final Path temporaryFile = createTempFile("knime2db", fileFormat.getFileExtension()).toPath();
        try (AutoCloseable temporaryFileDeleter = () -> delete(temporaryFile)) {
            executionContext.setMessage("Writing temporary file...");
            final ExecutionMonitor subExec = executionContext.createSubProgress(0.7);
            final BigQueryLoaderSettings additionalSettings;
            switch (fileFormat) {
                case CSV:
                    final FileWriterSettings fileWriterSettings =
                        customSettings.getFileFormatModel().getFileWriterSettings();
                    writeCsv(parameters.getRowInput(), temporaryFile, fileWriterSettings, subExec);
                    additionalSettings = new BigQueryLoaderSettings(fileFormat, fileWriterSettings);
                    break;
                case PARQUET:
                    writeParquet(parameters.getRowInput(), temporaryFile, table, session, subExec);
                    additionalSettings = new BigQueryLoaderSettings(fileFormat, null);
                    break;
                default:
                    throw new InvalidSettingsException("Unknown file format: " + fileFormat);
            }
            subExec.setProgress(1.0);
            // Load the data
            executionContext.setMessage("Loading file into BigQuery");
            session.getAgent(DBLoader.class).load(executionContext,
                new DBLoadTableFromFileParameters<>(null, temporaryFile.toString(), table, additionalSettings));
        }
        // Output
        return new DBDataPortObject(sessionPortObject,
            session.getAgent(DBMetadataReader.class).getDBDataObject(executionContext, table.getSchemaName(),
                table.getName(),
                sessionPortObject.getExternalToKnimeTypeMapping().resolve(
                    DBTypeMappingRegistry.getInstance().getDBTypeMappingService(session.getDBType()),
                    EXTERNAL_TO_KNIME)));
    }

    @Override
    protected void loadDialogSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs,
        final List<DialogComponent> dialogComponents, final UnconnectedCsvLoaderNodeComponents customComponents)
        throws NotConfigurableException {
        super.loadDialogSettingsFrom(settings, specs, dialogComponents, customComponents);
        onFileFormatSelectionChange((BigQueryLoaderNodeComponents)customComponents);
    }
}
