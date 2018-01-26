---
layout: post
title:  "New administration features: manage your queues and your reporitories"
date:   2018-01-26 00:00:22 +0200
categories: james update
---

An often asked feature for James is the abilitiy to handle the mails that after some processing
have landed in a repository, for example /var/mail/error/.

A brand new webadmin API allows you to handle the content of these repositores, see the documentation in [manage-webadmin].
It allows you to list the repositories, their content, but also to reprocess one or several mails in the processor and queue
of your choice.

Oh! And bonus, now you can forget this non scalable file repository and use the brand new Cassandra repository!

On the same documentation page you will also find the new mail queue management API, done to list queued mails, remove some
of them regarding different criteria, but also flush delayed mails.

[manage-webadmin]: https://james.apache.org/server/manage-webadmin.html
