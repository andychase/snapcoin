package PhotoMoney

import java.util.concurrent.Executors

import PaymentProviders.{BlockchainPayments, DebugProvider}
import Repliers.{DebugReplier, MailgunReplier}
import akka.actor.ActorSystem
import spray.http.{FormData, MultipartContent}
import spray.routing.SimpleRoutingApp

import scala.concurrent.{ExecutionContext, Future}

object Server extends App with SimpleRoutingApp {
    implicit val system = ActorSystem("photomoney-system")
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

    System.loadLibrary("zbarjni")

    def setup(): PhotoMoneyStoryboard = {
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
            new PhotoMoneyStoryboard(new DebugProvider(), new DebugReplier())
        } else {
            val _paymentProvider = new BlockchainPayments(blockchain_api)
            _paymentProvider.validateCredentials()
            val _replier = new MailgunReplier(email_post_url, email_user, email_password)
            if (!_replier.validateCredentials()) {
                throw new Exception("Can't validate replier")
            }
            new PhotoMoneyStoryboard(_paymentProvider, _replier)
        }
    }

    val photoMoneyStoryboard = setup()
    val port = Option(System.getenv("PORT")) getOrElse "50031"

    startServer(interface = "0.0.0.0", port = Integer.parseInt(port)) {
        path("msg") {
            post {
                entity(as[MultipartContent]) { emailData =>
                    Future(photoMoneyStoryboard.handleQuery(MessageProcessor.processEmail(emailData)))
                    complete("OK")
                }~ entity(as[FormData]) { emailData =>
                    Future(photoMoneyStoryboard.handleQuery(MessageProcessor.processEmail(emailData)))
                    complete("OK")
                }
            }
        }
    }
}