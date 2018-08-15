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
package org.apache.james.jmap;

import java.util.concurrent.TimeUnit;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.metrics.api.NoopMetricFactory;
import org.apache.james.user.memory.MemoryUsersRepository;
import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.junit.Before;
import org.junit.Test;

public class UserProvisioningFilterThreadTest {

    private UserProvisioningFilter sut;
    private MemoryUsersRepository usersRepository;
    private MailboxSession session;

    @Before
    public void setUp() {
        usersRepository = MemoryUsersRepository.withoutVirtualHosting();
        session = new MockMailboxSession("username");
        sut = new UserProvisioningFilter(usersRepository, new NoopMetricFactory());
    }
    
    @Test
    public void createAccountIfNeededShouldNotThrowWhenCalledConcurrently() throws Exception {
        ConcurrentTestRunner testRunner = ConcurrentTestRunner.builder()
            .threadCount(2)
            .build((a, b) -> sut.createAccountIfNeeded(session))
            .run();

        testRunner.awaitTermination(1, TimeUnit.MINUTES);

        testRunner.assertNoException();
    }
}

