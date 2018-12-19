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
import java.util.Date

import javax.mail.{Flags => JavaMailFlags}
import org.apache.james.core.quota.QuotaValue
import org.apache.james.mailbox.acl.{ACLDiff => JavaACLDiff}
import org.apache.james.mailbox.model.{MailboxACL, MessageId, MailboxPath => JavaMailboxPath, MessageMetaData => JavaMessageMetaData, Quota => JavaQuota, UpdatedFlags => JavaUpdatedFlags}
import org.apache.james.mailbox.{FlagsBuilder, MessageUid}

import scala.collection.JavaConverters._

object DTOs {

  object ACLDiff {
    def fromJava(javaACLDiff: JavaACLDiff): ACLDiff = ACLDiff(
      javaACLDiff.getOldACL.getEntries.asScala.toMap,
      javaACLDiff.getNewACL.getEntries.asScala.toMap)
  }

  object MailboxPath {
    def fromJava(javaMailboxPath: JavaMailboxPath): MailboxPath = MailboxPath(
      Option(javaMailboxPath.getNamespace),
      Option(javaMailboxPath.getUser),
      javaMailboxPath.getName)
  }

  object Quota {
    def toScala[T <: QuotaValue[T]](java: JavaQuota[T]): Quota[T] = Quota(
      used = java.getUsed,
      limit = java.getLimit,
      limits = java.getLimitByScope.asScala.toMap)
  }

  case class ACLDiff(oldACL: Map[MailboxACL.EntryKey, MailboxACL.Rfc4314Rights],
                     newACL: Map[MailboxACL.EntryKey, MailboxACL.Rfc4314Rights]) {
    def toJava: JavaACLDiff = new JavaACLDiff(new MailboxACL(oldACL.asJava), new MailboxACL(newACL.asJava))
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

  object MessageMetaData {
    def fromJava(javaMessageMetaData: JavaMessageMetaData): MessageMetaData = DTOs.MessageMetaData(
      javaMessageMetaData.getUid,
      javaMessageMetaData.getModSeq,
      Flags.toScala(javaMessageMetaData.getFlags),
      javaMessageMetaData.getSize,
      javaMessageMetaData.getInternalDate.toInstant,
      javaMessageMetaData.getMessageId)
  }

  case class MessageMetaData(uid: MessageUid, modSeq: Long, flags: Flags, size: Long, internalDate: Instant, messageId: MessageId) {
    def toJava: JavaMessageMetaData = new JavaMessageMetaData(uid, modSeq, flags.toJava, size, Date.from(internalDate), messageId)
  }

  object SystemFlag {
    def addToBuilder(flagsBuilder: FlagsBuilder, systemFlag: SystemFlag): FlagsBuilder = systemFlag match {
      case Answered => flagsBuilder.add(JavaMailFlags.Flag.ANSWERED)
      case Deleted => flagsBuilder.add(JavaMailFlags.Flag.DELETED)
      case Draft => flagsBuilder.add(JavaMailFlags.Flag.DRAFT)
      case Flagged => flagsBuilder.add(JavaMailFlags.Flag.FLAGGED)
      case Recent => flagsBuilder.add(JavaMailFlags.Flag.RECENT)
      case Seen => flagsBuilder.add(JavaMailFlags.Flag.SEEN)
    }

    def toScala(flag: JavaMailFlags.Flag): SystemFlag = flag match {
      case JavaMailFlags.Flag.ANSWERED => Answered
      case JavaMailFlags.Flag.DELETED => Deleted
      case JavaMailFlags.Flag.DRAFT => Draft
      case JavaMailFlags.Flag.FLAGGED => Flagged
      case JavaMailFlags.Flag.RECENT => Recent
      case JavaMailFlags.Flag.SEEN => Seen
    }

    val ANSWERED = "Answered"
    val DELETED = "Deleted"
    val DRAFT = "Draft"
    val FLAGGED = "Flagged"
    val RECENT = "Recent"
    val SEEN = "Seen"

    def fromString(value: String): SystemFlag = value match {
      case ANSWERED => Answered
      case DELETED => Deleted
      case DRAFT => Draft
      case FLAGGED => Flagged
      case RECENT => Recent
      case SEEN => Seen
      case unknown => throw new RuntimeException("Unknown system flag " + unknown)
    }
  }

  sealed trait SystemFlag {
    def rawString: String
  }
  case object Answered extends SystemFlag {
    override def rawString: String = SystemFlag.ANSWERED
  }
  case object Deleted extends SystemFlag {
    override def rawString: String = SystemFlag.DELETED
  }
  case object Draft extends SystemFlag {
    override def rawString: String = SystemFlag.DRAFT
  }
  case object Flagged extends SystemFlag {
    override def rawString: String = SystemFlag.FLAGGED
  }
  case object Recent extends SystemFlag {
    override def rawString: String = SystemFlag.RECENT
  }
  case object Seen extends SystemFlag {
    override def rawString: String = SystemFlag.SEEN
  }

  object Flags {
    def toScala(flags: JavaMailFlags): Flags = Flags(
        Option(flags.getSystemFlags).getOrElse(Array()).map(SystemFlag.toScala).toList,
        Option(flags.getUserFlags).getOrElse(Array()).toList)
  }

  case class Flags(systemFlags: List[SystemFlag], userFlags: List[String]) {
    def toJava: JavaMailFlags = systemFlags.foldLeft(new FlagsBuilder())(SystemFlag.addToBuilder)
      .addUserFlags(userFlags.asJava)
      .build()
  }

  object UpdatedFlags {
    def toUpdatedFlags(javaUpdatedFlags: JavaUpdatedFlags): UpdatedFlags = UpdatedFlags(
      javaUpdatedFlags.getUid,
      javaUpdatedFlags.getModSeq,
      Flags.toScala(javaUpdatedFlags.getOldFlags),
      Flags.toScala(javaUpdatedFlags.getNewFlags))
  }

  case class UpdatedFlags(uid: MessageUid, modSeq: Long, oldFlags: Flags, newFlags: Flags) {
    def toJava: JavaUpdatedFlags = JavaUpdatedFlags.builder()
      .uid(uid)
      .modSeq(modSeq)
      .oldFlags(oldFlags.toJava)
      .newFlags(newFlags.toJava)
      .build()
  }
}


