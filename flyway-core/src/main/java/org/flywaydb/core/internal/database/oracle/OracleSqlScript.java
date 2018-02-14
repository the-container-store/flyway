/*
 * Copyright 2010-2018 Boxfuse GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core.internal.database.oracle;

import org.flywaydb.core.api.errorhandler.Error;
import org.flywaydb.core.api.errorhandler.ErrorHandler;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.database.Delimiter;
import org.flywaydb.core.internal.database.ExecutableSqlScript;
import org.flywaydb.core.internal.database.SqlStatementBuilder;
import org.flywaydb.core.internal.exception.FlywaySqlException;
import org.flywaydb.core.internal.sqlscript.SqlStatement;
import org.flywaydb.core.internal.util.StringUtils;
import org.flywaydb.core.internal.util.jdbc.JdbcTemplate;
import org.flywaydb.core.internal.util.jdbc.JdbcUtils;
import org.flywaydb.core.internal.util.scanner.Resource;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Oracle-specific SQL script.
 */
class OracleSqlScript extends ExecutableSqlScript<OracleContextImpl> {
    private static final Log LOG = LogFactory.getLog(OracleSqlScript.class);

    /**
     * Whether exceptions in the subsequent statements should cause the migration to fail.
     */
    private boolean failOnException;

    /**
     * Whether Oracle DBMS_OUTPUT should be displayed
     */
    private boolean echoDbmsOutput;








    OracleSqlScript(Resource sqlScriptResource, String sqlScriptSource, boolean mixed



    ) {
        super(sqlScriptResource, sqlScriptSource, mixed



        );
        this.failOnException = true;
        this.echoDbmsOutput = false;
    }

    @Override
    protected SqlStatementBuilder createSqlStatementBuilder() {
        return new OracleSqlStatementBuilder(Delimiter.SEMICOLON);
    }

    @Override
    protected void handleException(SQLException e, SqlStatement sqlStatement, OracleContextImpl context) {









        if (context.getFailOnException()) {
            super.handleException(e, sqlStatement, context);
        } else {
            LOG.warn(createExceptionWarning(resource, sqlStatement, e));
        }
    }

    private String createExceptionWarning(Resource resource, SqlStatement statement, SQLException e) {
        StringBuilder msg = new StringBuilder();
        if (resource == null) {
            msg.append("Script error (non-fatal)");
        } else {
            msg.append("Migration ").append(resource.getFilename()).append(" had a non-fatal error");
        }
        String underline = StringUtils.trimOrPad("", msg.length(), '-');
        msg.append("\n");
        msg.append(underline);
        msg.append("\n");

        SQLException rootCause = e;
        while (rootCause.getNextException() != null) {
            rootCause = rootCause.getNextException();
        }

        msg.append("SQL State  : ").append(rootCause.getSQLState()).append("\n");
        msg.append("Error Code : ").append(rootCause.getErrorCode()).append("\n");
        if (rootCause.getMessage() != null) {
            msg.append("Message    : ").append(rootCause.getMessage().trim()).append("\n");
        }

        if (resource != null) {
            msg.append("Location   : ").append(resource.getLocation()).append(" (").append(resource.getLocationOnDisk()).append(")\n");
        }
        if (statement != null) {
            msg.append("Line       : ").append(statement.getLineNumber()).append("\n");
            msg.append("Statement  : ").append(statement.getSql()).append("\n");
        }
        return msg.toString();
    }

    @Override
    protected void addStatement(List<SqlStatement<OracleContextImpl>> sqlStatements, SqlStatementBuilder sqlStatementBuilder) {
        OracleSqlStatementBuilder builder = (OracleSqlStatementBuilder) sqlStatementBuilder;
        if (builder.isExceptionDirective()) {
            processExceptionDirective(builder);
            SqlStatement<OracleContextImpl> sqlStatement = builder.getSqlStatement();
            LOG.debug("Found directive at line " + sqlStatement.getLineNumber() + ": " + sqlStatement.getSql());
            return;
        }
        if (builder.isServerOutputDirective()) {
            processServerOutputDirective(builder);
            SqlStatement<OracleContextImpl> sqlStatement = builder.getSqlStatement();
            LOG.debug("Found directive at line " + sqlStatement.getLineNumber() + ": " + sqlStatement.getSql());
            return;
        }

        builder.setFailOnException(failOnException);
        builder.setEchoDbmsOutput(echoDbmsOutput);

        super.addStatement(sqlStatements, builder);
    }

    /**
     * Records the instructions from an exception directive.
     * <p>
     * An exception directive is a statement whose only purpose is to instruct flyway how to handle exceptions
     * for all subsequent statements.
     *
     * @param sqlStatementBuilder the exception directive statement.
     */
    private void processExceptionDirective(OracleSqlStatementBuilder sqlStatementBuilder) {
        if (sqlStatementBuilder.isIgnoreExceptionDirective()) {
            failOnException = false;
        } else if (sqlStatementBuilder.isFailOnExceptionDirective()) {
            failOnException = true;
        }
    }

    /**
     * Records the instructions from an Oracle SQL*Plus serveroutput directive.
     * <p>
     * An Oracle SQL*Plus serveroutput directive tells Flyway whether or not to echo DBMS_OUTPUT
     * for all subsequent statements.
     *
     * @param sqlStatementBuilder the statement to evaluate
     */
    private void processServerOutputDirective(OracleSqlStatementBuilder sqlStatementBuilder) {
        if (sqlStatementBuilder.isServerOutputOnDirective()) {
            echoDbmsOutput = true;
        } else if (sqlStatementBuilder.isServerOutputOffDirective()) {
            echoDbmsOutput = false;
        }
    }

    @Override
    protected OracleContextImpl createContext() {
        return new OracleContextImpl();
    }



































































}