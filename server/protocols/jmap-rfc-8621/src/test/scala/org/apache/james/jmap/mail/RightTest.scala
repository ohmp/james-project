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
import org.apache.james.mailbox.model.MailboxACL
import org.apache.james.mailbox.model.MailboxACL.{EntryKey, Rfc4314Rights, Right => JavaRight}
import org.scalatest.{MustMatchers, WordSpec}

import scala.jdk.CollectionConverters._

class RightTest extends WordSpec with MustMatchers {
  val NEGATIVE = true
  val USERNAME: Username = Username.of("user")
  val OTHER_USERNAME: Username = Username.of("otherUser")

  "Right ofCharacter" should  {
    "recognise 'a'" in {
      Right.forChar('a') must be(Some(Right.Administer))
    }
    "recognise 'e'" in {
      Right.forChar('e') must be(Some(Right.Expunge))
    }
    "recognise 'i'" in {
      Right.forChar('i') must be(Some(Right.Insert))
    }
    "recognise 'l'" in {
      Right.forChar('l') must be(Some(Right.Lookup))
    }
    "recognise 'r'" in {
      Right.forChar('r') must be(Some(Right.Read))
    }
    "recognise 'w'" in {
      Right.forChar('w') must be(Some(Right.Write))
    }
    "recognise 't'" in {
      Right.forChar('t') must be(Some(Right.DeleteMessages))
    }
    "return empty when unknown" in {
      Right.forChar('k') must be(None)
    }
  }
  "From ACL" should  {
    "filter out group entries" in {
      val acl = new MailboxACL(Map(
        EntryKey.createGroupEntryKey("group") -> Rfc4314Rights.fromSerializedRfc4314Rights("aet")).asJava)

      Rights.fromACL(acl) must be(Rights.EMPTY)
    }
    "filter out negative users" in {
      val acl = new MailboxACL(Map(
        EntryKey.createUserEntryKey(USERNAME, NEGATIVE) -> Rfc4314Rights.fromSerializedRfc4314Rights("aet")).asJava)

      Rights.fromACL(acl) must be(Rights.EMPTY)
    }
    "accept users" in {
      val acl = new MailboxACL(Map(
        EntryKey.createUserEntryKey(USERNAME) -> Rfc4314Rights.fromSerializedRfc4314Rights("aet")).asJava)

      Rights.fromACL(acl) must be(Rights.builder
        .delegateTo(USERNAME, Seq(Right.Administer, Right.Expunge, Right.DeleteMessages))
        .build)
    }
    "filter out unknown rights" in {
      val acl = new MailboxACL(Map(
        EntryKey.createUserEntryKey(USERNAME) -> Rfc4314Rights.fromSerializedRfc4314Rights("aetpk")).asJava)

      Rights.fromACL(acl) must be(Rights.builder
        .delegateTo(USERNAME, Seq(Right.Administer, Right.Expunge, Right.DeleteMessages))
        .build)
    }
  }
  "To ACL" should  {
    "return empty when empty" in {
      Rights.EMPTY.toMailboxAcl must be(new MailboxACL())
    }
    "return acl conversion" in {
      val user1 = Username.of("user1")
      val user2 = Username.of("user2")
      val expected = new MailboxACL(Map(
          EntryKey.createUserEntryKey(user1) -> new Rfc4314Rights(JavaRight.Administer, JavaRight.DeleteMessages),
          EntryKey.createUserEntryKey(user2) -> new Rfc4314Rights(JavaRight.PerformExpunge, JavaRight.Lookup))
        .asJava)
      val jmapPojo = Rights.builder
        .delegateTo(user1, Seq(Right.Administer, Right.DeleteMessages))
        .delegateTo(user2, Seq(Right.Expunge, Right.Lookup))
        .build

      jmapPojo.toMailboxAcl must be(expected)
    }
  }
  "Remove entries" should  {
    "return empty when empty" in {
      Rights.EMPTY.removeEntriesFor(USERNAME) must be(Rights.EMPTY)
    }
    "return empty when only user" in {
      Rights.builder
        .delegateTo(USERNAME, Right.Lookup)
        .build
        .removeEntriesFor(USERNAME) must be(Rights.EMPTY)
    }
    "only remove specified users" in {
      val expected = Rights.builder.delegateTo(OTHER_USERNAME, Right.Lookup).build

      Rights.builder
        .delegateTo(USERNAME, Right.Lookup)
        .delegateTo(OTHER_USERNAME, Right.Lookup)
        .build
        .removeEntriesFor(USERNAME) must be(expected)
    }
  }
  "mayAddItems" should  {
    "return empty when no user" in {
      Rights.EMPTY.mayAddItems(USERNAME) must be(None)
    }
    "return false when no insert right" in {
      Rights.builder.delegateTo(USERNAME, Seq(Right.Administer, Right.Expunge, Right.Lookup, Right.DeleteMessages, Right.Read, Right.Seen, Right.Write))
        .build.mayAddItems(USERNAME) must be(Some(false))
    }
    "return true when insert right" in {
      Rights.builder.delegateTo(USERNAME, Right.Insert)
        .build.mayAddItems(USERNAME) must be(Some(true))
    }
  }
  "mayReadItems" should  {
    "return empty when no user" in {
      Rights.EMPTY.mayReadItems(USERNAME) must be(None)
    }
    "return false when no read right" in {
      Rights.builder.delegateTo(USERNAME, Seq(Right.Administer, Right.Expunge, Right.Lookup, Right.DeleteMessages, Right.Administer, Right.Seen, Right.Write))
        .build.mayReadItems(USERNAME) must be(Some(false))
    }
    "return true when read right" in {
      Rights.builder.delegateTo(USERNAME, Right.Read)
        .build.mayReadItems(USERNAME) must be(Some(true))
    }
  }
  "mayRemoveItems" should  {
    "return empty when no user" in {
      Rights.EMPTY.mayRemoveItems(USERNAME) must be(None)
    }
    "return false when no delete right" in {
      Rights.builder.delegateTo(USERNAME, Seq(Right.Administer, Right.Expunge, Right.Lookup, Right.Read, Right.Administer, Right.Seen, Right.Write))
        .build.mayRemoveItems(USERNAME) must be(Some(false))
    }
    "return true when delete right" in {
      Rights.builder.delegateTo(USERNAME, Right.DeleteMessages)
        .build.mayRemoveItems(USERNAME) must be(Some(true))
    }
  }
  "mayRename" should  {
    "return unsupported" in {
      Rights.EMPTY.mayRename(USERNAME) must be(Right.UNSUPPORTED)
    }
  }
  "mayDelete" should  {
    "return unsupported" in {
      Rights.EMPTY.mayDelete(USERNAME) must be(Right.UNSUPPORTED)
    }
  }
  "mayCreateChild" should  {
    "return unsupported" in {
      Rights.EMPTY.mayCreateChild(USERNAME) must be(Right.UNSUPPORTED)
    }
  }
}
