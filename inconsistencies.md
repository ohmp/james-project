# Solving Cassandra consistency

This plan makes the following assumption:

  - We are not willing to rewrite mailbox/cassandra with event sourcing
  - We will try to identify possibly inconsistent duplicated information, and how to resolve it.

# Details

## mailbox/mailboxPath

Table: mailbox
Table: mailboxPathV2

Why: This projection is used for reading mailboxes by mailboxPath or by mailboxId. We also ensure mailboxPath unicity.

Operation:
 - Create: We write to `mailboxPathV2` in order to ensure path unicity, then write to `mailbox`. If partially applied,
 then we can have an orphan `mailboxPathV2`. The corresponding mailbox don't exist, but can't either be created.
 - Rename: We write the new path to `mailboxPathV2` in order to ensure path unicity, then we read the old path from 
 `mailbox` (op1) to remove it from `mailboxPathV2` (op2) and finally we update the `mailbox` to reference it with 
 new path (op3). If op1, op2 or op3 fails, we have an entry conflict that might require merging the mailboxes.
 - Delete: We remove the `mailbox` entry, and finally the `mailboxPathV2`. If partially applied,
 then we can have an orphan `mailboxPathV2`. The corresponding mailbox don't exist, but can't either be created.

Mitigation:
 - As part of Sprint 15 we:
  - Add a retry strategy on write operations that can lead to inconsistencies
  - We removed unecessary operations out of the write path to increase resilience
  
Solution:

We can create a webadmin to solve these inconsistencies. Orphan mailboxPaths should be removed, and conflicts reported
so that an admin can use the mailbox merging endpoint.

However, due to the two invariants mentioned above, we can not identify a clear source of trust based on existing 
tables for the mailbox object. The task previously mentioned is subject to concurrency issues that might cancel 
legitimate concurrent user actions.

Hence this task must be run offline.

In the future, we should revisit the mailbox object data-model and restructure it, to identify a source of truth to 
base the inconsistency fixing task on. Event sourcing is a good candidate for this.

Criticity:
 - High. This had been spotted in production. It prevents from migrating some user mailboxes.

Read the following related ADR: https://github.com/linagora/james-project/pull/3147

**ADR pull request**: https://github.com/linagora/james-project/pull/3147/files?short_path=85aa5b4#diff-85aa5b4ec6d985fa1c3fbc972cd2e355

## ACL/CassandraUserMailboxRightsTable

Table: acl : holds the acls of a mailbox
Table: UserMailboxACL: Denormalisation table. Allow to retrieve non personal mailboxIds a user has right on

Why: This projection is to list mailboxes a user has rights on.

Operation:
 - Apply path: A conditional update is first performed on `acl` (either INSERT or UPDATE) and the result is referenced
  in `UserMailboxACL`. Race conditions or failure to save in `UserMailboxACL` could result in invalid `UserMailboxACL`,
  with a mailbox beeing referenced while it should not, a mailbox not referenced while it should or a mailbox 
  referenced with incorrect rights.
  
Mitigation:
 - Add a retry strategy on write operations on `UserMailboxACL` could limit inconsistencies.
 - Tests to demonstrate impact of partial failures

Solution:
We can create a webadmin to solve these inconsistencies. Iterating `acl` entries, we can rewrite entries in 
`UserMailboxACL`.
Iterating `UserMailboxACL` we can remove entries not referenced in `acl`.

Criticity: High as it impacts the right system. Not yet spotted.

**ADR pull request**: https://github.com/linagora/james-project/pull/3148/files?short_path=6d04996#diff-6d0499602ac1c7e4a3e8f6144e3ca271

## Message

Table: messageIdTable: Holds mailbox and flags for each message, lookup by mailbox ID + UID
Table: imapUidTable: Holds mailbox and flags for each message, lookup by message ID

Why: Access of a comon resource (messages) addressed by MessageId (JMAP) or mailboxId + uid (IMAP)

Inconsistencies here will mean a different mailbox content in JMAP of IMAP.

