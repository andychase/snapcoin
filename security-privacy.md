---
layout: page
title: "Snapcoin: Security Model"
on_security_privacy: current
---

Contents
========
{:.no_toc}


* Table of Contents
{:toc}

Security
========

<i class="fa fa-exclamation-triangle"></i>
Never keep large amounts of money in your snapcoin.net account.
{:.warning}

<i class="fa fa-exclamation-triangle"></i>
Snapcoin.net does not insure your funds. Bitcoin transactions are irreversible.
{:.warning}

<i class="fa fa-exclamation-triangle"></i>
Don't give out your snapcoin.net account address.
{:.warning}

<i class="fa fa-exclamation-triangle"></i>
Anyone can spend your money from your phone as easily as you can.
Enable the security features on your phone and don't let strangers use your phone.
{:.warning}

Privacy
=======

Information kept
----------------

* Analytics information (including your ip) when browsing [snapcoin.net](https://snapcoin.net).
* Timing information associated with using our service.

  The server numbers each requests and logs the time it took to complete that request number.

* If you send a QR code without an amount you want to send,
  a one-way hash of your credentials is saved
  as well as the destination bitcoin address.

  This information is freed from memory after you complete
  the transaction or after 5 minutes, whichever is first.


Information not kept
--------------------

* Your phone number or carrier.
* Your snapcoin.net account address.
* Any information that would enable us to spend your money.

Services used
-------------

* [Mailgun](https://mailgun.com)
* [Blockchain.info](https://blockchain.info)'s api
* [Heroku](https://heroku.com)

Snapcoin.net cannot make guarantees about what they keep or don't keep.