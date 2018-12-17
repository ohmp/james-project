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

package org.apache.james.event.json

import java.time.Instant
import java.util.Optional

import javax.mail.{Flags => JavaMailFlags}
import julienrf.json.derived
import org.apache.james.core.quota.{QuotaCount, QuotaSize, QuotaValue}
import org.apache.james.core.{Domain, User}
import org.apache.james.event.json.MetaDataDTO.Flags
import org.apache.james.mailbox.MailboxListener.{Added => JavaAdded, MailboxAdded => JavaMailboxAdded, MailboxDeletion => JavaMailboxDeletion, MailboxRenamed => JavaMailboxRenamed, QuotaUsageUpdatedEvent => JavaQuotaUsageUpdatedEvent}
import org.apache.james.mailbox.MailboxSession.SessionId
import org.apache.james.mailbox.model.{MailboxId, MessageId, MessageMoves, QuotaRoot, MailboxPath => JavaMailboxPath, Quota => JavaQuota}
import org.apache.james.mailbox.{MessageUid, Event => JavaEvent, MessageMoveEvent => JavaMessageMoveEvent}
import play.api.libs.json.{JsArray, JsError, JsNull, JsNumber, JsObject, JsResult, JsString, JsSuccess, Json, OFormat, Reads, Writes}

import scala.collection.JavaConverters._

private sealed trait Event {
  def toJava: JavaEvent
}

private object DTO {

  object MailboxPath {
    def fromJava(javaMailboxPath: JavaMailboxPath): MailboxPath = DTO.MailboxPath(
      Option(javaMailboxPath.getNamespace),
      Option(javaMailboxPath.getUser),
      javaMailboxPath.getName)
  }

  case class MailboxPath(namespace: Option[String], user: Option[String], name: String) {
    def toJava: JavaMailboxPath = new JavaMailboxPath(namespace.orNull, user.orNull, name)
  }

  case class Quota[T <: QuotaValue[T]](used: T, limit: T, limits: Map[JavaQuota.Scope, T]) {
    def toJava: JavaQuota[T] =
      JavaQuota.builder[T]
        .used(used)
        .computedLimit(limit)
        .limitsByScope(limits.asJava)
        .build()
  }

  case class QuotaUsageUpdatedEvent(user: User, quotaRoot: QuotaRoot, countQuota: Quota[QuotaCount],
                                    sizeQuota: Quota[QuotaSize], time: Instant) extends Event {
    override def toJava: JavaEvent = new JavaQuotaUsageUpdatedEvent(user, quotaRoot, countQuota.toJava, sizeQuota.toJava, time)
  }

  case class MailboxAdded(mailboxPath: MailboxPath, mailboxId: MailboxId, user: User, sessionId: SessionId) extends Event {
    override def toJava: JavaEvent = new JavaMailboxAdded(sessionId, user, mailboxPath.toJava, mailboxId)
  }

  case class MailboxRenamed(sessionId: SessionId, user: User, path: MailboxPath, mailboxId: MailboxId, newPath: MailboxPath) extends Event {
    override def toJava: JavaEvent = new JavaMailboxRenamed(sessionId, user, path.toJava, mailboxId, newPath.toJava)
  }

  case class MailboxDeletion(sessionId: SessionId, user: User, path: MailboxPath, quotaRoot: QuotaRoot,
                             deletedMessageCount: QuotaCount, totalDeletedSize: QuotaSize, mailboxId: MailboxId) extends Event {
    override def toJava: JavaEvent = new JavaMailboxDeletion(sessionId, user, path.toJava, quotaRoot, deletedMessageCount,
      totalDeletedSize,
      mailboxId)
  }

  case class Added(sessionId: SessionId, user: User, path: MailboxPath, mailboxId: MailboxId,
                   added: Map[MessageUid, MetaDataDTO.MessageMetaData]) extends Event {
    override def toJava: JavaEvent = new JavaAdded(
      sessionId,
      user,
      path.toJava,
      mailboxId,
      added.map(entry => entry._1 -> entry._2.toJava).asJava)
  }

  case class MessageMoveEvent(user: User, previousMailboxIds: List[MailboxId], targetMailboxIds: List[MailboxId],
                              messageIds: List[MessageId]) extends Event {
    override def toJava: JavaEvent = JavaMessageMoveEvent.builder()
      .user(user)
      .messageId(messageIds.asJava)
      .messageMoves(MessageMoves.builder()
          .previousMailboxIds(previousMailboxIds.asJava)
          .targetMailboxIds(targetMailboxIds.asJava)
        .build())
      .build()
  }
}

