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
package org.apache.james.transport.matchers;

import java.util.Collection;
import javax.mail.MessagingException;
import org.apache.james.core.MailAddress;
import org.apache.mailet.base.test.FakeMail;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;

public class HasPriorityTest extends PriorityTest {
    public HasPriorityTest() {
        super("5", new HasPriority());
    }

    @Before
    public void setup() throws Exception {
        super.setup();
    }

    @Test
    public void shouldMatchWhenPriorityMatch() throws MessagingException {
        FakeMail fakeMail = this.getFakeMail(5);

        Collection<MailAddress> actual = matcher.match(fakeMail);

        assertThat(actual).containsOnly(testRecipient);
    }

    @Test
    public void shouldNotMatchWhenPriorityDoesNotMatch() throws MessagingException {
        FakeMail fakeMail = this.getFakeMail(7);

        Collection<MailAddress> actual = matcher.match(fakeMail);

        assertThat(actual).isNull();
    }

}
