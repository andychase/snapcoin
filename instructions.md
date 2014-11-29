Bitcoin Email Payments
======================

If you don't have a Smartphone your options for paying with Bitcoins are
limited.

You'll need:

-   A computer you can leave on
-   A new email account that's secure (with an address that's obscure)
-   A (Blockchain.info)\* wallet with spare change (don't put much in it
    for security)
-   A phone with text-to-email\*\* support and a camera.
-   This server

\* In the future I'd like to support more than this wallet service.

\*\* You might have this ability without realizing it.

Setup:

-   You download the server and run it (In the future, I hope to make a
    service to make it way more accessible for everyday folk). Enter
    your info and click 'start'. Wait a second and hopefully get a
    'running' message.
-   Enter the obscure email address into your phone. (Most phones
    support sending text and pictures to email addresses, try yours and
    see).

How it works:

-   When you're out and want to make a payment, snap a pic of the qr
    code (being careful to to get a clean image including the whitespace
    around the qr code)
-   Send that picture to your email address
-   In 40-80 seconds\* you should get a response saying the payment has
    been made.

\*Note: The biggest delay is downloading the image back to your computer
and processing it. The server uses IDLE-based push email notifications.

Running The Server
------------------

-   Download the JAR
    [Binary](https://github.com/asperous/photomoney/raw/master/photomoney.jar)
    (Note: binaries can have security risks. I encourage you to look
    over the source and building yourself. Also remember the binaries of
    libraries I included could be compromised. I just recommend simply
    not putting very much in the wallet you use with this server. )
-   Server requires Java, if you have it you can probably just
    double-click the jar. Otherwise `java -jar photomoney.jar`.
-   You can also run the server in gui-less mode. Usage:

        java -jar photomoney.jar <host> <email username/email> <email password> blockchain <blockchain token> <blockchain password>

Warnings
--------

THIS SOFTWARE IS ALPHA/PROOF-OF-CONCEPT. THE SOFTWARE IS PROVIDED "AS
IS", WITHOUT WARRANTY OF ANY KIND.

Absolutely do not provide this program with any credentials to wallets
with funds you aren't willing to loose.

I am not a security expert and have not yet gotten this software audited
by people who are. If you want to see this software become more secure
consider contributing to its development.

Also, don't use an email address that you might use for other things.
Your address should be reserved for use of this server only. This is
because the server replies to all mails and also deletes mail after it
processes them.

If you use it with your main email prepare to watch your emails
disappear!

Security Risks/Information:

-   This software runs on your Computer, in a JVM.
-   Your credentials aren't stored after the server is closed.
-   Currently any qr code sent to the provided email will be processed
    regardless of source, without limit.
-   Anyone with access to your phone could make payments (consider a
    screen lock).
-   Emails are sent with stmps and recieved using imaps. Email is
    deleted after being processed.
-   Your bitcoin credentials are sent over the internet using secure
    https/SSL for processing. Blockchain.info uses AES\_128\_GCM with
    ECDHE\_RSA keys. Java handles the Certificate Authority.
-   Other risks I haven't thought of.

Building
--------

You'll need [sbt](http://www.scala-sbt.org/).

> sbt compile

or

> sbt one-jar

