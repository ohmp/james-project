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

package org.apache.james.mailbox.cassandra.mail;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.model.MailboxPath;

public class CassandraMailboxPathDAO {

    public CompletableFuture<CassandraId> retrieveId(MailboxPath mailboxPath) {
        throw new NotImplementedException();
    }

    public CompletableFuture<Stream<CassandraId>> listUserMailboxes(String namespace, String user) {
        throw new NotImplementedException();
    }

    public CompletableFuture<Boolean> save(MailboxPath mailboxPath, CassandraId mailboxId) {
        throw new NotImplementedException();
    }

    public CompletableFuture<Void> delete(MailboxPath mailboxPath) {
        throw new NotImplementedException();
    }

}
