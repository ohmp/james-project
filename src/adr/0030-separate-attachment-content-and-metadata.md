# 30. Separate attachment content and metadata

Date: 2020-04-13

## Status

Accepted (lazy consensus)

## Context

Some mailbox implementations of James store already parsed attachments for faster retrieval.

Here are the POJOs representing these attachments:

 - **Attachment** : holds an attachmentId, the attachment content, as well as the content type
 - **MessageAttachment** : composes an attachment with it's disposition within a message (cid, inline and name)
 - **Message** exposes its list of MessageAttachment when it is read with FetchType Full..
 - **Blob** represents some downloadable content, and can be either an attachment or a message. Blob has a byte array 
 payload too.
 
The following classes works with the aforementioned POJOs:
 
 - **AttachmentMapper** and **AttachmentManager** are responsible of storing and retrieving an attachment content.
 - **BlobManager** is used by JMAP to allow blob downloads.
 - Mailbox search exposes attachment content related criteria. These criteria are used by the JMAP protocol.

This organisation causes attachment content to be loaded every time a message is fully read (which happens for instance
when you open a message using JMAP) despite the fact that it's not needed.

Also, the content being loaded "at once", we allocate memory space to store the whole attachment, which is sub-optimal.

To be noted that JPA and maildir mailbox implementations do not support attachment storage. To retrieve attachments of a 
message, these implementations parse the message to extract their attachments.

Cassandra mailbox prior schema version 4 stores attachment and its metadata in the same table, but from version 5 relies 
on the blobStore to store the attachment content.

## Decision

Enforce cassandra schema version to be 5 from James release 3.5.0. This allows to drop attachment management prior version
5.

We will re-organize the attachment POJOs: 

 - **Attachment** should hold an attachmentId, a content type, and a size. It will no longer hold the content.
 - **MessageAttachment** : composes an attachment with it's disposition within a message (cid, inline and name)
 - **Blob** would no longer hold the content as a byte array but rather a content retriever (`Supplier<InputStream>`)
 - **ParsedAttachment** is the direct result of attachment parsing, and composes a **MessageAttachment** and the 
 corresponding content as byte array. This class is only relied upon when saving a message in mailbox. This is used as 
 an output of `MessageParser`.

Some adjustments are needed on class working with attachment:

 - **AttachmentMapper** and **AttachmentManager** needs to allow from an attachmentId to retrieve the attachment content
 as an `InputStream`. This is done through a separate `AttachmentLoader` interface.
 - **AttachmentMapper** and **AttachmentManager** needs the Attachment and its content to persist an attachment
 - **MessageManager** then needs to return attachment metadata as a result of Append operation.
 - **InMemoryAttachmentMapper** needs to store attachment content separately.
 - **MessageStorer** will take care of storing a message on the behalf of `MessageManager`. This enables to determine if 
 attachment should be parsed or not on an implementation aware fashion, saving attachment parsing upon writes for JPA 
 and Maildir.
 
Maildir and JPA no longer support attachment content loading.

Mailbox search attachment content criteria will be supported only on implementation supporting attachment storage.

## Consequences

Users running Cassandra schema version prior version 5 will have to go through James release 3.5.0 to upgrade to a 
version after version 5 before proceeding with their update.

We noticed performance enhancement when using IMAP FETCH and JMAP GetMessages. Running a gatling test suite exercising 
JMAP getMessages on a dataset containing attachments leads to the following observations:

 - Overall better average performance for all JMAP queries (10% global p50 improvement)
 - Sharp decrease in tail latency of getMessages (x40 time faster)

We also expect improvements in James memory allocation.

## References

 - [Contribution on this topic](https://github.com/linagora/james-project/pull/3061). Also contains benchmark for this 
 proposal.
 - [JIRA](https://issues.apache.org/jira/browse/JAMES-2997)