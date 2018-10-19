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

package org.apache.james.transport.mailets.delivery;

import java.util.Optional;

import org.apache.james.core.MailAddress;

public class DeliveryUtils {

    public static String prettyPrint(MailAddress mailAddress) {
        return prettyPrint(Optional.ofNullable(mailAddress)
            .filter(address -> ! address.isNullSender()));
    }

    public static String prettyPrint(Optional<MailAddress> mailAddress) {
        return mailAddress.map(MailAddress::asPrettyString)
            .orElse(MailAddress.NULL_SENDER_AS_STRING);
    }

}
