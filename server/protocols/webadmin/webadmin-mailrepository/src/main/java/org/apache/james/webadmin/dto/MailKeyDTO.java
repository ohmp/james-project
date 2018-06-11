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

package org.apache.james.webadmin.dto;

import java.util.Objects;

import org.apache.james.mailrepository.api.MailKey;

public class MailKeyDTO {

    private final MailKey mailKey;

    public MailKeyDTO(MailKey mailKey) {
        this.mailKey = mailKey;
    }

    public String getMailKey() {
        return mailKey.asString();
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof MailKeyDTO) {
            MailKeyDTO that = (MailKeyDTO) o;

            return Objects.equals(this.mailKey, that.mailKey);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(mailKey);
    }
}
