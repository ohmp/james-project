/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/

package org.apache.james.jmap

import java.net.URI

import org.apache.james.core.{Domain, Username}
import org.apache.james.jmap.SerializerTest.{MAILBOX_RIGHTS, QUOTAS, RIGHTS, SESSION, SESSION_JSON, USER_1}
import org.apache.james.jmap.mail.{Mailbox, MailboxName, MailboxNamespace, MailboxRights, Quota, QuotaId, QuotaRoot, Quotas, Right, Rights, SortOrder, Value}
import org.apache.james.jmap.model.{Account, CapabilityIdentifier, CoreCapability, Id, MailCapability, Session, UnsignedInt}
import org.apache.james.mailbox.Role
import org.apache.james.mailbox.model.TestId
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue, Json}

import scala.io.Source

object SerializerTest {
  private val ALGO_1 = "i;ascii-numeric"
  private val ALGO_2 = "i;ascii-casemap"
  private val ALGO_3 = "i;unicode-casemap"
  private val MAX_SIZE_UPLOAD = UnsignedInt(50000000)
  private val MAX_CONCURRENT_UPLOAD = UnsignedInt(8)
  private val MAX_SIZE_REQUEST = UnsignedInt(10000000)
  private val MAX_CONCURRENT_REQUESTS = UnsignedInt(10000000)
  private val MAX_CALLS_IN_REQUEST = UnsignedInt(32)
  private val MAX_OBJECTS_IN_GET = UnsignedInt(256)
  private val MAX_OBJECTS_IN_SET = UnsignedInt(128)
  private val USER_1 = Username.of("user1@james.org")
  private val USER_1_ID = Id("user1Id")
  private val USER_2 = Username.of("user2@james.org")
  private val USER_2_ID = Id("user2Id")
  private val URL = "http://james.org"
  private val STATE = "fda9342jcm"

  private val CORE_IDENTIFIER = CapabilityIdentifier(new URI("urn:ietf:params:jmap:core"))
  private val MAIL_IDENTIFIER = CapabilityIdentifier(new URI("urn:ietf:params:jmap:mail"))
  private val CONTACT_IDENTIFIER = CapabilityIdentifier(new URI("urn:ietf:params:jmap:contact"))

  private val CORE_CAPABILITY = CoreCapability(
    maxSizeUpload = MAX_SIZE_UPLOAD, maxConcurrentUpload = MAX_CONCURRENT_UPLOAD,
    maxSizeRequest = MAX_SIZE_REQUEST, maxConcurrentRequests = MAX_CONCURRENT_REQUESTS,
    maxCallsInRequest = MAX_CALLS_IN_REQUEST, maxObjectsInGet = MAX_OBJECTS_IN_GET, maxObjectsInSet = MAX_OBJECTS_IN_SET,
    collationAlgorithms = List(ALGO_1, ALGO_2, ALGO_3))
  private val MAX_MAILBOX_DEPTH = Some(UnsignedInt(1432))
  private val MAX_MAILBOXES_PER_EMAIL = Some(UnsignedInt(9359))
  private val MAX_SIZE_MAILBOX_NAME = UnsignedInt(9000)
  private val MAX_SIZE_ATTACHMENTS_PER_EMAIL = UnsignedInt(890099)

  private val MAIL_CAPABILITY = MailCapability(
    maxMailboxDepth = MAX_MAILBOX_DEPTH,
    maxMailboxesPerEmail = MAX_MAILBOXES_PER_EMAIL,
    maxSizeMailboxName = MAX_SIZE_MAILBOX_NAME,
    maxSizeAttachmentsPerEmail = MAX_SIZE_ATTACHMENTS_PER_EMAIL,
    emailQuerySortOptions = List(),
    mayCreateTopLevelMailbox = true)

  private val CAPABILITIES = Map(
    CORE_IDENTIFIER -> CORE_CAPABILITY,
    MAIL_IDENTIFIER -> MAIL_CAPABILITY
  )

  private val ACCOUNT_1 = Account(
    name = USER_1,
    isPersonal = true,
    isReadOnly = false,
    accountCapabilities = Map(CORE_IDENTIFIER -> CORE_CAPABILITY))
  private val ACCOUNT_2 = Account(
    name = USER_2,
    isPersonal = false,
    isReadOnly = false,
    accountCapabilities = Map(CORE_IDENTIFIER -> CORE_CAPABILITY))
  private val ACCOUNTS = Map(
    USER_1_ID -> ACCOUNT_1,
    USER_2_ID -> ACCOUNT_2,
  )
  private val PRIMARY_ACCOUNTS = Map(
    MAIL_IDENTIFIER -> USER_1_ID,
    CONTACT_IDENTIFIER -> USER_2_ID
  )

  private val SESSION = Session(
    capabilities = CAPABILITIES,
    accounts = ACCOUNTS,
    primaryAccounts = PRIMARY_ACCOUNTS,
    username = USER_1,
    apiUrl = URL,
    downloadUrl = URL,
    uploadUrl = URL,
    eventSourceUrl = URL,
    state = STATE)