Operation:
 - Adding a message:
   - (CassandraMessageMapper) First reference the message in `messageIdTable` then in `imapUidTable`.
   - (CassandraMessageIdMapper) First reference the message in `imapUidTable` then in `messageIdTable`.
 - Deleting a message:
   - (CassandraMessageMapper) First delete the message in `imapUidTable` then in `messageIdTable`.
   - (CassandraMessageIdMapper) Read the message metadata using `imapUidTable`, then first delete the message in 
   `imapUidTable` then in `messageIdTable`.
 - Copying a message:
   - (CassandraMessageMapper) Read the message first, then first reference the message in `messageIdTable` then
    in `imapUidTable`.
 - Moving a message:
   - (CassandraMessageMapper) Logically copy then delete. A failure in the chain migh lead to duplicated message (present 
   in both source and destination mailbox) as well as different view in IMAP/JMAP.
   - (CassandraMessageIdMapper) First reference the message in `imapUidTable` then in `messageIdTable`.
 - Updating a message flags:
   - (CassandraMessageMapper) First update conditionally the message in `imapUidTable` then in `messageIdTable`.
   - (CassandraMessageIdMapper) First update conditionally the message in `imapUidTable` then in `messageIdTable`.

We need to choose a source of truth. MessageIdToImapUid should be prefered as it holds flags conditional updates.

 - We thus need to modify CassandraMessageMapper 'add' + 'copy' to first write to the source of truth (`imapUidTable`)

