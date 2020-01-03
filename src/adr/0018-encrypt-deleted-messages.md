# 18. Encrypt deleted messages

Date: 2020-01-03

## Status

Proposed

## Context

James relies on a blobStore for storing blobs.

One of its usage is the DeletedMessage Vault which should support permanent deletion. Currently, this is implemented via 
BlobStore::delete operation.

However, deleting a blob have some serious implications:
 - It complicates invariant for (soon to be contributed) deleted blob suppression in a deduplicated context.
 - It introduces complex, concurrency issue for big blobs on top of scality (see [JAMES-2860])
 - [15. Persist BlobIds for avoiding persisting several time the same blobs within ObjectStorage](0015-objectstorage-blobid-list.md) 
 can not be safely implemented if a blob can be concurrently added or deleted
 
## Decision

Encrypt deleted messages.

AES encryption key is derived from a salt, that is being stored as part of metadata.

Metadata deletion and thus key deletion will prevent anyone from reading the message.

## Consequences

BlobStore::delete of a single blob as an application level feature is no longer needed. Concurrency issues of concurrent add/delete are avoided.

Adoption of [15. Persist BlobIds for avoiding persisting several time the same blobs within ObjectStorage](0015-objectstorage-blobid-list.md) should be eased, as
well as further work on 'distributed deduplicated blob garbage collection' upcoming work.

Deleted messages stored before AES encryption adoption are kept unencrypted. They will not be readable anymore from an applicative level
once deleted, but their content will be kept in the object storage.

## Reference
 - [JAMES-2860](https://issues.apache.org/jira/browse/JAMES-2860) : Read partial blob issue with Scality