private object ScalaConverter {
  private def toScala[T <: QuotaValue[T]](java: JavaQuota[T]): DTO.Quota[T] = DTO.Quota(
    used = java.getUsed,
    limit = java.getLimit,
    limits = java.getLimitByScope.asScala.toMap)

  private def toScala(event: JavaQuotaUsageUpdatedEvent): DTO.QuotaUsageUpdatedEvent = DTO.QuotaUsageUpdatedEvent(
    user = event.getUser,
    quotaRoot = event.getQuotaRoot,
    countQuota = toScala(event.getCountQuota),
    sizeQuota = toScala(event.getSizeQuota),
    time = event.getInstant)

  private def toScala(event: JavaMailboxAdded): DTO.MailboxAdded = DTO.MailboxAdded(
    mailboxPath = DTO.MailboxPath.fromJava(event.getMailboxPath),
    mailboxId = event.getMailboxId,
    user = event.getUser,
    sessionId = event.getSessionId)

  private def toScala(event: JavaMailboxRenamed): DTO.MailboxRenamed = DTO.MailboxRenamed(
    sessionId = event.getSessionId,
    user = event.getUser,
    path = DTO.MailboxPath.fromJava(event.getMailboxPath),
    mailboxId = event.getMailboxId,
    newPath = DTO.MailboxPath.fromJava(event.getNewPath))

  private def toScala(event: JavaMailboxDeletion): DTO.MailboxDeletion = DTO.MailboxDeletion(
    sessionId = event.getSessionId,
    user = event.getUser,
    quotaRoot = event.getQuotaRoot,
    path = DTO.MailboxPath.fromJava(event.getMailboxPath),
    deletedMessageCount = event.getDeletedMessageCount,
    totalDeletedSize = event.getTotalDeletedSize,
    mailboxId = event.getMailboxId)

  private def toScala(event: JavaAdded): DTO.Added = DTO.Added(
    sessionId = event.getSessionId,
    user = event.getUser,
    path = DTO.MailboxPath.fromJava(event.getMailboxPath),
    mailboxId = event.getMailboxId,
    added = event.getAdded.asScala.map(entry => entry._1 -> MetaDataDTO.MessageMetaData.fromJava(entry._2)).toMap)

  private def toScala(event: JavaMessageMoveEvent): DTO.MessageMoveEvent = DTO.MessageMoveEvent(
    user = event.getUser,
    previousMailboxIds = event.getMessageMoves.getPreviousMailboxIds.asScala.toList,
    targetMailboxIds = event.getMessageMoves.getTargetMailboxIds.asScala.toList,
    messageIds = event.getMessageIds.asScala.toList)

  def toScala(javaEvent: JavaEvent): Event = javaEvent match {
    case e: JavaQuotaUsageUpdatedEvent => toScala(e)
    case e: JavaMailboxAdded => toScala(e)
    case e: JavaMailboxRenamed => toScala(e)
    case e: JavaMailboxDeletion => toScala(e)
    case e: JavaAdded => toScala(e)
    case e: JavaMessageMoveEvent => toScala(e)
    case _ => throw new RuntimeException("no Scala conversion known")
  }
}

private class JsonSerialize(mailboxIdFactory: MailboxId.Factory, messageIdFactory: MessageId.Factory) {
  implicit val userWriters: Writes[User] = (user: User) => JsString(user.asString)
  implicit val quotaRootWrites: Writes[QuotaRoot] = quotaRoot => JsString(quotaRoot.getValue)
  implicit val quotaValueWrites: Writes[QuotaValue[_]] = value => if (value.isUnlimited) JsNull else JsNumber(value.asLong())
  implicit val quotaScopeWrites: Writes[JavaQuota.Scope] = value => JsString(value.name)
  implicit val quotaCountWrites: Writes[DTO.Quota[QuotaCount]] = Json.writes[DTO.Quota[QuotaCount]]
  implicit val quotaSizeWrites: Writes[DTO.Quota[QuotaSize]] = Json.writes[DTO.Quota[QuotaSize]]
  implicit val mailboxPathWrites: Writes[DTO.MailboxPath] = Json.writes[DTO.MailboxPath]
  implicit val mailboxIdWrites: Writes[MailboxId] = value => JsString(value.serialize())
  implicit val sessionIdWrites: Writes[SessionId] = value => JsNumber(value.getValue)
  implicit val messageIdWrites: Writes[MessageId] = value => JsString(value.serialize())
  implicit val messageUidWrites: Writes[MessageUid] = value => JsNumber(value.asLong())
  implicit val flagsWrites: Writes[JavaMailFlags] = value => JsArray(Flags.fromJavaFlags(value).map(flag => JsString(flag)))
  implicit val messageMetaDataWrites: Writes[MetaDataDTO.MessageMetaData] = Json.writes[MetaDataDTO.MessageMetaData]

