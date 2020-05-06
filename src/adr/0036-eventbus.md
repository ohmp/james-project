# 36. Event bus

Date: 2020-05-05

## Status

Accepted (lazy consensus)

## Context

James mailbox uses an event driven architecture. 
It means every meaningful action on mailboxes or messages triggers an event for any component to react to that event.
`MailboxListener` allows executing actions upon mailbox events. They could be used for a wide variety of purposes, like 
enriching mailbox managers features or enabling user notifications upon concurrent mailboxes operations.

Interactions happen via the managers (RightManager, MailboxManager, MessageManager, MessageIdManager) which emits an
event on the EventBus, which will ensure the relevant MailboxListeners will be executed.

MailboxListener can be registered in a work queue fashion on the eventBus. Each work queue correspond to a given 
MailboxListener class with the same configuration, identified by their group. Each event is executed at least once
within a James cluster, errors are retried with an exponential back-off delay. If the execution keeps failing, the event
 is stored in `DeadLetter` for later reprocessing, triggered via WebAdmin.

Guice products enable the registration of additional mailbox listeners. A user can furthermore define its own 
mailboxListeners via the use of `extension-jars`.

MailboxListener can also be registered on a notification fashion on the eventBus. A mailbox listener can be registered 
via a `registrationKey` identifying entities concerned by the event. Upon event emission, the manager will indicate the 
`registrationKey` this event should be sent to. A mailboxListener will thus only receive the event for the registration 
key it is registered to.

## Features

The following features are leveraged through the use of Group mailbox listeners:

 - Email indexing in Lucene or ElasticSearch
 - Deletion of mailbox annotations
 - Cassandra Message metadata cleanup upon deletion
 - Current Quota updates
 - Quota indexation
 - OverQuota mailing
 - SpamAssassin Spam/Ham reporting

## Decision

Provide an InVM version of the EventBus. This implementation is not distributed.

Provide a distributed implementation of the EventBus leveraging RabbitMQ.

Events will be emitted to a single Exchange.

Each group will have a corresponding queue, bound to the main exchange, with a default routing key. Each eventBus
will consume this queue and execute the relevant listener, ensuring at least once execution at the cluster level.

Retries are managed via a dedicated exchange for each group: as we need to count retries, the message headers need to 
be altered and we cannot rely on rabbitMQ build in retries. Each time the execution fails locally, a new event is emitted 
via the dedicated exchange, and the original event is acknowledged.

Each eventBus will have a dedicated exclusive queue, bound to the main exchange with the registrationKeys used by local 
notification mailboxListeners (to only receive the corresponding subset of events). Errors are not retried for 
notification.

## Related ADRs

The implementation of the the distributed EventBus suffers from the following flows:

 - [Removing a configured additional MailboxListener](0026-removing-configured-additional-mailboxListeners.md)
 - [Distributed Mailbox Listeners Configuration](0035-distributed-listeners-configuration.md) also covers more in details
 topology changes and supersedes ADR 0026. 
 
The following enhancement have furthermore been contributed:

 - [EventBus error handling upon dispatch](0027-eventBus-error-handling-upon-dispatch.md)
