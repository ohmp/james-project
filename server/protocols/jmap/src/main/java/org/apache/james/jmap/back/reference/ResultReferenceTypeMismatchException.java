/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap.back.reference;

public class ResultReferenceTypeMismatchException extends RuntimeException {
    public static void assertValidTypes(Class expected, Class actual) {
        if (!actual.isAssignableFrom(expected)) {
            throw new ResultReferenceTypeMismatchException(expected, actual);
        }
    }

    private final Class expected;
    private final Class actual;

    public ResultReferenceTypeMismatchException(Class expected, Class actual) {
        super("Invalid type " + actual + " requested as a resultReference for " + expected);
        this.expected = expected;
        this.actual = actual;
    }
}
