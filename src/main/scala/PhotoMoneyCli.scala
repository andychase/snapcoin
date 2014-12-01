import javax.mail.{Flags, Message}

import scala.collection.mutable

object PhotoMoneyCli extends App {
    override def main(args: Array[String]) {
        var paymentProvider: Option[PaymentProvider] = None
        val receivedEmails = new mutable.HashSet[Array[Byte]]()
        var replier: Option[SmtpReplier] = None


        // Cli Entry Point
        args.length match {
            case 5 => start(args(0), args(1), args(2), args(3), args(4))
            case _ =>
                println("Usage: <email host> <email username/email> <email password> <blockchain token>")
        }

        def start(
                     imap_host: String,
                     smtp_host: String,
                     email_user: String,
                     email_password: String,
                     blockchain_api: String
                     ) {
            paymentProvider = Some(new BlockchainPayments(blockchain_api))
            paymentProvider.get.validateCredentials()
            replier = Some(new SmtpReplier(smtp_host, email_user, email_password))
            val photoMoneyServer = new PhotoMoneyServer(paymentProvider.get)

            val callbacks: List[(Message) => Unit] =
            // Main Message Processor
                (EmailMainProcessor.processMessage(_: Message, receivedEmails, photoMoneyServer, replier)) ::
                    // Delete message after handling it
                    ((msg: Message) => msg.setFlag(Flags.Flag.DELETED, true)) :: Nil

            new ImapListener(imap_host, email_user, email_password, expunge = true, callbacks = callbacks)
        }
    }
}
