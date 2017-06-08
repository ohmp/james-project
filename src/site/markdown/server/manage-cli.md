Manage James via the Command Line
=================================

With any distribution, James is packed with a command line client.

To use it enter, for Spring distrubution:

```
./bin/james-cli.sh -h 127.0.0.1 -p 9999 COMMAND
```

And for Guice distributions:

```
java -jar /root/james-cli.jar -h 127.0.0.1 -p 9999 COMMAND
```

The following document will explain you which are the available options for **COMMAND**.

Note: the command line before **COMMAND** will be documente as *{cli}*.

## Manage Domains

Domains represent the domain names handled by your server.

You can add a domain:

```
{cli} AddDomain domain.tld
```

You can remove a domain:

```
{cli} RemoveDomain domain.tld
```

Check if a domain is handled:

```
{cli} ContainsDomain domain.tld
```

And list your domains:

```
{cli} ListDomains
```

## Managing users

Users are accounts on the mail server. James can maintain mailboxes for these mailaddress.

You can add a user:

```
{cli} AddUser user@domain.tld password
```

Note: the domain used should have been previously created.

You can delete a user:

```
{cli} RemoveUser user@domain.tld
```

And reset its password:

```
{cli} SetPassword user@domain.tld password
```

Note: All these write operations can not be performed on LDAP backend, as the implementation is read-only.

Finally, you can list users:

```
{cli} ListUsers
```

## Managing mailboxes

An administrator can perform some basic operation on user mailboxes.

Note on mailbox formatting: mailboxes are composed of three parts.

 - The namespace, indicating what kind of mailbox it is. (Shared or not?). The value for users mailboxes is #private
 - The username.
 - And finally mailbox name. Be aware that '.' serves as mailbox hierarchy delimiter.

An administrator can delete all of the mailboxes of a user, which is not done automatically when removing a user (to avoid data loss):

```
{cli} DeleteUserMailboxes user@domain.tld
```

He can delete a specific mailbox:

```
{cli} DeleteMailbox #private user@domain.tld INBOX.toBeDeleted
```

He can list the mailboxes of a specific user:

```
{cli} ListUserMailboxes user@domain.tld
```

And finally can create a specific mailbox:

```
{cli} CreateMailbox #private user@domain.tld INBO.newFolder
```

## Managing mappings

A mapping is a recipient rewritting rule. There is several kind of rewritting rules:

 - address mapping: rewritte a given mail address into an other one.
 - regex mapping.

You can manage address mapping like this:

```
{cli} AddAddressMapping redirected domain.new user@domain.tld
{cli} RemoveAddressMapping redirected domain.new user@domain.tld
```

You can manage regex mapping like this:

```
{cli} AddRegexMapping redirected domain.new .*@domain.tld
{cli} RemoveRegexMapping redirected domain.new .*@domain.tld
```

You can view mapping for a mail address:

```
{cli} ListUserDomainMappings user domain.tld
```

And all mappings defined on the server:

```
{cli} ListMappings
```

## Manage quotas

Quotas are limitations on a group of mailboxes. They can limit the **size** or the **messages count** in a group of mailboxes.

James groups by defaults mailboxes by user (but it can be overridden), and labels each groups with a quotaroot.

To get the quotaroot a given mailbox belongs to:

```
{cli} GetQuotaroot #private user@domain.tld INBOX
```

Then you can get the specific quotaroot limitations.

For the number of messages:

```
{cli} GetMessageCountQuota quotaroot
```

And for the storage space available:

```
{cli} GetStorageQuota quotaroot
```

You see the maximum allowed for these values:

For the number of messages:

```
{cli} GetMaxMessageCountQuota quotaroot
```

And for the storage space available:

```
{cli} GetMaxStorageQuota quotaroot
```

You can also specify maximum for these values.


For the number of messages:

```
{cli} SetMaxMessageCountQuota quotaroot value
```

And for the storage space available:

```
{cli} SetMaxStorageQuota quotaroot value
```

With value being an integer. Please note the use of units for storage (K, M, G).

Moreover, James allows to specify defaults maximum values, at the server level. Note: syntax is similar to what was exposed previously.

```
{cli} SetDefaultMaxMessageCountQuota value
{cli} GetDefaultMaxMessageCountQuota
{cli} SetDefaultMaxStorageQuota value
{cli} GetDefaultMaxStorageQuota
```

## Re-indexing

James allow you to index your data in an indexer, for making search faster. Both ElasticSearch and Lucene are supported.

For some reasons, you might want to re-index your documents (inconsistencies across datastore, migrations).

To reindex all mail of all mailboxes of all users, type:

```
{cli} ReindexAll
```

And for a precise mailbox:

```
{cli} Reindex #private user@domain.tld INBOX
```

## Sieve scripts quota

James allow you to configure a Sieve mailet, and stores Sieve scripts. You can update them via the ManageSieve protocol,
or via the ManageSieveMailet.

You can define quota for the total size of Sieve scripts, per user.

Syntax is similar to what as exposed for quotas. For defaults values:

```
{cli} GetSieveQuota
{cli} SetSieveQuota value
{cli} RemoveSieveQuota
```

And for specific user quotas:

```
{cli} GetSieveUserQuota user@domain.tld
{cli} SetSieveQuota user@domain.tld value
{cli} RemoveSieveUserQuota user@domain.tld
```

## Changing of mailbox implementation

Migration is experimental for now. You would need to customize **Spring** configuration to add a new mailbox manager with a different bean name.

You can then copy data accross mailbox managers using:

```
{cli} CopyMailbox srcBean dstBean
```
