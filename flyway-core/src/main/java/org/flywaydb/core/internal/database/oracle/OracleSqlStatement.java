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

import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.internal.database.StandardSqlStatement;
import org.flywaydb.core.internal.util.jdbc.*;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class OracleSqlStatement extends StandardSqlStatement {
    private static final Log LOG = LogFactory.getLog(OracleSqlStatement.class);
    private static final int MAX_LINE_PER_DBMS_OUTPUT_FETCH = 1000;

    /**
     * Whether an exception in this statement should cause the migration to fail.
     */
    private boolean failOnException;

    /**
     * Whether Oracle DBMS_OUTPUT should be displayed for this statement
     */
    private boolean echoDbmsOutput;

    /**
     * Creates a new sql statement.
     *
     * @param lineNumber The original line number where the statement was located in the script it came from.
     * @param sql        The sql to send to the database.
     */
    OracleSqlStatement(int lineNumber, String sql, boolean failOnException, boolean echoDbmsOutput) {
        super(lineNumber, sql);
        this.failOnException = failOnException;
        this.echoDbmsOutput = echoDbmsOutput;
    }

    @Override
    public List<Result> execute(ContextImpl context, JdbcTemplate jdbcTemplate) throws SQLException {
        ((OracleContextImpl) context).setFailOnException(failOnException);
        if (echoDbmsOutput) {
            enableDbmsOutput(jdbcTemplate);
        }

        final List<Result> results = super.execute(context, jdbcTemplate);

        if (echoDbmsOutput) {
            logDbmsOutput(jdbcTemplate);
            disableDbmsOutput(jdbcTemplate);
        }
        return results;
    }

    private void logDbmsOutput(JdbcTemplate jdbcTemplate) throws SQLException {
        boolean fetchMoreLines = true;
        while(fetchMoreLines) {
            Integer linesFetched = (Integer) jdbcTemplate.execute(OracleSqlStatement::createFetchDbmsOutputStatement,
                    OracleSqlStatement::logDbmsOutput);
            // If we fetched the maximum number of lines, there may be more
            fetchMoreLines = (linesFetched == MAX_LINE_PER_DBMS_OUTPUT_FETCH);
        }
    }

    private void enableDbmsOutput(JdbcTemplate jdbcTemplate) throws SQLException {
        jdbcTemplate.execute("begin dbms_output.enable(NULL); end;");
    }

    private void disableDbmsOutput(JdbcTemplate jdbcTemplate) throws SQLException {
        jdbcTemplate.execute("begin dbms_output.disable(); end;");
    }

    private static CallableStatement createFetchDbmsOutputStatement(Connection con) throws SQLException {
        CallableStatement call = con.prepareCall("begin dbms_output.get_lines(?, ?); end;");
        call.registerOutParameter(1, Types.ARRAY, "DBMSOUTPUT_LINESARRAY");
        call.registerOutParameter(2, Types.INTEGER);
        call.setInt(2, MAX_LINE_PER_DBMS_OUTPUT_FETCH);
        return call;
    }

    private static Object logDbmsOutput(CallableStatement cs) throws SQLException {
        Array array = null;
        Integer linesReceived;
        try {
            array = cs.getArray(1);
            linesReceived = cs.getInt(2);
            Arrays.stream((String[]) array.getArray())
                    .filter(Objects::nonNull)
                    .forEach(LOG::info);
        } finally {
            if (array != null)
                array.free();
        }
        return linesReceived;
    }
}