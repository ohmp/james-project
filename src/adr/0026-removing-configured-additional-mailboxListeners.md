# 26. Removing a configured additional MailboxListener

Date: 2020-04-03

## Status

Accepted (lazy consensus)

## Context

James enables a user to register additional mailbox listeners.

The distributed James server is handling mailbox event processing (mailboxListener execution) using a RabbitMQ work-queue
per listener.

The distributed James server then declares a queue upon start for each one of these user registered listeners, that it
binds to the main event exchange. 

If the user unconfigures the listener, the queue and the binding are still present but not consumed. This results in 
unbounded queue growth eventually causing RabbitMQ resource exhaustion and failure.

## Decision

We need, upon start, to sanitize bindings, and remove the ones corresponding to mailboxListeners that are not configured,
using a name matching strategy.

The queue should not be deleted to prevent message loss.

Given a James topology with a non uniform configuration, the effective RabbitMQ routing will be the one of the latest 
started James server.

## Alternatives

We could also consider adding a webadmin endpoint to sanitize eventBus bindings, allowing more predictability than the
above solution but it would require admin intervention.
