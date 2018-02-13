/*
 * Copyright 2010-2017 Boxfuse GmbH
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
package org.flywaydb.core.internal.dbsupport;

import org.flywaydb.core.internal.dbsupport.oracle.OracleDbSupport;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SqlScriptExceptionSmallTest {
    private JdbcTemplate jdbcTemplate;

    @Before
    public void before() {
        this.jdbcTemplate = mock(JdbcTemplate.class);
    }

    @Test(expected = FlywaySqlScriptException.class)
    public void failOnExceptionsByDefault() throws SQLException {
        doThrow(new SQLException()).when(this.jdbcTemplate).executeStatement("SELECT 'fail' FROM DUAL", false);

        String source = "SELECT 1 FROM DUAL;\n" +
                "SELECT 'fail' FROM DUAL;\n" +
                "SELECT 2 FROM DUAL;\n";

        SqlScript sqlScript = new SqlScript(source, new OracleDbSupport(null));
        sqlScript.execute(this.jdbcTemplate);
    }

    @Test
    public void ignoreExceptionsWhenDirected() throws SQLException {
        doThrow(new SQLException()).when(this.jdbcTemplate).executeStatement("SELECT 'fail' FROM DUAL", false);

        String source = "SELECT 1 FROM DUAL;\n" +
                "WHENEVER SQLERROR CONTINUE;\n" +
                "SELECT 'fail' FROM DUAL;\n" +
                "SELECT 2 FROM DUAL;\n" +
                "WHENEVER SQLERROR EXIT FAILURE;\n" +
                "SELECT 3 FROM DUAL;\n";

        SqlScript sqlScript = new SqlScript(source, new OracleDbSupport(null));
        sqlScript.execute(this.jdbcTemplate);

        verify(this.jdbcTemplate).executeStatement("SELECT 1 FROM DUAL", false);
        verify(this.jdbcTemplate).executeStatement("SELECT 2 FROM DUAL", false);
        verify(this.jdbcTemplate).executeStatement("SELECT 3 FROM DUAL", false);
    }

    @Test
    public void failOnExceptionWhenDirected() throws SQLException {
        doThrow(new SQLException()).when(this.jdbcTemplate).executeStatement("SELECT 'fail1' FROM DUAL", false);
        doThrow(new SQLException()).when(this.jdbcTemplate).executeStatement("SELECT 'fail2' FROM DUAL", false);

        boolean caughtException = false;
        String source = "SELECT 1 FROM DUAL;\n" +
                "WHENEVER SQLERROR CONTINUE;\n" +
                "SELECT 2 FROM DUAL;\n" +
                "SELECT 'fail1' FROM DUAL;\n" +
                "WHENEVER SQLERROR EXIT FAILURE;\n" +
                "SELECT 3 FROM DUAL;\n" +
                "SELECT 'fail2' FROM DUAL;\n" +
                "SELECT 4 FROM DUAL;\n";

        SqlScript sqlScript = new SqlScript(source, new OracleDbSupport(null));
        try {
            sqlScript.execute(this.jdbcTemplate);
        } catch (FlywaySqlScriptException ignore) {
            caughtException = true;
        }

        verify(this.jdbcTemplate).executeStatement("SELECT 1 FROM DUAL", false);
        verify(this.jdbcTemplate).executeStatement("SELECT 2 FROM DUAL", false);
        verify(this.jdbcTemplate).executeStatement("SELECT 3 FROM DUAL", false);
        verify(this.jdbcTemplate, never()).executeStatement("SELECT 4 FROM DUAL", false);
        assertTrue(caughtException);
    }

    @Test
    public void echoDbmsOutputWhenDirected() throws SQLException {
        String source = "SELECT 1 FROM DUAL;\n" +
                "set serveroutput on;\n" +
                "SELECT 2 FROM DUAL;\n" +
                "set serveroutput off;\n" +
                "SELECT 3 FROM DUAL;\n";

        SqlScript sqlScript = new SqlScript(source, new OracleDbSupport(null));
        sqlScript.execute(this.jdbcTemplate);

        verify(this.jdbcTemplate).executeStatement("SELECT 1 FROM DUAL", false);
        verify(this.jdbcTemplate).executeStatement("SELECT 2 FROM DUAL", true);
        verify(this.jdbcTemplate).executeStatement("SELECT 3 FROM DUAL", false);
    }
}
