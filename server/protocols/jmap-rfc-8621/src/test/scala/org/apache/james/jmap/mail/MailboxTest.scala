/** **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                 *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/

package org.apache.james.jmap.mail

import org.apache.james.core.Username
import org.apache.james.mailbox.Role
import org.apache.james.mailbox.model.TestId
import org.scalatest.{MustMatchers, WordSpec}

import scala.compat.java8.OptionConverters._

class MailboxTest extends WordSpec with MustMatchers {
  "sortOrder" should  {
    "be comparable" in {
      SortOrder.of(4L).compare(SortOrder.of(3L)) must equal(1L)
      SortOrder.of(4L).compare(SortOrder.of(4L)) must equal(0L)
      SortOrder.of(4L).compare(SortOrder.of(5L)) must equal(-1L)
    }
  }

  "namespace" should  {
    "throw when null user" in {
      the [IllegalArgumentException] thrownBy {
        MailboxNamespace.delegated(null)
      } must have message "requirement failed"
    }
    "throw when empty user " in {
      the [IllegalArgumentException] thrownBy {
        MailboxNamespace.delegated(Username.of(""))
      } must have message "username should not be null or empty"
    }
    "throw when blank user" in {
      the [IllegalArgumentException] thrownBy {
        MailboxNamespace.delegated(Username.of(""))
      } must have message "username should not be null or empty"
    }
    "return personal when personal" in {
      MailboxNamespace.personal.`type` must be(MailboxNamespace.Personal)
    }
    "return None when personal" in {
      MailboxNamespace.personal.owner.isEmpty must be(true)
    }
    "return delegated when delegated" in {
      MailboxNamespace.delegated(Username.of("bob")).`type` must be(MailboxNamespace.Delegated)
    }
    "return owner when delegated" in {
      val username = Username.of("bob")
      MailboxNamespace.delegated(username).owner must be(Some(username))
    }
  }

  "mailbox name" should  {
    "throw when empty" in {
      the [IllegalArgumentException] thrownBy {
        MailboxName("")
      } must have message "requirement failed: 'name' is mandatory"
    }
    "throw when null" in {
      the [IllegalArgumentException] thrownBy {
        MailboxName(null)
      } must have message "requirement failed: 'name' is mandatory"
    }
  }

  "mailbox" should  {
    "throw when id is null" in {
      the [IllegalArgumentException] thrownBy {
        Mailbox(
          id = null,
          mailboxName = MailboxName("INBOX"),
          parentId = None,
          role = None,
          sortOrder = SortOrder.of(3L),
          totalEmails = 3L,
          unreadEmails = 4L,
          totalThreads = 5L,
          unreadThreads = 6L,
          mailboxRights = MailboxRights(mayReadItems = true,
            mayAddItems = true,
            mayRemoveItems = true,
            maySetSeen = true,
            maySetKeywords = true,
            mayCreateChild = true,
            mayRename = true,
            mayDelete = true,
            maySubmit = true),
          isSubscribed = true,
          namespace = MailboxNamespace(MailboxNamespace.Personal, None),
          rights = Rights.EMPTY,
          quotas = Quotas(Map()))
      } must have message "requirement failed: 'id' is mandatory"
    }
    "throw when name is null" in {
      the [IllegalArgumentException] thrownBy {
        Mailbox(
          id = TestId.of(42L),
          mailboxName = null,
          parentId = None,
          role = None,
          sortOrder = SortOrder.of(3L),
          totalEmails = 3L,
          unreadEmails = 4L,
          totalThreads = 5L,
          unreadThreads = 6L,
          mailboxRights = MailboxRights(mayReadItems = true,
            mayAddItems = true,
            mayRemoveItems = true,
            maySetSeen = true,
            maySetKeywords = true,
            mayCreateChild = true,
            mayRename = true,
            mayDelete = true,
            maySubmit = true),
          isSubscribed = true,
          namespace = MailboxNamespace(MailboxNamespace.Personal, None),
          rights = Rights.EMPTY,
          quotas = Quotas(Map()))
      } must have message "requirement failed: 'mailboxName' is mandatory"
    }
  }

  "mailbox hasRole" should  {
    "return false when None " in {
      Mailbox(
        id = TestId.of(42L),
        mailboxName = MailboxName("Inbox"),
        parentId = None,
        role = None,
        sortOrder = SortOrder.of(3L),
        totalEmails = 3L,
        unreadEmails = 4L,
        totalThreads = 5L,
        unreadThreads = 6L,
        mailboxRights = MailboxRights(mayReadItems = true,
          mayAddItems = true,
          mayRemoveItems = true,
          maySetSeen = true,
          maySetKeywords = true,
          mayCreateChild = true,
          mayRename = true,
          mayDelete = true,
          maySubmit = true),
        isSubscribed = true,
        namespace = MailboxNamespace(MailboxNamespace.Personal, None),
        rights = Rights.EMPTY,
        quotas = Quotas(Map())).hasRole(Role.INBOX) must be(false)
    }
    "return false when different" in {
      Mailbox(
        id = TestId.of(42L),
        mailboxName = MailboxName("Inbox"),
        parentId = None,
        role = Some(Role.OUTBOX),
        sortOrder = SortOrder.of(3L),
        totalEmails = 3L,
        unreadEmails = 4L,
        totalThreads = 5L,
        unreadThreads = 6L,
        mailboxRights = MailboxRights(mayReadItems = true,
          mayAddItems = true,
          mayRemoveItems = true,
          maySetSeen = true,
          maySetKeywords = true,
          mayCreateChild = true,
          mayRename = true,
          mayDelete = true,
          maySubmit = true),
        isSubscribed = true,
        namespace = MailboxNamespace(MailboxNamespace.Personal, None),
        rights = Rights.EMPTY,
        quotas = Quotas(Map())).hasRole(Role.INBOX) must be(false)
    }
    "return true when equals" in {
      Mailbox(
        id = TestId.of(42L),
        mailboxName = MailboxName("Inbox"),
        parentId = None,
        role = Some(Role.INBOX),
        sortOrder = SortOrder.of(3L),
        totalEmails = 3L,
        unreadEmails = 4L,
        totalThreads = 5L,
        unreadThreads = 6L,
        mailboxRights = MailboxRights(mayReadItems = true,
          mayAddItems = true,
          mayRemoveItems = true,
          maySetSeen = true,
          maySetKeywords = true,
          mayCreateChild = true,
          mayRename = true,
          mayDelete = true,
          maySubmit = true),
        isSubscribed = true,
        namespace = MailboxNamespace(MailboxNamespace.Personal, None),
        rights = Rights.EMPTY,
        quotas = Quotas(Map())).hasRole(Role.INBOX) must be(true)
    }
  }

  "mailbox hasSystemRole" should  {
    "return false when None" in {
      Mailbox(
        id = TestId.of(42L),
        mailboxName = MailboxName("Inbox"),
        parentId = None,
        role = None,
        sortOrder = SortOrder.of(3L),
        totalEmails = 3L,
        unreadEmails = 4L,
        totalThreads = 5L,
        unreadThreads = 6L,
        mailboxRights = MailboxRights(mayReadItems = true,
          mayAddItems = true,
          mayRemoveItems = true,
          maySetSeen = true,
          maySetKeywords = true,
          mayCreateChild = true,
          mayRename = true,
          mayDelete = true,
          maySubmit = true),
        isSubscribed = true,
        namespace = MailboxNamespace(MailboxNamespace.Personal, None),
        rights = Rights.EMPTY,
        quotas = Quotas(Map())).hasSystemRole must be(false)
    }
    "return false when not system" in {
      Mailbox(
        id = TestId.of(42L),
        mailboxName = MailboxName("Inbox"),
        parentId = None,
        role = Role.from("any").asScala,
        sortOrder = SortOrder.of(3L),
        totalEmails = 3L,
        unreadEmails = 4L,
        totalThreads = 5L,
        unreadThreads = 6L,
        mailboxRights = MailboxRights(mayReadItems = true,
          mayAddItems = true,
          mayRemoveItems = true,
          maySetSeen = true,
          maySetKeywords = true,
          mayCreateChild = true,
          mayRename = true,
          mayDelete = true,
          maySubmit = true),
        isSubscribed = true,
        namespace = MailboxNamespace(MailboxNamespace.Personal, None),
        rights = Rights.EMPTY,
        quotas = Quotas(Map())).hasSystemRole must be(false)
    }
    "return true when system" in {
      Mailbox(
        id = TestId.of(42L),
        mailboxName = MailboxName("Inbox"),
        parentId = None,
        role = Some(Role.INBOX),
        sortOrder = SortOrder.of(3L),
        totalEmails = 3L,
        unreadEmails = 4L,
        totalThreads = 5L,
        unreadThreads = 6L,
        mailboxRights = MailboxRights(mayReadItems = true,
          mayAddItems = true,
          mayRemoveItems = true,
          maySetSeen = true,
          maySetKeywords = true,
          mayCreateChild = true,
          mayRename = true,
          mayDelete = true,
          maySubmit = true),
        isSubscribed = true,
        namespace = MailboxNamespace(MailboxNamespace.Personal, None),
        rights = Rights.EMPTY,
        quotas = Quotas(Map())).hasSystemRole must be(true)
    }
  }
}
