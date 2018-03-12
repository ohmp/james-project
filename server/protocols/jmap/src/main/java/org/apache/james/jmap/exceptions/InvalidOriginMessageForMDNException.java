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

package org.apache.james.jmap.exceptions;

import org.apache.james.jmap.model.JmapMDN;

public class InvalidOriginMessageForMDNException extends Exception {
    private static final String MISSING_HEADER = "Header is missing";

    public static InvalidOriginMessageForMDNException missingField(String fieldName) {
        return new InvalidOriginMessageForMDNException(MISSING_HEADER, fieldName);
    }

    public static InvalidOriginMessageForMDNException headerMismatch(String expectedValue) {
        return new InvalidOriginMessageForMDNException(JmapMDN.DISPOSITION_NOTIFICATION_TO,
            String.format("'%s' field of targeted message do not match the '%s' field. Expected value was '%s'",
                JmapMDN.DISPOSITION_NOTIFICATION_TO, JmapMDN.RETURN_PATH, expectedValue));
    }

    private final String invalidField;
    private final String explanation;

    public InvalidOriginMessageForMDNException(String invalidField, String explanation) {
        this.invalidField = invalidField;
        this.explanation = explanation;
    }

    public String getInvalidHeader() {
        return invalidField;
    }

    public String getExplanation() {
        return explanation;
    }
}
