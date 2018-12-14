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
import org.apache.james.mailbox.MessageUid
import org.apache.james.mailbox.model.{MessageId, MessageMetaData => JavaMessageMetaData}
import org.apache.james.mailbox.store.{SimpleMessageMetaData => JavaSimpleMessageMetaData}

object MetaDataDTO {

  object MessageMetaData {
    def fromJava(javaMessageMetaData: JavaMessageMetaData): MessageMetaData = MetaDataDTO.MessageMetaData(
      javaMessageMetaData.getUid,
      javaMessageMetaData.getModSeq,
      javaMessageMetaData.getFlags,
      javaMessageMetaData.getSize,
      javaMessageMetaData.getInternalDate.toInstant,
      javaMessageMetaData.getMessageId)
  }

  object Flags {
    val ANSWERED = "answered"
    val DELETED = "deleted"
    val DRAFT = "draft"
    val FLAGGED = "flagged"
    val RECENT = "recent"
    val SEEN = "seen"
    val USER = "user"
    val ALL_SYSTEM_FLAGS = List(ANSWERED, DELETED, DRAFT, FLAGGED, RECENT, SEEN, USER)

    def toJavaFlags(serializedFlags: Array[String]): JavaMailFlags = {
      val javaMailFlags = new JavaMailFlags()

      serializedFlags
        .filter(flag => ALL_SYSTEM_FLAGS.contains(flag.toLowerCase()))
        .foreach(serializedSystemFlag => javaMailFlags.add(stringToSystemFlag(serializedSystemFlag)))

      serializedFlags
        .filterNot(flag => ALL_SYSTEM_FLAGS.contains(flag.toLowerCase()))
        .foreach(userFlag => javaMailFlags.add(userFlag))

      javaMailFlags
    }

    def fromJavaFlags(flags: JavaMailFlags): Array[String] = {
      flags.getUserFlags ++ flags.getSystemFlags.map(flag => systemFlagToString(flag))
    }

    private def stringToSystemFlag(serializedFlag: String): JavaMailFlags.Flag = serializedFlag.toLowerCase match {
      case ANSWERED => JavaMailFlags.Flag.ANSWERED
      case DELETED => JavaMailFlags.Flag.DELETED
      case DRAFT => JavaMailFlags.Flag.DRAFT
      case FLAGGED => JavaMailFlags.Flag.FLAGGED
      case RECENT => JavaMailFlags.Flag.RECENT
      case SEEN => JavaMailFlags.Flag.SEEN
      case USER => JavaMailFlags.Flag.USER
      case _ => null
    }

    private def systemFlagToString(flag: JavaMailFlags.Flag): String = flag match {
      case JavaMailFlags.Flag.ANSWERED => ANSWERED
      case JavaMailFlags.Flag.DELETED => DELETED
      case JavaMailFlags.Flag.DRAFT => DRAFT
      case JavaMailFlags.Flag.FLAGGED => FLAGGED
      case JavaMailFlags.Flag.RECENT => RECENT
      case JavaMailFlags.Flag.SEEN => SEEN
      case JavaMailFlags.Flag.USER => USER
    }
  }

  case class MessageMetaData(uid: MessageUid, modSeq: Long, flags: JavaMailFlags, size: Long, internalDate: Instant, messageId: MessageId) {
    def toJava: JavaMessageMetaData = new JavaSimpleMessageMetaData(uid, modSeq, flags, size, Date.from(internalDate), messageId)
  }
}
