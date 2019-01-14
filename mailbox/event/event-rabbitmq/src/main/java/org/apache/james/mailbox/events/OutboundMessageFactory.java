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

package org.apache.james.mailbox.events;

import static org.apache.james.backend.rabbitmq.Constants.EMPTY_ROUTING_KEY;

import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.AMQP;

import reactor.rabbitmq.OutboundMessage;

class OutboundMessageFactory {
    static OutboundMessage create(String exchangeName, byte[] payload, int firstDelivery) {
        return new OutboundMessage(
            exchangeName,
            EMPTY_ROUTING_KEY,
            new AMQP.BasicProperties.Builder()
                .headers(ImmutableMap.of(GroupRegistration.DELIVERY_COUNT, firstDelivery))
                .build(),
            payload);
    }
}