Mitigation:
 - Add a retry strategy on write operations on `messageIdTable` could limit inconsistencies.
 - Tests to demonstrate impact of partial failures (see preliminary work done here: 
 https://github.com/linagora/james-project/pull/3151 in JAMES-3076)

Solution:
We can create a webadmin to solve these inconsistencies. Iterating `imapUidTable` entries, we can rewrite entries 
in `messageIdTable`. Iterating `imapUidTable` we can remove entries not referenced in `messageIdTable`.

Criticity:
 - High. This had been spotted in production. (some STATUS failed).
 
**ADR pull request**: https://github.com/linagora/james-project/pull/3148/files?short_path=2b9b796#diff-2b9b796b57624d55c3e7b4bbc46e4314

## Message <-> attachment

Table: messageV2: Holds message metadata, independently of any mailboxes. Content of messages is stored in `blobs` 
       and `blobparts` tables.
Table: attachmentV2: Holds attachment for fast attachment retrieval. Content of messages is stored in `blobs` and 
`blobparts` tables.
Table: attachmentMessageId: Holds ids of messages owning the attachment

Foreign key relationship on immutable data.

We just need to test that a message cannot have missing attachments. 

Testing:
  - saving a message fails if saving one of it's attachment fails
  - saving a message fails if saving the messageId reference of an attachment fails
(we can not read that message + re-saving works + the attachment cannot be downloaded)

## Upload attachment

Table: attachmentV2: Holds attachment for fast attachment retrieval. Content of messages is stored in `blobs` and 
`blobparts` tables.
Table: attachmentOwners: Holds explicit owners of some attachments

Testing:
  - An upload fails if saving the attachment fails
  - An upload fails if saving it's user reference fail

(we can not read that attachment + JMAP re-upload works works)

## Current quota

Table: imapUidTable: Holds mailbox and flags for each message, lookup by message ID
Table: messageV2: Holds message metadata, independently of any mailboxes. Content of messages is stored in `blobs` 
       and `blobparts` tables.
Table: currentQuota: Holds per quota-root current values. Quota-roots defines groups of mailboxes which shares quotas 
limitations.

Operation:
 - Quota updates is done asynchronously (event bus + listener) for successful mailbox operations.
   - If the quota update is not applied, then we are inconsistent
   - To be noted, event bus mechanisms should already limit

Maybe running Event DeadLetter fixes + diagnosing issues can bring more insight here...

Correction can be done from source of truth: MessageIdToImapUid + messageV2 + list of user mailboxes (sum of all message 
size)
Read notes below about restrictions reseting counters.
As this is a counter value we might need to have an eventBus registration to not missconcurrent additions/deletes 
(and correct the count afterward?

## Tests about CassandraIndexTableHandler

We should ensure that a failed update to a given table don't impact updates to other tables.

 - if updating `messageDeleted` fails then `mailboxCounters` should still be updated (if applicable)
 - if updating `messageDeleted` fails then `applicableFlag` should still be updated (if applicable)
 - if updating `messageDeleted` fails then `firstUnseen` should still be updated (if applicable)
 - if updating `messageDeleted` fails then `mailboxRecents` should still be updated (if applicable)

 - if updating `mailboxCounters` fails then `messageDeleted` should still be updated (if applicable)
 - if updating `mailboxCounters` fails then `applicableFlag` should still be updated (if applicable)
 - if updating `mailboxCounters` fails then `firstUnseen` should still be updated (if applicable)
 - if updating `mailboxCounters` fails then `mailboxRecents` should still be updated (if applicable)

 - if updating `applicableFlag` fails then `messageDeleted` should still be updated (if applicable)
 - if updating `applicableFlag` fails then `mailboxCounters` should still be updated (if applicable)
 - if updating `applicableFlag` fails then `firstUnseen` should still be updated (if applicable)
 - if updating `applicableFlag` fails then `mailboxRecents` should still be updated (if applicable)

 - if updating `firstUnseen` fails then `messageDeleted` should still be updated (if applicable)
 - if updating `firstUnseen` fails then `mailboxCounters` should still be updated (if applicable)
 - if updating `firstUnseen` fails then `applicableFlag` should still be updated (if applicable)
 - if updating `firstUnseen` fails then `mailboxRecents` should still be updated (if applicable)

 - if updating `mailboxRecents` fails then `messageDeleted` should still be updated (if applicable)
 - if updating `mailboxRecents` fails then `mailboxCounters` should still be updated (if applicable)
 - if updating `mailboxRecents` fails then `applicableFlag` should still be updated (if applicable)
 - if updating `mailboxRecents` fails then `firstUnseen` should still be updated (if applicable)
 
https://github.com/linagora/james-project/pull/3150 shows the first encoutered errors aborts the overall pipeline, and
proposes a fix for it. It is merged but we remain to write systematic tests for this.

[JIRA](https://issues.apache.org/jira/browse/JAMES-3075) 

## Mailbox counters

Table: mailboxCounters : Holds messages count and unseen message count for each mailbox.

Correction can be done from source of truth: MessageIdToImapUid

Operation:
 - After a add/delete/update on a message a set of counter is maintained.
 An error updating the counter would lead to an invalid count.
 
Consideration:
 - Would moving this to the mailbox event bus responsibility help decrease inconsistencies 
 (retries if failed? EventDeadLetter?)
 
Correction can be done from source of truth: MessageIdToImapUid

See JAMES-3105 https://issues.apache.org/jira/browse/JAMES-3105 https://github.com/linagora/james-project/pull/3185 
contributes a first corrective task that needs to be run offline to be concurrency-free. To be noted, rerunning the
task upon consistencies issues will eventually be consistent.

Further work is required to make this work concurrency friendly.

Read notes below about restrictions reseting counters.

Idea: As this is a counter value we might need to have an eventBus registration to not miss concurrent additions/deletes 
(and correct the count afterward?)

## Applicale Flags

Table: applicableFlag : Holds flags being used on specific mailboxes. As system flags are implicit, this table stores
       user flags.
       
This table holds the union of user flags used in this mailbox for the applicable flags parameter of the SELECT IMAP 
command.

This table do not seem critical at all.

Correction can be done from source of truth: `MessageIdToImapUid`. Iterating all mailbox messages and performing a union 
on userFlags, then reset stored value in `applicableFlag`.

## Deleted Messages

Table: messageDeleted : Denormalisation table. Allows to retrieve UID marked as DELETED in specific mailboxes.
       
This table holds a list of MessageUid marked as deleted in order to fasten EXPUNGE related IMAP commands (also CLOSE).

Upon deletes this value is updated. If it fails we have an invalid reference.

Consideration:
 - Would moving this to the mailbox event bus responsibility help decrease inconsistencies 
 (retries if failed? EventDeadLetter?)

Correction can be done from source of truth: `MessageIdToImapUid`. Iterating `MessageIdToImapUid` we can rewrite 
messages marked as deleted in `messageDeleted`. Iterating `messageDeleted`, we remove entries not marked as deleted in 
`MessageIdToImapUid`.

## First unseen
Table: firstUnseen : Denormalisation table. Allow to quickly retrieve the first UNSEEN UID of a specific mailbox.

This table holds a list of MessageUid marked as unseen in order to fasten SELECT IMAP commands.

Consideration:
 - Would moving this to the mailbox event bus responsibility help decrease inconsistencies 
 (retries if failed? EventDeadLetter?)

Correction can be done from source of truth: `MessageIdToImapUid`. Iterating `MessageIdToImapUid` we can rewrite 
messages uids in `firstUnseen`. Iterating `firstUnseen`, we remove entries not existing in `MessageIdToImapUid`.

## Mailbox recent

Table: mailboxRecents : Denormalisation table. This table holds for each mailbox the messages marked as RECENT. This
       is a SELECT optimisation.

This table holds a list of MessageUid marked as recent in order to fasten SELECT & STATUS IMAP commands.

Consideration:
 - Would moving this to the mailbox event bus responsibility help decrease inconsistencies 
 (retries if failed? EventDeadLetter?)
 
Correction can be done from source of truth: `MessageIdToImapUid`. Iterating `MessageIdToImapUid` we can rewrite 
messages marked as recent in `mailboxRecents`. Iterating `mailboxRecents`, we remove entries not marked as recent in 
`MessageIdToImapUid`.

## Cassandra counter limitations

CF https://cwiki.apache.org/confluence/display/CASSANDRA2/Counters

Counter removal is intrinsically limited. For instance, if you issue very quickly the sequence "increment, remove, increment" it is possible for the removal to be lost (if for some reason the remove happens to be the last received messages). Hence, removal of counters is provided for definitive removal only, that is when the deleted counter is not increment afterwards. This holds for row deletion too: if you delete a row of counters, incrementing any counter in that row (that existed before the deletion) will result in an undetermined behavior. Note that if you need to reset a counter, one option (that is unfortunately not concurrent safe) could be to read its value and add -value.

Strong consistency can not be guaranteed here. Concurrent operations results in undefined behaviour.

The only truely consistant option might be to use a standard row + a lightweight condition...

We might want to explore the performance cost of this option.

# Action plan

(prioritized) this section present the conclusion of this studu which details can be found below.

## Grooming

Performance impact: lightweight transaction VS counters

## High

 - SolveInconsistencies webadmin tasks:
   - messages (MessageIdToImapUid -> MessageIdTable)
   - ACLs (ACL/CassandraUserMailboxRightsTable)
   
Grooming about counters (is event bus helping us here?):  
   - Mailbox counters (/!\ counters)
   - Current quota (/!\ counters)

 - Testing:
   - MessageIdToImapUid -> MessageIdTable
   - Message <-> attachment
   - Upload attachment
   - Tests about CassandraIndexTableHandler

 - Retry:
   - messages (MessageIdToImapUid -> MessageIdTable)

### Low

Used to speed up IMAP operation (SELECT / EXPUNGE)

 - SolveInconsistencies task:
   - Applicale Flags
   - Deleted Messages module
   - First unseen
   - Mailbox recent

### Consideration

Some denormalisation table updates can be done on the mailbox event bus to help decrease inconsistencies? (retries if 
failed? EventDeadLetter?)

Includes: `mailboxRecents`, `messageDeleted`, `mailboxCounters`, `applicableFlag`, `firstUnseen`

Impact: some tests then have to be moved from the mapper layer to the manager layer.
