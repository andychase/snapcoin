package PhotoMoney

import PaymentProviders.{PaymentProvider, BlockchainPayments, DebugProvider}
import Repliers.{MailgunReplier, DebugReplier, Replier}
import akka.actor.ActorSystem
import spray.http.{FormData, MultipartContent}
import spray.routing.SimpleRoutingApp

object Server extends App with SimpleRoutingApp {
    implicit val system = ActorSystem("photomoney-system")
    System.loadLibrary("zbarjni")

    def setup(): (PaymentProvider, Replier) = {
        var offlineMode = false
        val (blockchain_api, email_post_url, email_user, email_password) = {
            val e = { key: String => System.getenv(key: String) match {
                case s: String => s
                case null =>
                    offlineMode = true
                    println(s"Missing environment variable $key")
                    ""
            }
            }
            (e("BLOCKCHAIN_API_KEY"), e("MAILGUN_POST_URL"), e("MAILGUN_POST_USER"), e("MAILGUN_API_KEY"))
        }
        if (offlineMode) {
            println("Missing environment variables running in offline mode")
            (new DebugProvider(), new DebugReplier())
        } else {
            val _paymentProvider = new BlockchainPayments(blockchain_api)
            _paymentProvider.validateCredentials()
            val _replier = new MailgunReplier(email_post_url, email_user, email_password)
            if (!_replier.validateCredentials()) {
                throw new Exception("Can't validate replier")
            }
            (_paymentProvider, _replier)
        }
    }

    val (paymentProvider, replier) = setup()
    val port = Option(System.getenv("PORT")) getOrElse "50031"

    startServer(interface = "0.0.0.0", port = Integer.parseInt(port)) {
        path("msg") {
            post {
                entity(as[MultipartContent]) { emailData =>
                    MessageProcessor.processEmail(emailData, paymentProvider, replier)
                    complete("OK")
                }~ entity(as[FormData]) { emailData =>
                    MessageProcessor.processEmail(emailData, paymentProvider, replier)
                    complete("OK")
                }
            }
        }
    }
}