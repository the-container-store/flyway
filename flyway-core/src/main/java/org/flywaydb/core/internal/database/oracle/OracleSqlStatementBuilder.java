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
import org.flywaydb.core.internal.database.Delimiter;
import org.flywaydb.core.internal.sqlscript.SqlStatement;
import org.flywaydb.core.internal.database.SqlStatementBuilder;
import org.flywaydb.core.internal.util.StringUtils;
import org.flywaydb.core.internal.util.jdbc.ContextImpl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SqlStatementBuilder supporting Oracle-specific PL/SQL constructs.
 */
public class OracleSqlStatementBuilder extends SqlStatementBuilder {
    private static final Log LOG = LogFactory.getLog(SqlStatementBuilder.class);

    /**
     * Characters that are acceptable at the end of a SQL*Plus directive line
     */
    private static final String SQL_PLUS_OPTIONAL_LINE_ENDING = "\\s*;?\\s*(--.*)?";

    /**
     * Regex for keywords that can appear before a string literal without being separated by a space.
     */
    private static final Pattern KEYWORDS_BEFORE_STRING_LITERAL_REGEX =
            Pattern.compile("^(N|DATE|IF|ELSIF|SELECT|IMMEDIATE|RETURN|IS)('.*)");

    /**
     * Regex for keywords that can appear after a string literal without being separated by a space.
     */
    private static final Pattern KEYWORDS_AFTER_STRING_LITERAL_REGEX = Pattern.compile("(.*')(USING|THEN|FROM|AND|OR|AS)(?!.)");

    private static Pattern toRegex(String... commands) {
        return Pattern.compile("^(" + StringUtils.arrayToDelimitedString("|", commands) + ")(\\s.*)?");
    }




















































































    private static final Pattern DECLARE_BEGIN_REGEX = toRegex("DECLARE|BEGIN");
    private static final Pattern PLSQL_REGEX = Pattern.compile(
            "^CREATE(\\s+OR\\s+REPLACE)?(\\s+(NON)?EDITIONABLE)?\\s+(FUNCTION|PROCEDURE|PACKAGE|TYPE|TRIGGER).*");
    private static final Pattern JAVA_REGEX = Pattern.compile(
            "^CREATE(\\s+OR\\s+REPLACE)?(\\s+AND\\s+(RESOLVE|COMPILE))?(\\s+NOFORCE)?\\s+JAVA\\s+(SOURCE|RESOURCE|CLASS).*");

    /**
     * Delimiter of PL/SQL blocks and statements.
     */
    private static final Delimiter PLSQL_DELIMITER = new Delimiter("/", true);

    /**
     * Holds the beginning of the statement.
     */
    private String statementStart = "";

    /**
     * Whether an exception in this statement should cause the migration to fail.
     */
    private boolean failOnException;

    /**
     * Whether Oracle DBMS_OUTPUT should be displayed for this statement
     */
    private boolean echoDbmsOutput;

    public OracleSqlStatementBuilder(Delimiter defaultDelimiter) {
        super(defaultDelimiter);
    }

































    @Override
    public boolean isTerminated() {
        return super.isTerminated() || isExceptionDirective() || isServerOutputDirective();
    }

    @Override
    public <C extends ContextImpl> SqlStatement<C> getSqlStatement() {
        //noinspection unchecked
        return (SqlStatement<C>) new OracleSqlStatement(lineNumber, statement.toString(),
                failOnException, echoDbmsOutput);
    }

    @Override
    protected void applyStateChanges(String line) {
        super.applyStateChanges(line);

        if (StringUtils.countOccurrencesOf(statementStart, " ") < 8) {
            statementStart += line;
            statementStart += " ";
            statementStart = statementStart.replaceAll("\\s+", " ");
        }
    }

    @Override
    protected Delimiter changeDelimiterIfNecessary(String line, Delimiter delimiter) {
        if (DECLARE_BEGIN_REGEX.matcher(line).matches()) {
            return PLSQL_DELIMITER;
        }

        if (PLSQL_REGEX.matcher(statementStart).matches() || JAVA_REGEX.matcher(statementStart).matches()) {
            return PLSQL_DELIMITER;
        }

        return delimiter;
    }

    @Override
    protected String cleanToken(String token) {
        if (token.startsWith("'") && token.endsWith("'")) {
            return token;
        }

        Matcher beforeMatcher = KEYWORDS_BEFORE_STRING_LITERAL_REGEX.matcher(token);
        if (beforeMatcher.find()) {
            token = beforeMatcher.group(2);
        }

        Matcher afterMatcher = KEYWORDS_AFTER_STRING_LITERAL_REGEX.matcher(token);
        if (afterMatcher.find()) {
            token = afterMatcher.group(1);
        }

        return token;
    }

    @Override
    protected String simplifyLine(String line) {
        String simplifiedQQuotes = StringUtils.replaceAll(StringUtils.replaceAll(line, "q'(", "q'["), ")'", "]'");
        return super.simplifyLine(simplifiedQQuotes);
    }

    @Override
    protected String extractAlternateOpenQuote(String token) {
        if (token.startsWith("Q'") && (token.length() >= 3)) {
            return token.substring(0, 3);
        }
        return null;
    }

    @Override
    protected String computeAlternateCloseQuote(String openQuote) {
        char specialChar = openQuote.charAt(2);
        switch (specialChar) {
            case '[':
                return "]'";
            case '(':
                return ")'";
            case '{':
                return "}'";
            case '<':
                return ">'";
            default:
                return specialChar + "'";
        }
    }

    @Override
    public boolean canDiscard() {
        return super.canDiscard()
                || isDiscardableSqlPlusCommand()



                || statementStart.equals("/ "); // Lone / that can safely be ignored
    }

    private boolean isDiscardableSqlPlusCommand() {
        return statementStart.matches("SET\\s+(DEFINE|ECHO|TIMING|SQLBL(ANKLINES)?)\\s+(ON|OFF).*")
                || statementStart.matches("COLUMN\\s+SPOOLFILE.*")
                || statementStart.matches("SPOOL\\s+(OFF|&V_SPOOLFILE).*");
    }

    public boolean isIgnoreExceptionDirective() {
        return statementStart.matches("WHENEVER\\s+SQLERROR\\s+CONTINUE" + SQL_PLUS_OPTIONAL_LINE_ENDING);
    }

    public boolean isFailOnExceptionDirective() {
        return statementStart.matches("WHENEVER\\s+SQLERROR\\s+EXIT\\s+FAILURE" + SQL_PLUS_OPTIONAL_LINE_ENDING);
    }

    public boolean isExceptionDirective() {
        return isIgnoreExceptionDirective() || isFailOnExceptionDirective();
    }

    public boolean isServerOutputOnDirective() {
        return statementStart.matches("SET\\s+SERVEROUTPUT\\s+ON" + SQL_PLUS_OPTIONAL_LINE_ENDING);
    }

    public boolean isServerOutputOffDirective() {
        return statementStart.matches("SET\\s+SERVEROUTPUT\\s+OFF" + SQL_PLUS_OPTIONAL_LINE_ENDING);
    }

    public boolean isServerOutputDirective() {
        return isServerOutputOnDirective() || isServerOutputOffDirective();
    }

    public void setFailOnException(boolean failOnException) {
        this.failOnException = failOnException;
    }

    public void setEchoDbmsOutput(boolean echoDbmsOutput) {
        this.echoDbmsOutput = echoDbmsOutput;
    }











}