  implicit val userReads: Reads[User] = {
    case JsString(userAsString) => JsSuccess(User.fromUsername(userAsString))
    case _ => JsError()
  }
  implicit val mailboxIdReads: Reads[MailboxId] = {
    case JsString(serializedMailboxId) => JsSuccess(mailboxIdFactory.fromString(serializedMailboxId))
    case _ => JsError()
  }
  implicit val sessionIdReads: Reads[SessionId] = {
    case JsNumber(id) => JsSuccess(SessionId.of(id.longValue()))
    case _ => JsError()
  }
  implicit val quotaRootReads: Reads[QuotaRoot] = {
    case JsString(quotaRoot) => JsSuccess(QuotaRoot.quotaRoot(quotaRoot, Optional.empty[Domain]))
    case _ => JsError()
  }
  implicit val quotaCountReads: Reads[QuotaCount] = {
    case JsNumber(count) => JsSuccess(QuotaCount.count(count.toLong))
    case JsNull => JsSuccess(QuotaCount.unlimited())
    case _ => JsError()
  }
  implicit val quotaSizeReads: Reads[QuotaSize] = {
    case JsNumber(size) => JsSuccess(QuotaSize.size(size.toLong))
    case JsNull => JsSuccess(QuotaSize.unlimited())
    case _ => JsError()
  }
  implicit val quotaScopeReads: Reads[JavaQuota.Scope] = {
    case JsString(value) => JsSuccess(JavaQuota.Scope.valueOf(value))
    case _ => JsError()
  }
  implicit val messageIdReads: Reads[MessageId] = {
    case JsString(value) => JsSuccess(messageIdFactory.fromString(value))
    case _ => JsError()
  }
  implicit val messageUidReads: Reads[MessageUid] = {
    case JsNumber(value) => JsSuccess(MessageUid.of(value.toLong))
    case _ => JsError()
  }
  implicit val flagsReads: Reads[JavaMailFlags] = {
    case JsArray(seqOfJsValues) => JsSuccess(Flags.toJavaFlags(seqOfJsValues.toArray.map(jsValue => jsValue.toString())))
    case _ => JsError()
  }

  implicit def scopeMapReads[V](implicit vr: Reads[V]): Reads[Map[JavaQuota.Scope, V]] =
    Reads.mapReads[JavaQuota.Scope, V] { str =>
      Json.fromJson[JavaQuota.Scope](JsString(str))
    }

  implicit def scopeMapWrite[V](implicit vr: Writes[V]): Writes[Map[JavaQuota.Scope, V]] =
    (m: Map[JavaQuota.Scope, V]) => {
      JsObject(m.map { case (k, v) => (k.toString, vr.writes(v)) }.toSeq)
    }

  implicit def scopeMessageUidMapReads[V](implicit vr: Reads[V]): Reads[Map[MessageUid, V]] =
    Reads.mapReads[MessageUid, V] { str =>
      JsSuccess(MessageUid.of(str.toLong))
    }

  implicit def scopeMessageUidMapWrite[V](implicit vr: Writes[V]): Writes[Map[MessageUid, V]] =
    (m: Map[MessageUid, V]) => {
      JsObject(m.map { case (k, v) => (String.valueOf(k.asLong()), vr.writes(v)) }.toSeq)
    }

  implicit val quotaCReads: Reads[DTO.Quota[QuotaCount]] = Json.reads[DTO.Quota[QuotaCount]]
  implicit val quotaSReads: Reads[DTO.Quota[QuotaSize]] = Json.reads[DTO.Quota[QuotaSize]]
  implicit val mailboxPathReads: Reads[DTO.MailboxPath] = Json.reads[DTO.MailboxPath]
  implicit val messageMetaDataReads: Reads[MetaDataDTO.MessageMetaData] = Json.reads[MetaDataDTO.MessageMetaData]

  implicit val eventOFormat: OFormat[Event] = derived.oformat()

  def toJson(event: Event): String = Json.toJson(event).toString()

  def fromJson(json: String): JsResult[Event] = Json.fromJson[Event](Json.parse(json))
}

class EventSerializer(mailboxIdFactory: MailboxId.Factory, messageIdFactory: MessageId.Factory) {
  def toJson(event: JavaEvent): String = new JsonSerialize(mailboxIdFactory, messageIdFactory).toJson(ScalaConverter.toScala(event))

  def fromJson(json: String): JsResult[JavaEvent] = {
    new JsonSerialize(mailboxIdFactory, messageIdFactory)
      .fromJson(json)
      .map(event => event.toJava)
  }
}
