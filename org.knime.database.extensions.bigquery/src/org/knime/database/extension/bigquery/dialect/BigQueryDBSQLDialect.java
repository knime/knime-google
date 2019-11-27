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
package org.knime.database.extension.bigquery.dialect;

import static java.util.Objects.requireNonNull;
import static org.knime.database.attribute.AttributeCollection.Accessibility.EDITABLE;
import static org.knime.database.attribute.AttributeCollection.Accessibility.HIDDEN;

import org.knime.database.SQLCommand;
import org.knime.database.SQLQuery;
import org.knime.database.attribute.Attribute;
import org.knime.database.attribute.AttributeCollection;
import org.knime.database.attribute.AttributeCollection.Accessibility;
import org.knime.database.dialect.DBSQLDialect;
import org.knime.database.dialect.DBSQLDialectFactory;
import org.knime.database.dialect.DBSQLDialectFactoryParameters;
import org.knime.database.dialect.DBSQLDialectParameters;
import org.knime.database.dialect.impl.SQL92DBSQLDialect;
import org.knime.database.model.DBSchemaObject;

/**
 * {@link DBSQLDialect} for Google BigQuery databases.
 *
 * @author Noemi Balassa
 * @see <a href="https://cloud.google.com/bigquery/docs/reference/standard-sql/query-syntax">Google BigQuery Standard
 *      SQL Query Syntax</a>
 * @see <a href="https://cloud.google.com/bigquery/docs/reference/standard-sql/data-definition-language">Google BigQuery
 *      Data Definition Language Statements</a>
 * @see <a href="https://cloud.google.com/bigquery/docs/reference/standard-sql/dml-syntax">Google BigQuery Data
 *      Manipulation Language Syntax</a>
 */
public class BigQueryDBSQLDialect extends SQL92DBSQLDialect {
    /**
     * {@link DBSQLDialectFactory} that produces {@link BigQueryDBSQLDialect} instances.
     *
     * @author Noemi Balassa
     */
    public static class Factory implements DBSQLDialectFactory {
        @Override
        public DBSQLDialect createDialect(final DBSQLDialectFactoryParameters parameters) {
            return new BigQueryDBSQLDialect(this,
                new DBSQLDialectParameters(requireNonNull(parameters, "parameters").getSessionReference()));
        }

        @Override
        public AttributeCollection getAttributes() {
            return ATTRIBUTES;
        }

        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public String getName() {
            return NAME;
        }
    }

    /**
     * Attribute that indicates the capability to drop tables.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_CAPABILITY_DROP_TABLE
     */
    @SuppressWarnings("hiding")
    public static final Attribute<Boolean> ATTRIBUTE_CAPABILITY_DROP_TABLE;

    /**
     * Attribute that indicates {@code CASE} expression capability.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_CAPABILITY_EXPRESSION_CASE
     */
    @SuppressWarnings("hiding")
    public static final Attribute<Boolean> ATTRIBUTE_CAPABILITY_EXPRESSION_CASE;

    /**
     * Attribute that indicates the capability to insert into a table via a select statement.
     *
     * @see #createInsertAsSelectStatement(DBSchemaObject, SQLQuery, String...)
     */
    @SuppressWarnings("hiding")
    public static final Attribute<Boolean> ATTRIBUTE_CAPABILITY_INSERT_AS_SELECT;

    /**
     * Attribute that indicates the capability to use MINUS operation.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_CAPABILITY_MINUS_OPERATION
     */
    @SuppressWarnings("hiding")
    public static final Attribute<Boolean> ATTRIBUTE_CAPABILITY_MINUS_OPERATION;

    /**
     * Attribute that indicates the capability of table references being derived tables.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_CAPABILITY_TABLE_REFERENCE_DERIVED_TABLE
     */
    @SuppressWarnings("hiding")
    public static final Attribute<Boolean> ATTRIBUTE_CAPABILITY_TABLE_REFERENCE_DERIVED_TABLE;

    /**
     * Attribute that contains the literal keyword or keyword for {@code CREATE [ ( GLOBAL | LOCAL ) TEMPORARY ] TABLE}
     * statements.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_SYNTAX_CREATE_TABLE_TEMPORARY
     */
    @SuppressWarnings("hiding")
    public static final Attribute<String> ATTRIBUTE_SYNTAX_CREATE_TABLE_TEMPORARY;

    /**
     * Attribute that contains the closing identifier delimiter, e.g.&nbsp;{@code "} or {@code ]}.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_SYNTAX_DELIMITER_IDENTIFIER_CLOSING
     */
    @SuppressWarnings("hiding")
    public static final Attribute<String> ATTRIBUTE_SYNTAX_DELIMITER_IDENTIFIER_CLOSING;

