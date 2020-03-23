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
  def delegated(owner: Username) = DelegatedNamespace(owner)

  def personal = PersonalNamespace
}

sealed trait MailboxNamespace {
  def `type`: String
  def owner: Option[Username]
}

case object PersonalNamespace extends MailboxNamespace {
  override def `type`: String = "Personal"

  override def owner: Option[Username] = None
}

case class DelegatedNamespace(user: Username) extends MailboxNamespace {
  override val `type`: String = "Delegated"

  override val owner: Option[Username] = Some(user)
}

object SortOrder {
  private val defaultSortOrders = Map(
      Role.INBOX -> SortOrder(10L),
      Role.ARCHIVE -> SortOrder(20L),
      Role.DRAFTS -> SortOrder(30L),
      Role.OUTBOX -> SortOrder(40L),
      Role.SENT -> SortOrder(50L),
      Role.TRASH -> SortOrder(60L),
      Role.SPAM -> SortOrder(70L),
      Role.TEMPLATES -> SortOrder(80L),
      Role.RESTORED_MESSAGES -> SortOrder(90L))
    .withDefaultValue( SortOrder(1000L))

  def getSortOrder(role: Role): SortOrder = defaultSortOrders(role)
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
  def hasRole(role: Role): Boolean = this.role.contains(role)

  val hasSystemRole: Boolean = role.exists(_.isSystemRole)
}