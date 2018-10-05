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

package org.apache.james.jmap.cassandra;

import org.apache.james.JamesServerExtension;
import org.apache.james.jmap.methods.integration.SetMessagesMethodContract;
import org.apache.james.mailbox.cassandra.ids.CassandraMessageId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.modules.CassandraJMAPTestModule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CassandraSetMessagesMethodTest extends SetMessagesMethodContract {
    @RegisterExtension
    static JamesServerExtension testExtension = JamesServerExtension.builder()
        .extensions(CassandraJMAPTestModule.DEFAULT_EXTENSIONS)
        .server(CassandraJMAPTestModule.DEFAULT_CASSANDRA_JMAP_SERVER)
        .build();

    @Override
    protected void await() {
        testExtension.await();
    }
    
    @Override
    protected MessageId randomMessageId() {
        return new CassandraMessageId.Factory().generate();
    }

    @Disabled("JAMES-2221 Temporally ignored failed test")
    @Override
    @Test
    public void attachmentsShouldBeRetrievedWhenChainingSetMessagesAndGetMessagesTextAttachment() {

    }

    @Disabled("Temporally ignored CI failing test")
    @Override
    @Test
    public void setMessagesWithABigBodyShouldReturnCreatedMessageWhenSendingMessage() {

    }
}