    /**
     * Attribute that contains the opening identifier delimiter, e.g.&nbsp;{@code "} or {@code [}.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_SYNTAX_DELIMITER_IDENTIFIER_OPENING
     */
    @SuppressWarnings("hiding")
    public static final Attribute<String> ATTRIBUTE_SYNTAX_DELIMITER_IDENTIFIER_OPENING;

    /**
     * Attribute that contains the replacement for non-word identifier characters.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_SYNTAX_IDENTIFIER_NON_WORD_CHARACTER_REPLACEMENT
     */
    @SuppressWarnings("hiding")
    public static final Attribute<String> ATTRIBUTE_SYNTAX_IDENTIFIER_NON_WORD_CHARACTER_REPLACEMENT;

    /**
     * Attribute that indicates to replace non-word characters in identifiers.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_SYNTAX_IDENTIFIER_REPLACE_NON_WORD_CHARACTERS
     */
    @SuppressWarnings("hiding")
    public static final Attribute<Boolean> ATTRIBUTE_SYNTAX_IDENTIFIER_REPLACE_NON_WORD_CHARACTERS;

    /**
     * Attribute that contains the keyword between the two queries in case of minus operation.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_SYNTAX_MINUS_OPERATOR_KEYWORD
     */
    @SuppressWarnings("hiding")
    public static final Attribute<String> ATTRIBUTE_SYNTAX_MINUS_OPERATOR_KEYWORD;

    /**
     * Attribute that contains the keyword between the table/view name or derived table and the correlation name in
     * table reference expressions.
     *
     * @see SQL92DBSQLDialect#ATTRIBUTE_SYNTAX_TABLE_REFERENCE_KEYWORD
     */
    @SuppressWarnings("hiding")
    public static final Attribute<String> ATTRIBUTE_SYNTAX_TABLE_REFERENCE_KEYWORD;

    /**
     * The {@link AttributeCollection} of this {@link DBSQLDialect}.
     *
     * @see Factory#getAttributes()
     */
    @SuppressWarnings("hiding")
    public static final AttributeCollection ATTRIBUTES;

    static {
        final AttributeCollection.Builder builder = AttributeCollection.builder(SQL92DBSQLDialect.ATTRIBUTES);
        // Capabilities
        builder.setGroup(SQL92DBSQLDialect.ATTRIBUTE_GROUP_CAPABILITIES);

        ATTRIBUTE_CAPABILITY_DROP_TABLE = builder.add(HIDDEN, SQL92DBSQLDialect.ATTRIBUTE_CAPABILITY_DROP_TABLE, true);

        ATTRIBUTE_CAPABILITY_EXPRESSION_CASE =
            builder.add(Accessibility.HIDDEN, SQL92DBSQLDialect.ATTRIBUTE_CAPABILITY_EXPRESSION_CASE, true);

        ATTRIBUTE_CAPABILITY_INSERT_AS_SELECT =
            builder.add(Accessibility.HIDDEN, SQL92DBSQLDialect.ATTRIBUTE_CAPABILITY_INSERT_AS_SELECT, true);

        ATTRIBUTE_CAPABILITY_MINUS_OPERATION =
            builder.add(Accessibility.HIDDEN, SQL92DBSQLDialect.ATTRIBUTE_CAPABILITY_MINUS_OPERATION, true);

        ATTRIBUTE_CAPABILITY_TABLE_REFERENCE_DERIVED_TABLE = builder.add(Accessibility.HIDDEN,
            SQL92DBSQLDialect.ATTRIBUTE_CAPABILITY_TABLE_REFERENCE_DERIVED_TABLE, true);

        // Syntax
        builder.setGroup("knime.db.dialect.syntax", "Dialect syntax");

        ATTRIBUTE_SYNTAX_CREATE_TABLE_TEMPORARY =
            builder.add(Accessibility.HIDDEN, SQL92DBSQLDialect.ATTRIBUTE_SYNTAX_CREATE_TABLE_TEMPORARY, "TEMPORARY");

        ATTRIBUTE_SYNTAX_DELIMITER_IDENTIFIER_CLOSING =
            builder.add(EDITABLE, SQL92DBSQLDialect.ATTRIBUTE_SYNTAX_DELIMITER_IDENTIFIER_CLOSING, "`");

        ATTRIBUTE_SYNTAX_DELIMITER_IDENTIFIER_OPENING =
            builder.add(EDITABLE, SQL92DBSQLDialect.ATTRIBUTE_SYNTAX_DELIMITER_IDENTIFIER_OPENING, "`");

        ATTRIBUTE_SYNTAX_IDENTIFIER_NON_WORD_CHARACTER_REPLACEMENT =
            builder.add(EDITABLE, SQL92DBSQLDialect.ATTRIBUTE_SYNTAX_IDENTIFIER_NON_WORD_CHARACTER_REPLACEMENT, "_");

        ATTRIBUTE_SYNTAX_IDENTIFIER_REPLACE_NON_WORD_CHARACTERS =
            builder.add(EDITABLE, SQL92DBSQLDialect.ATTRIBUTE_SYNTAX_IDENTIFIER_REPLACE_NON_WORD_CHARACTERS, true);

        ATTRIBUTE_SYNTAX_MINUS_OPERATOR_KEYWORD = builder.add(Accessibility.HIDDEN,
            SQL92DBSQLDialect.ATTRIBUTE_SYNTAX_MINUS_OPERATOR_KEYWORD, "EXCEPT DISTINCT");

        ATTRIBUTE_SYNTAX_TABLE_REFERENCE_KEYWORD =
            builder.add(Accessibility.HIDDEN, SQL92DBSQLDialect.ATTRIBUTE_SYNTAX_TABLE_REFERENCE_KEYWORD, "AS");

        ATTRIBUTES = builder.build();
    }