  private val SESSION_JSON =
    s"""{
      |  "capabilities": {
      |    "${CORE_IDENTIFIER.asString()}": {
      |      "maxSizeUpload": ${MAX_SIZE_UPLOAD.value},
      |      "maxConcurrentUpload": ${MAX_CONCURRENT_UPLOAD.value},
      |      "maxSizeRequest": ${MAX_SIZE_REQUEST.value},
      |      "maxConcurrentRequests": ${MAX_CONCURRENT_REQUESTS.value},
      |      "maxCallsInRequest": ${MAX_CALLS_IN_REQUEST.value},
      |      "maxObjectsInGet": ${MAX_OBJECTS_IN_GET.value},
      |      "maxObjectsInSet": ${MAX_OBJECTS_IN_SET.value},
      |      "collationAlgorithms": [
      |        "$ALGO_1",
      |        "$ALGO_2",
      |        "$ALGO_3"
      |      ]
      |    },
      |    "${MAIL_IDENTIFIER.asString()}": {
      |      "maxMailboxesPerEmail": ${MAX_MAILBOXES_PER_EMAIL.get.value},
      |      "maxMailboxDepth": ${MAX_MAILBOX_DEPTH.get.value},
      |      "maxSizeMailboxName": ${MAX_SIZE_MAILBOX_NAME.value},
      |      "maxSizeAttachmentsPerEmail": ${MAX_SIZE_ATTACHMENTS_PER_EMAIL.value},
      |      "emailQuerySortOptions": [],
      |      "mayCreateTopLevelMailbox": true
      |    }
      |  },
      |  "accounts": {
      |    "${USER_1_ID.value}": {
      |      "name": "${USER_1.asString}",
      |      "isPersonal": true,
      |      "isReadOnly": false,
      |      "accountCapabilities": {
      |        "${CORE_IDENTIFIER.asString()}": {
      |          "maxSizeUpload": ${MAX_SIZE_UPLOAD.value},
      |          "maxConcurrentUpload": ${MAX_CONCURRENT_UPLOAD.value},
      |          "maxSizeRequest": ${MAX_SIZE_REQUEST.value},
      |          "maxConcurrentRequests": ${MAX_CONCURRENT_REQUESTS.value},
      |          "maxCallsInRequest": ${MAX_CALLS_IN_REQUEST.value},
      |          "maxObjectsInGet": ${MAX_OBJECTS_IN_GET.value},
      |          "maxObjectsInSet": ${MAX_OBJECTS_IN_SET.value},
      |          "collationAlgorithms": [
      |            "$ALGO_1",
      |            "$ALGO_2",
      |            "$ALGO_3"
      |          ]
      |        }
      |      }
      |    },
      |    "${USER_2_ID.value}": {
      |      "name": "${USER_2.asString}",
      |      "isPersonal": false,
      |      "isReadOnly": false,
      |      "accountCapabilities": {
      |        "${CORE_IDENTIFIER.asString()}": {
      |          "maxSizeUpload": ${MAX_SIZE_UPLOAD.value},
      |          "maxConcurrentUpload": ${MAX_CONCURRENT_UPLOAD.value},
      |          "maxSizeRequest": ${MAX_SIZE_REQUEST.value},
      |          "maxConcurrentRequests": ${MAX_CONCURRENT_REQUESTS.value},
      |          "maxCallsInRequest": ${MAX_CALLS_IN_REQUEST.value},
      |          "maxObjectsInGet": ${MAX_OBJECTS_IN_GET.value},
      |          "maxObjectsInSet": ${MAX_OBJECTS_IN_SET.value},
      |          "collationAlgorithms": [
      |            "$ALGO_1",
      |            "$ALGO_2",
      |            "$ALGO_3"
      |          ]
      |        }
      |      }
      |    }
      |  },
      |  "primaryAccounts": {
      |    "${MAIL_IDENTIFIER.asString()}": "${USER_1_ID.value}",
      |    "${CONTACT_IDENTIFIER.asString()}": "${USER_2_ID.value}"
      |  },
      |  "username": "${USER_1.asString}",
      |  "apiUrl": "$URL",
      |  "downloadUrl": "$URL",
      |  "uploadUrl": "$URL",
      |  "eventSourceUrl": "$URL",
      |  "state": "$STATE"
      |}""".stripMargin

  private val QUOTAS = Quotas(Map(
    QuotaId(QuotaRoot("quotaRoot", None)) -> Quota(Map(
      Quotas.Message() -> Value(18L, Some(42L)),
      Quotas.Storage() -> Value(12L, None))),
    QuotaId(QuotaRoot("quotaRoot2@localhost", Some(Domain.LOCALHOST))) -> Quota(Map(
      Quotas.Message() -> Value(14L, Some(43L)),
      Quotas.Storage() -> Value(19L, None)))))
  private val MAILBOX_RIGHTS = MailboxRights(mayReadItems = true,
    mayAddItems = true,
    mayRemoveItems = true,
    maySetSeen = true,
    maySetKeywords = true,
    mayCreateChild = true,
    mayRename = true,
    mayDelete = true,
    maySubmit = true)
  private val RIGHTS: Rights = Rights.of(USER_1, Seq(Right.Expunge, Right.Lookup))
    .append(USER_2, Seq(Right.Read, Right.Write))

