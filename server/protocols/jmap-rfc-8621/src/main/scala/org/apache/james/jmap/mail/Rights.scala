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

package org.apache.james.jmap.mail

import org.apache.james.core.Username
import org.apache.james.mailbox.model.MailboxACL
import org.apache.james.mailbox.model.MailboxACL.{EntryKey, Rfc4314Rights}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object Right {
  val UNSUPPORTED: Option[Boolean] = None

  val Administer = Right(MailboxACL.Right.Administer)
  val Expunge = Right(MailboxACL.Right.PerformExpunge)
  val Insert = Right(MailboxACL.Right.Insert)
  val Lookup = Right(MailboxACL.Right.Lookup)
  val Read = Right(MailboxACL.Right.Read)
  val Seen = Right(MailboxACL.Right.WriteSeenFlag)
  val DeleteMessages = Right(MailboxACL.Right.DeleteMessages)
  val Write = Right(MailboxACL.Right.Write)

  private val allRights = Seq(Administer, Expunge, Insert, Lookup, Read, Seen, DeleteMessages, Write)

  def forRight(right: MailboxACL.Right): Option[Right] = allRights.find(_.right.equals(right))

  def forChar(c: Char): Option[Right] = allRights.find(_.asCharacter == c)
}

sealed case class Right(right: MailboxACL.Right) {
  val asCharacter: Char = right.asCharacter

  val toMailboxRight: MailboxACL.Right = right
}

object Rights {
  private val LOGGER: Logger = LoggerFactory.getLogger(classOf[Rights])

  val EMPTY = new Rights(Map())

  def of(username: Username, right: Right): Rights = of(username, Seq(right))

  def of(username: Username, rights: Seq[Right]): Rights = {
    require(rights.nonEmpty, "'rights' should not be empty")

    Rights(Map(username -> rights))
  }

  def fromACL(acl: MailboxACL): Rights = acl.getEntries.asScala
    .filter {
      case (entryKey, _) => isSupported(entryKey)
    }
    .map {
      case (entryKey, aclRights) => toRights(entryKey, aclRights)
    }
    .fold(EMPTY)(_ combine _)

  private def toRights(entryKey: MailboxACL.EntryKey, aclRights: MailboxACL.Rfc4314Rights): Rights =
    of(Username.of(entryKey.getName), fromACL(aclRights))

  private def fromACL(rights: MailboxACL.Rfc4314Rights): Seq[Right] = rights.list.asScala
      .toSeq
      .flatMap(Right.forRight)

  private def isSupported(key: MailboxACL.EntryKey): Boolean = {
    if (key.isNegative) {
      LOGGER.info("Negative keys are not supported")
      return false
    }
    if (key == MailboxACL.OWNER_KEY) {
      return false
    }
    if (key.getNameType ne MailboxACL.NameType.user) {
      LOGGER.info("{} is not supported. Only 'user' is.", key.getNameType)
      return false
    }
    true
  }
}

case class Rights private(rights: Map[Username, Seq[Right]]) {

  def removeEntriesFor(username: Username) = Rights(rights.filter(!_._1.equals(username)))

  def toMailboxAcl: MailboxACL = {
    val map: Map[EntryKey, Rfc4314Rights] = rights.view
      .mapValues(seq => toJavaRights(seq))
      .toMap
      .map {
        case (user, rfc4314Rights) => (EntryKey.createUserEntryKey(user), rfc4314Rights)
      }
    new MailboxACL(map.asJava)
  }

  def append(username: Username, right: Right): Rights = append(username, Seq(right))

  def append(username: Username, rights: Seq[Right]): Rights = {
    require(rights.nonEmpty, "'rights' should not be empty")

    Rights(this.rights + (username -> rights))
  }

  def combine(that: Rights): Rights = Rights(this.rights ++ that.rights)

  private def toJavaRights(seq: Seq[Right]): Rfc4314Rights = Rfc4314Rights.of(seq.map(_.right).asJava)

  def mayReadItems(username: Username): Option[Boolean] = containsRight(username, Right.Read)

  def mayAddItems(username: Username): Option[Boolean] = containsRight(username, Right.Insert)

  def mayRemoveItems(username: Username): Option[Boolean] = containsRight(username, Right.DeleteMessages)

  def mayCreateChild(username: Username): Option[Boolean] = Right.UNSUPPORTED

  def mayRename(username: Username): Option[Boolean] = Right.UNSUPPORTED

  def mayDelete(username: Username): Option[Boolean] = Right.UNSUPPORTED

  private def containsRight(username: Username, right: Right): Option[Boolean] = rights.get(username)
    .map(_.contains(right))
}