    /**
     * The {@linkplain #getId() ID} of the {@link BigQueryDBSQLDialect} instances.
     *
     * @see DBSQLDialectFactory#getId()
     * @see BigQueryDBSQLDialect.Factory#getId()
     */
    @SuppressWarnings("hiding")
    public static final String ID = "bigquery";

    /**
     * The {@linkplain #getDescription() description} of the {@link BigQueryDBSQLDialect} instances.
     *
     * @see DBSQLDialectFactory#getDescription()
     * @see BigQueryDBSQLDialect.Factory#getDescription()
     */
    static final String DESCRIPTION = "Google BigQuery";

    /**
     * The {@linkplain #getName() name} of the {@link BigQueryDBSQLDialect} instances.
     *
     * @see DBSQLDialectFactory#getName()
     * @see BigQueryDBSQLDialect.Factory#getName()
     */
    static final String NAME = "Google BigQuery";

    /**
     * Constructs a {@link BigQueryDBSQLDialect} object.
     *
     * @param factory the factory that produces the instance.
     * @param dialectParameters the dialect-specific parameters controlling statement creation.
     */
    protected BigQueryDBSQLDialect(final DBSQLDialectFactory factory, final DBSQLDialectParameters dialectParameters) {
        super(factory, dialectParameters);
    }

    @Override
    public SQLQuery createLimitQueryWithOffset(final SQLQuery query, final long offset, final long count) {
        return new SQLQuery(asTable(selectAll().getPart() + "FROM (" + query.getQuery() + "\n)", getTempTableName())
            + " LIMIT " + count + " OFFSET " + offset);
    }

    @Override
    public SQLCommand[] createMergeStatement(final DBSchemaObject schemaObject, final String[] setColumns,
        final String[] whereColumns) {
        // The MERGE statement cannot be used because the automatic types of the input columns are not always correct.
        /*final String[] columns = ArrayUtils.addAll(setColumns, whereColumns);
        return new SQLCommand[]{new SQLCommand("MERGE INTO " + createFullName(schemaObject)
            + stream(columns).map(column -> "? " + delimit(column)).collect(joining(",", " d USING (SELECT ", ") t"))
            + stream(whereColumns).map(column -> delimit(column)).map(column -> "d." + column + " = t." + column)
                .collect(joining(" AND ", " ON (", ")"))
            + stream(setColumns).map(column -> delimit(column)).map(column -> "d." + column + " = t." + column)
                .collect(joining(",", " WHEN MATCHED THEN UPDATE SET ", ""))
            + stream(columns).map(column -> delimit(column))
                .collect(joining(",", " WHEN NOT MATCHED THEN INSERT (", ")"))
            + stream(columns).map(column -> "t." + delimit(column)).collect(joining(",", " VALUES (", ")")))};*/
        return super.createMergeStatement(schemaObject, setColumns, whereColumns);
    }

    @Override
    public SQLCommand[] getCreateTableAsSelectStatement(final DBSchemaObject schemaObject, final SQLQuery sql) {
        return new SQLCommand[]{
            new SQLCommand("CREATE TABLE " + createFullName(schemaObject) + " AS " + sql.getQuery())};
    }

    @Override
    public SQLCommand getDropTableStatement(final DBSchemaObject schemaObject, final boolean cascade) {
        return super.getDropTableStatement(schemaObject, false);
    }

    @Override
    public SQLCommand[] getReplaceTableAsSelectStatement(final DBSchemaObject schemaObject, final SQLQuery sql) {
        return new SQLCommand[]{
            new SQLCommand("CREATE OR REPLACE TABLE " + createFullName(schemaObject) + " AS " + sql.getQuery())};
    }
}
