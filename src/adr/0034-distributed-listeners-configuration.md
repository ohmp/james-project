# 34. Removing a configured additional MailboxListener

Date: 2020-04-23

## Status

Proposed

Supercedes [26. Removing a configured additional MailboxListener](0026-removing-configured-additional-mailboxListeners.md)

## Context

James enables a user to register additional mailbox listeners.

The distributed James server is handling mailbox event processing (mailboxListener execution) using a RabbitMQ work-queue
per listener.

Currently, mailbox listeners are determined by the guice bindings of the server and additional mailbox listener defined
via configuration files.

While the configuration might be specific for each James server, what actually is defined in RabbitMQ is common. 
Heterogeneous configuration might then result in unpredictable RabbitMQ resource status. This was left as a limitation
of [26. Removing a configured additional MailboxListener](0026-removing-configured-additional-mailboxListeners.md).

## Decision

We need to centralize the definition of mailbox listeners.

An event sourcing system will track the configured mailbox listeners.

It will have the following commands:

 - **AddListener**: Add a given listener. This should be rejected if the group is already used.
 - **RemoveListener**: Remove a given listener.

It will have the following events:

 - **ListenerAdded**: A mailbox listener is added
 - **ListenerRemoved**: A mailbox listener is removed

A subscriber will react to these event to modify the RabbitMQ resource accordingly by adding queues, adding or removing
binding.

This event sourcing system differs from the one defined in
[26. Removing a configured additional MailboxListener](0026-removing-configured-additional-mailboxListeners.md) by the
fact that we should also keep track of listener configuration.

Upon start, James will ensure the **configured mailbox listener event sourcing system** contains the guice injected 
listeners, and add them if missing (handling the RabbitMQ bindings by this mean), then starts the eventBus which will
consume the given queues.

If a listener is configured with a class unknown to James, the start-up fails. This can happen if a custom jar, containing
user implemented mailbox listeners is partially deployed.

This differs from [26. Removing a configured additional MailboxListener](0026-removing-configured-additional-mailboxListeners.md)
by the fact we no longer need to register all listeners at once.

A WebAdmin endpoint will allow:

 - **to add a listener** to the one configured. Such a call:
    - Will fail if the listener is unknown to James, or if the 
    - Upon success the listener is added to the **configured mailbox listener aggregate**, and the listener is 
    registered locally. No broadcast is attempted, meaning that other James server will need a reboot to actually start 
    consuming the queue.
 - **to remove a listener**. Such a call:
    - Will fail if the listener is required by Guice bindings on the current server (distributed check will not be 
    implemented) or if the listener is not configured.
    - Upon success, the listener is removed from to the **configured mailbox listener aggregate**, and the listener is 
    unregistered locally. No broadcast is attempted, meaning that other James server will need a reboot to actually stop 
    consuming the queue. However, new events will stop arriving in the queue as its binding will be removed.

Integration tests of the distributed James product will require to be ported to perform additional mailbox listener
registry with this WebAdmin endpoint.

We will also expose a endpoint listing the groups currently in use, and for each group the associated configuration, if 
any. This will query the **configured mailbox listener aggregate**.

We will introduce a health-check to actually ensure that RabbitMQ resources matches the configured listeners, and propose
a WebAdmin to add/remove bindings/queue in a similar fashion of what had been proposed in 
[26. Removing a configured additional MailboxListener](0026-removing-configured-additional-mailboxListeners.md).

## Consequences

All products other than "Distributed James" are unchanged.

All the currently configured additional listeners will need to be registered .

The definition of mailbox listeners is thus centralized and we are not exposed to an heterogeneous configuration 
incident.

## Possible evolutions

A broadcast can be attempted to propagate eventBus topology changes:

 - Each James server registers an exclusive queue to a "eventBus topology change" exchange.
 - Upon modification of the actual topology a "add" or "remove" event is emitted.
 - Each running James react to these event by instantiating the corresponding listener and starting consuming the 
 associated queue, or stops consuming the associated queue.
 
If a listener is added but is not in the classpath, an ERROR log is emitted. This can happen during a rolling upgrade,
which defines a new guice binding for a new mailbox listener. Events will still be emitted (and consumed by other James)
servers however a local James upgrade will be required to effectively be able to start processing these events. 

Propagating changes will thus no longer server reboot.
