# Solving Cassandra consistency

This plan makes the following assumption:

  - We are not willing to rewrite mailbox/cassandra with event sourcing
  - We will rather identify projections and how to heal them

# Action plan

(prioritized) this section present the conclusion of this studu which details can be found below.

## Grooming

Performance impact: lightweight transaction VS counters

## High

 - SolveInconsistencies webadmin tasks:
   - messages (MessageIdToImapUid -> MessageIdTable)
   - ACLs (ACL/CassandraUserMailboxRightsTable)
   - Mailbox counters (/!\ counters)
   - Current quota (/!\ counters)

 - Testing:
   - MessageIdToImapUid -> MessageIdTable
   - Message <-> attachment
   - Upload attachment

 - Retry:
   - messages (MessageIdToImapUid -> MessageIdTable)

### Low

Used to speed up IMAP operation (SELECT / EXPUNGE)

 - SolveInconsistencies task:
   - Applicale Flags
   - Deleted Messages module
   - First unseen
   - Mailbox recent

# Details

## mailbox/mailboxPath

This projection is used for reading mailboxes by mailboxPath or by mailboxId.

Work under progress

 - rebuild the view: a SolveInconsistency task clean up the mailboxPath table for it to match mailboxId table. MailboxMerging is available for conflicts
 - retries decrease the chance of inconsistencies to arise
 - testing: better understanding current code behavior. Some simple fixes can be found this way to remove inconsistencies scenari: re-order operation, remove unecessary reads for instance.

## ACL/CassandraUserMailboxRightsTable

The UserMailboxRightsTable holds a projection of the MailboxAcl in order for a user to know which mailboxes he have access to.

ACL holds the main view, rebuilding the view should be easy.

 - rebuild the view: add ACL entries not present in the UserMailboxRightsTable, remove entries of UserMailboxRightsTable not present in ACLs

## Message

This projection allows accessing the mails by messageId (JMAP) or by [mailboxId + uid] (IMAP)

Two tables: MessageIdTable + MessageIdToImapUid

We need to choose a source of truth. MessageIdToImapUid should be prefered as it holds flags conditional updates.

Testing will aim at better understanding issues here: (we can not read that message + re-saving works)

 - saving a message fails if saving in MessageIdToImapUid fails

Retry insertion/update in MessageIdTable if failed in order to reduce "inconsistencies" occurences can be performed.

Rebuilding the MessageIdTable view from MessageIdToImapUid is easy: add entries of MessageIdToImapUid not present in MessageIdTable, remove entries of MessageIdTable not present in MessageIdToImapUid

## Message <-> attachment

Here there is no data duplication (no denormalization) but just a foreign key relationship. We just needs to ensure a message is not accessible if one of its attachments was not well persisted.

Testing:

  - saving a message fails if saving one of it's attachment fails
  - saving a message fails if savinng the messageId reference of an attachment fails

(we can not read that message + re-saving works)

## Upload attachment

Idem, no data duplication (no denormalization).

Testing:

  - An upload fails if saving the attachment fails
  - An upload fails if saving it's user reference fail

(we can not read that message + re-saving works)

## Current quota

Correction can be done from source of truth: MessageIdToImapUid (sum of all message size

Read notes below about restrictions reseting counters.

As this is a counter value we might need to have an eventBus registration to not missconcurrent additions/deletes (and correct the count afterward?

## Mailbox counters

Correction can be done from source of truth: MessageIdToImapUid

Read notes below about restrictions reseting counters.

As this is a counter value we might need to have an eventBus registration to not missconcurrent additions/deletes (and correct the count afterward?

## Applicale Flags
This table holds the union of user flags used in this mailbox for the applicable flags parameter of the SELECT IMAP command.

This table do not seem critical at all.

Correction can be done from source of truth: MessageIdToImapUid

## Deleted Messages

This table holds a list of MessageUid marked as deleted in order to fasten EXPUNGE related IMAP commands (also CLOSE).

Correction can be done from source of truth: MessageIdToImapUid

## First unseen

This table holds a list of MessageUid marked as unseen in order to fasten SELECT IMAP commands.

Correction can be done from source of truth: MessageIdToImapUid

## Mailbox recent

This table holds a list of MessageUid marked as recent in order to fasten SELECT & STATUS IMAP commands.

Correction can be done from source of truth: MessageIdToImapUid

## Cassandra counter limitations

CF https://cwiki.apache.org/confluence/display/CASSANDRA2/Counters

Counter removal is intrinsically limited. For instance, if you issue very quickly the sequence "increment, remove, increment" it is possible for the removal to be lost (if for some reason the remove happens to be the last received messages). Hence, removal of counters is provided for definitive removal only, that is when the deleted counter is not increment afterwards. This holds for row deletion too: if you delete a row of counters, incrementing any counter in that row (that existed before the deletion) will result in an undetermined behavior. Note that if you need to reset a counter, one option (that is unfortunately not concurrent safe) could be to read its value and add -value.

Strong consistency can not be guaranteed here. Concurrent operations results in undefined behaviour.

The only truely consistant option might be to use a standard row + a lightweight condition...

We might want to explore the performance cost of this option.


