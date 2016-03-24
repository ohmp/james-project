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

package org.apache.james.mailbox.elasticsearch.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class EMailer {

    private final String name;
    private final String address;

    public EMailer(String name, String address) {
        this.name = name;
        this.address = address;
    }

    @JsonProperty(JsonMessageConstants.EMailer.NAME)
    public String getName() {
        return name;
    }

    @JsonProperty(JsonMessageConstants.EMailer.ADDRESS)
    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof EMailer) {
            EMailer otherEMailer = (EMailer) o;
            return Objects.equal(name, otherEMailer.name)
                && Objects.equal(address, otherEMailer.address);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, address);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("address", address)
            .toString();
    }
}
