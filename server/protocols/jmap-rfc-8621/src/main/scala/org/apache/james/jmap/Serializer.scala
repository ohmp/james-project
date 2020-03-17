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

import org.apache.james.core.Username
import org.apache.james.jmap.mail.Quotas.Type
import org.apache.james.jmap.mail.{DelegatedNamespace, Mailbox, MailboxName, MailboxNamespace, MailboxRights, PersonalNamespace, Quota, QuotaId, Quotas, Rights, SortOrder, Value, Right => MailboxRight}
import org.apache.james.jmap.model.{Account, CapabilityIdentifier, CoreCapability, Id, MailCapability, Session, UnsignedInt}
import org.apache.james.mailbox.Role
import org.apache.james.mailbox.model.MailboxId
import play.api.libs.json.{JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, Json, Writes}

class Serializer {
  implicit val unsignedIntWrites: Writes[UnsignedInt] = size => JsNumber(size.value)
  implicit val usernameWrites: Writes[Username] = username => JsString(username.asString)
  implicit val idWrites: Writes[Id] = id => JsString(id.value)
  implicit val capabilityIdentifierWrites: Writes[CapabilityIdentifier] = identifier => JsString(identifier.value.toString)
  implicit val coreCapabilityWrites: Writes[CoreCapability] = Json.writes[CoreCapability]
  implicit val mailCapabilityWrites: Writes[MailCapability] = Json.writes[MailCapability]

  implicit val mailboxNameWrites: Writes[MailboxName] = name => JsString(name.name)
  implicit val sortOrderWrites: Writes[SortOrder] = sortOrder => JsNumber(sortOrder.sortOrder.value)
  implicit val roleWrites: Writes[Role] = role => JsString(role.serialize())
  implicit val quotaIdWrites: Writes[QuotaId] = quotaId => JsString(quotaId.quotaRoot.value)
  implicit val mailboxIdWrites: Writes[MailboxId] = mailboxId => JsString(mailboxId.serialize())
  implicit val quotaTypesWrites: Writes[Type] = `type` => JsString(`type`.asString)
  implicit val rightWrites: Writes[MailboxRight] = right => JsString(right.asCharacter.toString)

  implicit def identifierMapWrite[Any](implicit coreWriter: Writes[CoreCapability],
                                       mailWriter: Writes[MailCapability],
                                       idWriter: Writes[Id]): Writes[Map[CapabilityIdentifier, Any]] =
    (m: Map[CapabilityIdentifier, Any]) => {
      JsObject(
        m.map {
          case (identifier, capability: CoreCapability) => (identifier.value.toString, coreWriter.writes(capability))
          case (identifier, capability: MailCapability) => (identifier.value.toString, mailWriter.writes(capability))
          case (identifier, id: Id) => (identifier.value.toString, idWriter.writes(id))
          case _ => throw new RuntimeException("non supported serializer")
        }.toSeq
      )
    }

  implicit val accountWrites: Writes[Account] = Json.writes[Account]
  implicit val sessionWrites: Writes[Session] = Json.writes[Session]
  implicit val personalNamespaceWrites: Writes[PersonalNamespace] = value => JsObject(Map(
    "type" -> JsString(value.`type`),
    "owner" -> JsNull))
  implicit val delegatedNamespaceWrites: Writes[DelegatedNamespace] = value => JsObject(Map(
    "type" -> JsString(value.`type`),
    "owner" -> value.owner.map(_.asString()).map(JsString).getOrElse(JsNull)))
  implicit val namespaceWrites: Writes[MailboxNamespace] = Writes[MailboxNamespace] {
    case personal: PersonalNamespace => personalNamespaceWrites.writes(personal)
    case delegated: DelegatedNamespace => delegatedNamespaceWrites.writes(delegated)
  }
  implicit val mailboxRightsWrites: Writes[MailboxRights] = Json.writes[MailboxRights]
  implicit val quotaValueWrites: Writes[Value] = Json.writes[Value]

  implicit def idMapWrite[Any](implicit vr: Writes[Any]): Writes[Map[Id, Any]] =
    (m: Map[Id, Any]) => {
      JsObject(m.map { case (k, v) => (k.value, vr.writes(v)) }.toSeq)
    }

  implicit def quotaIdMapWrite[Any](implicit vr: Writes[Any]): Writes[Map[QuotaId, Any]] =
    (m: Map[QuotaId, Any]) => {
      JsObject(m.map { case (k, v) => (k.quotaRoot.value, vr.writes(v)) }.toSeq)
    }

  implicit def userMapWrite[Any](implicit vr: Writes[Any]): Writes[Map[Username, Any]] =
    (m: Map[Username, Any]) => {
      JsObject(m.map { case (k, v) => (k.asString(), vr.writes(v)) }.toSeq)
    }
  implicit val rightsWrites: Writes[Rights] = rights => {
    val map: Map[String, JsArray] = rights.rights
      .map {
        case (username, value) => (username.asString(), JsArray(value.map(_.asCharacter.toString).map(JsString)))
      }
    JsObject(map)
  }
  implicit val quotasWrites: Writes[Quotas] = quotas => {
    val map: Map[String, JsValue] = quotas.quotas
      .map {
        case (id, value) => (id.quotaRoot.value, quotaWrites.writes(value))
      }
    JsObject(map)
  }
  implicit val quotaWrites: Writes[Quota] = quota => {
    val map: Map[String, JsValue] = quota.quota
      .map {
        case (key, value) => (key.asString, quotaValueWrites.writes(value))
      }
    JsObject(map)
  }

  implicit val mailboxWrites: Writes[Mailbox] = Json.writes[Mailbox]
}
