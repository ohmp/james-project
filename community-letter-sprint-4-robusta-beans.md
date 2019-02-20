Hi folks!

In the three coming weeks, me, Duc TRAN, René CORDIER, and Dat PHAM will be working on two cool features: "Event Dead Letter" and "Deleted Messages Vault"

As you know we recently refactored the event system. As part of this effort we promoted error handling as an event bus responsibility. Retries with exponential back-off are performed. When these 
retries are exausted, the event is stored in "dead letter" for diagnose and replay purposes. We then plan to finish the work related to this topic: add the webAdmin REST APIs, add the Cassandra
storage.

Owning a mail server exposes you to tricky user requests as well as some legal related responsibilities. Users might delete some mails by mistake and will need to recover them. Furthermore, an
administrator might be requested by the authorities for emails, including the deleted ones. A classic way to handle these corner cases is through backups, but it then is back-end specific,
administration intensive, and data between snapshots might be missing.

Thus we decided to leverage these concerns as supported features (mailbox plugin). We named it "Deleted Messages Vault". The vault is an API whose first implementation will leverage mail
repositories. Our target for this sprint is to allow restoring deleted emails, stored in the vault, matching a specific query, into the user mailboxes.

Here is the list of tickets that we plan to tackle:

 - MAILBOX-380 Cassandra EventDeadLetter implementation
 - JAMES-2661 EventDeadLetter read + delete in webAdmin
 - MAILBOX-382 EventDeadLetter reschedule event API
 - JAMES-2666 EventDeadLetter integration tests

 - MAILBOX-378 Refactoring: MessageManager.expunge should read then delete
 - MAILBOX-379 PreDeletionHook design + plug
 - MAILBOX-381 DeletedMessagesVault API + Contract test + implementation on top of MailRepositories
 - JAMES-2662 Implement a PreDeletionHook to move items in the vault
 - JAMES-2663 WebAdmin endpoint: restore deleted messages
 - JAMES-2664 Guice loading for PreDeletionHooks
 - JAMES-2665 Integration test for the vault restore operation

These features will be implemented on top of Guice. To get these features available for JPA Guice, these tickets requires to be handled. Don't hesitate to give it a try, as these features
might interest the community as all!

 - JAMES-2650 JPA Event dead letter
 - JAMES-2656 JPA MailRepository

Best regards,

Benoit TELLIER
