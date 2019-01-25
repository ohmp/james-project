Hi Apache James community,

We, at Linagora had been working hard on the past few months on making James a fully distributed email server.

This work had been achieved on top of the Guice product 'Cassandra-ElasticSearch-RabbitMQ'.

Our team did rewrite most of the mailbox event system, introducing a new EventBus component, which has
a memory InVM based implementation, as well as a RabbitMQ one.

Mailbox events are used by MailboxListeners to:
 - Extend the MDA capabilities of the Mailbox. We can find examples like Mailbox Indexing, SpamAssassin feedback, quota
 updates, etc...
 - Notifications. Like IMAP IDLE. And in the future this will be used by JMAP PUSH
 - Note that using Guice products, an admin can load custom mailbox listeners (achieving MDA extensibility)

We did pay a close look to error handling:
 - MailboxListener should now delegate error handling to the eventBus
 - The eventBus will retry MailboxListener execution, using a backoff strategy
 - When the maximum number of retries is exceeded, the event is stored in "dead-letters" so that the admin can diagnose
 issues and re-schedule delivery.
 - RabbitMQ eventBus ensures "at least once" delivery

We wrote an article about the RabbitMQ EventBus here: [1]

We now intend to further test and experiment with this brand new distributed James server!

An external eventBus like RabbitMQ also means that we need to serialize events. Being not satisfied of the verbose approach using
Jackson, the team did write this serialization logic using Scala (well isolated, well tested, and well integrated to Java 8 code) with
satisfying results. We wrote a quick article about scala JSON serialization here: [2]

Another recent topic had been mail aliases. We introduced a specific mapping type to highlight this intent. And we allowed listing sources
from a given mapping (here listing the aliases of a user). The associated webadmin routes can be found here: [3].

Last but not least, we are porting existing code of specific modules (Cassandra, rabbitMQ) to Reactor, to allow easier to maintain, and more efficient
asynchronous code. You can read about this ongoing effore here [4].

[1] https://medium.com/p/b159f46704be/ : Some explanations about the RabbitMQ eventBus
[2] https://medium.com/p/602530f68b75/ : Toward a polyglot James server... Scala and JSON serialization for mailbox events
[3] https://github.com/apache/james-project/blob/master/src/site/markdown/server/manage-webadmin.md#creating-address-aliases : Aliases routes documentation
[4] https://medium.com/p/3be69af3f0a9/ : Our journey with Reactor.

Other James related readings:

[5] https://medium.com/p/602530f68b75/ : Next level Java 8 staged builders, a design pattern we used to provide nice builders for Mailbox Events

Best regards,

Benoit TELLIER