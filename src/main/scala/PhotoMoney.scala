import javax.mail.{AuthenticationFailedException, Flags, Message}
import scala.collection.mutable

object PhotoMoney extends App {
    override def main(args: Array[String]) {
        var paymentProvider: Option[PaymentProvider] = None
        val receivedEmails = new mutable.HashSet[Array[Byte]]()
        var replier: Option[SmtpReplier] = None

        val callbacks: List[(Message) => Unit] =
        // Main Message Processor
            (EmailMainProcessor.processMessage(_: Message, receivedEmails, paymentProvider, replier)) ::
                // Delete message after handling it
                ((msg: Message) => msg.setFlag(Flags.Flag.DELETED, true)) ::
                Nil

        // Cli/Gui Entry Point
        args.length match {
            case 7 =>
                def errorCallback(e:Throwable) {
                    print(e.getMessage)
                    sys.exit(1)
                }
                start(args(0), args(1), args(2), args(3), args(4), args(5), errorCallback)
            case _ =>
                println("Usage: <host> <email username/email> " +
                    "<email password> blockchain <blockchain token> <blockchain password>"
                )
        }

        def start(host: String, username: String, pass: String, prov: String, bit_token: String, bit_password: String, errorCallback: (Throwable => Unit)):Boolean = {
            try {
                paymentProvider = Some(new BlockchainPayments(bit_token, bit_password))
                paymentProvider.get.validateCredentials()
                replier = Some(new SmtpReplier(host, username, pass))
                new ImapListener(host, username, pass, expunge = true, callbacks = callbacks)
                return true
            } catch {
                // case e: MailConnectException => errorCallback(e)
                case e: AuthenticationFailedException => errorCallback(e)
                case e: BitcoinTokenError => errorCallback(e)
                case e: BitcoinPasswordError => errorCallback(e)
                case e: BitcoinConnectionError => errorCallback(e)
            }
            false
        }
    }
}
