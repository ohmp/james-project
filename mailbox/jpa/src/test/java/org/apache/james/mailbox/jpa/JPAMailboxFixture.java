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

package org.apache.james.mailbox.jpa;

import java.util.List;

import org.apache.james.backends.jpa.TableDeclaration;
import org.apache.james.mailbox.jpa.mail.model.JPAMailbox;
import org.apache.james.mailbox.jpa.mail.model.JPAMailboxAnnotation;
import org.apache.james.mailbox.jpa.mail.model.JPAProperty;
import org.apache.james.mailbox.jpa.mail.model.JPAUserFlag;
import org.apache.james.mailbox.jpa.mail.model.openjpa.AbstractJPAMailboxMessage;
import org.apache.james.mailbox.jpa.mail.model.openjpa.JPAMailboxMessage;
import org.apache.james.mailbox.jpa.quota.model.JpaCurrentQuota;
import org.apache.james.mailbox.jpa.quota.model.MaxDomainMessageCount;
import org.apache.james.mailbox.jpa.quota.model.MaxDomainStorage;
import org.apache.james.mailbox.jpa.quota.model.MaxGlobalMessageCount;
import org.apache.james.mailbox.jpa.quota.model.MaxGlobalStorage;
import org.apache.james.mailbox.jpa.quota.model.MaxUserMessageCount;
import org.apache.james.mailbox.jpa.quota.model.MaxUserStorage;
import org.apache.james.mailbox.jpa.user.model.JPASubscription;

import com.google.common.collect.ImmutableList;

public interface JPAMailboxFixture {

    List<TableDeclaration> MAILBOX_PERSISTANCE_CLASSES = ImmutableList.of(
        new TableDeclaration(JPAMailbox.class, "JAMES_MAILBOX"),
        new TableDeclaration(AbstractJPAMailboxMessage.class, "JAMES_MAIL"),
        new TableDeclaration(JPAMailboxMessage.class, "JAMES_MAIL"),
        new TableDeclaration(JPAProperty.class, "JAMES_MAIL_PROPERTY"),
        new TableDeclaration(JPAUserFlag.class, "JAMES_MAIL_USERFLAG"),
        new TableDeclaration(JPAMailboxAnnotation.class, "JAMES_MAILBOX_ANNOTATION"),
        new TableDeclaration(JPASubscription.class, "JAMES_SUBSCRIPTION")
    );

    List<TableDeclaration> QUOTA_PERSISTANCE_CLASSES = ImmutableList.of(
        new TableDeclaration(MaxGlobalMessageCount.class, "JAMES_MAX_GLOBAL_MESSAGE_COUNT"),
        new TableDeclaration(MaxGlobalStorage.class, "JAMES_MAX_GLOBAL_STORAGE"),
        new TableDeclaration(MaxDomainStorage.class, "JAMES_MAX_DOMAIN_STORAGE"),
        new TableDeclaration(MaxDomainMessageCount.class, "JAMES_MAX_DOMAIN_MESSAGE_COUNT"),
        new TableDeclaration(MaxUserMessageCount.class, "JAMES_MAX_USER_MESSAGE_COUNT"),
        new TableDeclaration(MaxUserStorage.class, "JAMES_MAX_USER_STORAGE"),
        new TableDeclaration(JpaCurrentQuota.class, "JAMES_QUOTA_CURRENTQUOTA")
    );
}
