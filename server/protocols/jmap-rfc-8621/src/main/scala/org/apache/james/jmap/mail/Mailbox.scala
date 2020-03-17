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
import org.apache.james.jmap.model.UnsignedInt
import org.apache.james.mailbox.Role
import org.apache.james.mailbox.model.MailboxId

final case class MailboxName(name: String) {
  require(name != null, "'name' is mandatory")
  require(!name.isEmpty, "'name' is mandatory")
}

final case class MailboxRights(mayReadItems: Boolean,
                               mayAddItems: Boolean,
                               mayRemoveItems: Boolean,
                               maySetSeen: Boolean,
                               maySetKeywords: Boolean,
                               mayCreateChild: Boolean,
                               mayRename: Boolean,
                               mayDelete: Boolean,
                               maySubmit: Boolean)

object MailboxNamespace {
  sealed case class Type(value: String)

  val Delegated = Type("Delegated")
  val Personal = Type("Personal")

  def delegated(owner: Username): MailboxNamespace = {
    require(owner != null)
    require(!owner.asString.trim.isEmpty)

    new MailboxNamespace(Delegated, Some(owner))
  }

  def personal = new MailboxNamespace(Personal, None)
}

case class MailboxNamespace private(`type`: MailboxNamespace.Type, owner: Option[Username])

object SortOrder {
  private val DEFAULT_SORT_ORDER = SortOrder.of(1000L)
  private val defaultSortOrders = Map(
    Role.INBOX -> SortOrder.of(10L),
    Role.ARCHIVE -> SortOrder.of(20L),
    Role.DRAFTS -> SortOrder.of(30L),
    Role.OUTBOX -> SortOrder.of(40L),
    Role.SENT -> SortOrder.of(50L),
    Role.TRASH -> SortOrder.of(60L),
    Role.SPAM -> SortOrder.of(70L),
    Role.TEMPLATES -> SortOrder.of(80L),
    Role.RESTORED_MESSAGES -> SortOrder.of(90L))

  def getSortOrder(role: Option[Role]): SortOrder = role.flatMap(r => getDefaultSortOrder(r))
    .getOrElse(DEFAULT_SORT_ORDER)

  private def getDefaultSortOrder(role: Role) = defaultSortOrders.get(role)

  def of(sortOrder: UnsignedInt): SortOrder = {
    new SortOrder(sortOrder)
  }
}

case class SortOrder private(sortOrder: UnsignedInt) extends Ordered[SortOrder] {
  override def compare(that: SortOrder): Int = this.sortOrder.compare(that.sortOrder)
}

case class Mailbox(id: MailboxId,
                   mailboxName: MailboxName,
                   parentId: Option[MailboxId],
                   role: Option[Role],
                   sortOrder: SortOrder,
                   totalEmails: UnsignedInt,
                   unreadEmails: UnsignedInt,
                   totalThreads: UnsignedInt,
                   unreadThreads: UnsignedInt,
                   mailboxRights: MailboxRights,
                   isSubscribed: Boolean,
                   namespace: MailboxNamespace,
                   rights: Rights,
                   quotas: Quotas){
  require(id != null, "'id' is mandatory")
  require(mailboxName != null, "'mailboxName' is mandatory")

  def hasRole(role: Role): Boolean = this.role.exists(_.equals(role))

  def hasSystemRole: Boolean = role.exists(_.isSystemRole)
}