  def readResource(resourceFileName: String): Source = {
    Source.fromURL(getClass.getResource(resourceFileName), "UTF-8")
  }
}

class SerializerTest extends PlaySpec {

  "sessionWrites" should {
    "serialize session" in {
      new Serializer().sessionWrites.writes(SESSION) must equal(Json.parse(SESSION_JSON))
    }
  }

  "mailboxIdWrites" should {
    "serialize mailboxId" in {
      new Serializer().mailboxIdWrites.writes(TestId.of(45)) must be(JsString("45"))
    }
  }
  "mailboxNameWrites" should {
    "serialize mailboxName" in {
      new Serializer().mailboxNameWrites.writes(MailboxName("INBOX")) must be(JsString("INBOX"))
    }
  }
  "roleWrites" should {
    "serialize role" in {
      new Serializer().roleWrites.writes(Role.INBOX) must be(JsString("inbox"))
    }
  }
  "sortOrderWrites" should {
    "serialize sortOrder" in {
      new Serializer().sortOrderWrites.writes(SortOrder(3L)) must be(JsNumber(3L))
    }
  }
  "rightsWrites" should {
    "serialize empty rights" in {
      new Serializer().rightsWrites.writes(Rights.EMPTY) must be(JsObject(Map[String, JsValue]()))
    }
  }
  "namespaceWrites" should {
    "serialize personal" in {
      new Serializer().namespaceWrites.writes(MailboxNamespace.personal()) must equal(Json.parse(
        """{"type":"Personal","owner":null}"""))
    }
    "serialize delegated" in {
      new Serializer().namespaceWrites.writes(MailboxNamespace.delegated(USER_1)) must equal(Json.parse(
        """{"type":"Delegated","owner":"user1@james.org"}"""))
    }
  }
  "rightsWrites" should {
    "serialize rights" in {
      new Serializer().rightsWrites.writes(RIGHTS) must equal(Json.parse(
        """{
          |  "user1@james.org": ["e", "l"],
          |  "user2@james.org": ["r", "w"]
          |}""".stripMargin))
    }
  }
  "mailboxRightsWrites" should {
    "serialize rights" in {
      new Serializer().mailboxRightsWrites.writes(MAILBOX_RIGHTS) must be(Json.parse(
        """{
          |  "mayReadItems":true,
          |  "mayAddItems":true,
          |  "mayRemoveItems":true,
          |  "maySetSeen":true,
          |  "maySetKeywords":true,
          |  "mayCreateChild":true,
          |  "mayRename":true,
          |  "mayDelete":true,
          |  "maySubmit":true
          |}""".stripMargin))
    }
  }
  "quotasWrites" should {
    "serialize quotas" in {
      new Serializer().quotasWrites.writes(QUOTAS) must be(Json.parse(
        """{
          |  "quotaRoot":{
          |    "Message":{"used":18,"max":42},
          |    "Storage":{"used":12}},
          |  "quotaRoot2@localhost":{
          |    "Message":{"used":14,"max":43},
          |    "Storage":{"used":19}}
          |}""".stripMargin))
    }
  }

  "mailboxWrites" should {
    "serialize mailbox" in {
      val mailbox = Mailbox(
        id = TestId.of(42L),
        mailboxName = MailboxName("Inbox"),
        parentId = None,
        role = Some(Role.INBOX),
        sortOrder = SortOrder.apply(7L),
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
        namespace = MailboxNamespace.personal(),
        rights = RIGHTS,
        quotas = QUOTAS)

      new Serializer().mailboxWrites.writes(mailbox) must be(Json.parse(
        """{
          |  "id":"42",
          |  "mailboxName":"Inbox",
          |  "role":"inbox",
          |  "sortOrder":7,
          |  "totalEmails":3,
          |  "unreadEmails":4,
          |  "totalThreads":5,
          |  "unreadThreads":6,
          |  "mailboxRights":{
          |    "mayReadItems":true,
          |    "mayAddItems":true,
          |    "mayRemoveItems":true,
          |    "maySetSeen":true,"maySetKeywords":true,
          |    "mayCreateChild":true,"mayRename":true,
          |    "mayDelete":true,
          |    "maySubmit":true
          |  },
          |  "isSubscribed":true,
          |  "namespace":{"type":"Personal","owner":null},
          |  "rights":{
          |    "user1@james.org":["e","l"],
          |    "user2@james.org":["r","w"]
          |  },
          |  "quotas":{
          |    "quotaRoot":{
          |      "Message":{"used":18,"max":42},
          |      "Storage":{"used":12}
          |    },
          |    "quotaRoot2@localhost":{
          |      "Message":{"used":14,"max":43},
          |      "Storage":{"used":19}
          |    }
          |  }
          |}""".stripMargin))
    }
  }
}
