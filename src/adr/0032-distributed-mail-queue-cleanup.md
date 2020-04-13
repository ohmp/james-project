# 32. Distributed Mail Queue Cleanup

Date: 2020-04-13

## Status

Proposed

## Context

Read [Distributed Mail Queue](0031-distributed-mail-queue.md) for full context.

**enqueuedMailsV3** and **deletedMailsV2** is never cleaned up and the corresponding blobs are always referenced. This is not
ideal both from a privacy and space storage costs point of view.

## Decision

Add a new `contentStart` table referencing the point in time from which a given mailQueue holds data, for each mail queue.

The values contained between `contentStart` and `browseStart` can safely be deleted.

We can perform this cleanup upon `browseStartUpdate`: once finished we can browse then delete content of **enqueuedMailsV3**
and **deletedMailsV2** contained between `contentStart` and the new `browseStart` then we can safely set `contentStart` 
to the new `browseStart`.

Failing cleanup will lead to the content being eventually updated upon next browseStart update.

## Consequences

MailQueue content will eventually be dropped both in Cassandra and in ObjectStorage once Deduplicated Blob Store garbage 
collection is implemented. This will be both allow reclaiming storage space, reducing related costs, and respect privacy 
of James users.

Updating browse start will then be two times more expensive.
