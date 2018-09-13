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

package org.apache.james.transport.mailets.remote.delivery;

import java.util.Arrays;
import java.util.List;

import javax.mail.Address;

import org.apache.james.core.MailAddress;
import org.apache.james.util.OptionalUtils;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

public class AddressesArrayToMailAddressListConverter {
    public static List<MailAddress> getAddressesAsMailAddress(Address[] addresses) {
        if (addresses == null) {
            return ImmutableList.of();
        }
        return Arrays.stream(addresses)
            .map(Address::toString)
            .map(MailAddress::asMailAddressOptional)
            .flatMap(OptionalUtils::toStream)
            .collect(Guavate.toImmutableList());
    }
}
