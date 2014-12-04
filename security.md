---
layout: page
title: "Snapcoin: Security Model"
on_security: current
---

# Security Model

The security of your money is our highest priority.

When you register we create an account at [Blockchain](https://blockchain.info) and encode the details
into a special address. When you send qr code images to that address we forward the information to blockchain to
complete the transaction over a secure tls (https) channel. We don't store the information.

Neither we nor blockchain stores your private key. Blockchain stores a copy encrypted against the
secret credentials embedded in the secret address.

## Precaution

We are a service designed to make spending bitcoin easy. We aren't a fraud protection agency
and Bitcoin transactions are irreversible.

*Anyone with your phone can spend your money just as easily as you can.*

<i class="fa fa-exclamation-triangle"></i> **Don't store large amounts of money in your snapcoin.net account.**

## How to lose all of your money

When you register we send you a qr code from a special address.

Your money will be lost if you:

- Don't add the address to your contact list and your phone auto-deletes all messages from us
- Delete all messages from us without having that address in your contact list
- Delete all messages from us and delete the address from contact list

If you never added funds to your account and you deleted the address then you can simply re-sign-up
on our homepage and we'll make a new account for you (be more careful).

<i class="fa fa-exclamation-triangle"></i> **We can't recover your money if you delete the special address**.

## Business-closing risk

Your phone contains your Blockchain credentials.
You can transition to using their service if we go out of business.

If Blockchain goes out of business we will have no way of contacting you and you may lose your funds.

## Third-party risk

There's a quite a bit of third-party risk associated in using our application.

We use these services:

* Various phone carrier text-to-email/email-to-text services
* Heroku
* Mailgun

We use these technologies:

* Oracle JVM
* Scala
* RabbitMQ
* zxing
* zbar
* Blockchain api java client

## Expertise

Our code hasn't been audited (though you can audit it yourself, see [transparency](#transparency))

<i class="fa fa-exclamation-triangle"></i> **We aren't security experts**.

## Transparency

Our source code is available for review at [asperous/photomoney](https://github.com/asperous/photomoney).
