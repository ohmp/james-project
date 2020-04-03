# 28. Recompute mailbox quotas

Date: 2020-04-03

## Status

Accepted (lazy consensus)

## Context

JMAP custom quota extension, as well as IMAP [RFC-2087](https://tools.ietf.org/html/rfc2087) enables a user to monitor
the amount of space and message count he is allowed to use, and that he is effectively using.

To track the quota values a user is effectively using James relies on the 
[enventBus](../site/markdown/server/manage-guice-distributed-james.md#mailbox-event-bus) to increment a cassandra counter
corresponding to this user.

However, upon Cassandra failure, this value can be incorrect, hence the need of correcting it.

## Data model details

Table: imapUidTable: Holds mailbox and flags for each message, lookup by message ID

Table: messageV2: Holds message metadata, independently of any mailboxes. Content of messages is stored in `blobs` 
       and `blobparts` tables.
       
Table: currentQuota: Holds per quota-root current values. Quota-roots defines groups of mailboxes which shares quotas 
limitations.

Operation:
 - Quota updates is done asynchronously (event bus + listener) for successful mailbox operations.
   - If the quota update is not applied, then we are inconsistent
   - To be noted, event bus mechanisms should already limit

## Decision

We will implement a generic corrective task exposed via webadmin.

This task can reuse the `CurrentQuotaCalculator` and call it for each and every quotaRoot of each user.

This way, non-cassandra implementation will also benefit from this task.

## Consequences

This task is not concurrent-safe. Concurrent operations will result in an invalid quota to be persisted.

However, as the source of truth is not altered, re-running this task will eventually return the correct result.