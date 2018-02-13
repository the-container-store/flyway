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

/**
 * A sql statement from a script that can be executed at once against a database.
 */
public abstract class AbstractSqlStatement implements SqlStatement {
    /**
     * The original line number where the statement was located in the script it came from.
     */
    protected int lineNumber;

    /**
     * The sql to send to the database.
     */
    protected String sql;

    /**
     * Whether the executor should treat exceptions as failures and stop the migration.
     */
    private boolean failOnException;

    /**
     * Whether Oracle DBMS_OUTPUT should be displayed
     */
    private boolean echoDbmsOutput;

    public AbstractSqlStatement(String sql, int lineNumber) {
        this.sql = sql;
        this.lineNumber = lineNumber;
        this.failOnException = true;
        this.echoDbmsOutput = false;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String getSql() {
        return sql;
    }

    @Override
    public boolean getFailOnException() {
        return failOnException;
    }

    @Override
    public void setFailOnException(boolean failOnException) {
        this.failOnException = failOnException;
    }

    @Override
    public boolean getEchoDbmsOutput() {
        return echoDbmsOutput;
    }

    @Override
    public void setEchoDbmsOutput(boolean echoDbmsOutput) {
        this.echoDbmsOutput = echoDbmsOutput;
    }
}