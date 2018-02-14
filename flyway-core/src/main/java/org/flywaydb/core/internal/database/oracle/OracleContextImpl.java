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

import org.flywaydb.core.internal.util.Pair;
import org.flywaydb.core.internal.util.jdbc.ContextImpl;

import java.util.List;

public class OracleContextImpl extends ContextImpl {
    /**
     * Whether the executor should treat exceptions as failures and stop the migration.
     */
    private boolean failOnException;



    /**
     * @return {@code true} if the executor should treat exceptions as failures and stop the migration.
     */
    boolean getFailOnException() {
        return failOnException;
    }

    /**
     * Specify whether the executor should treat exceptions as failures and stop the migration.
     *
     * @param failOnException {@code true} if the executor should halt when an exception is thrown,
     * or {@code false} if the executor should ignore exceptions and continue with the rest of the migration.
     */
    public void setFailOnException(boolean failOnException) {
        this.failOnException = failOnException;
    }

































































}