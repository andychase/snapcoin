package PhotoMoney

import java.net.URI
import java.util.concurrent.Executors

import PaymentProviders.{BlockchainPayments, DebugProvider}
import Repliers.{DebugReplier, MailgunReplier}
import TemporaryStorage.{RedisStorage, DebugStorage}
import akka.actor.ActorSystem
import spray.http.{StatusCodes, FormData, MultipartContent}
import spray.routing.SimpleRoutingApp

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Server extends App with SimpleRoutingApp {
    implicit val system = ActorSystem("photomoney-system")
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

    System.loadLibrary("zbarjni")

    def setup(): Storyboard = {
        var offlineMode = false
        val (blockchain_api, email_post_url, email_user, email_password, redis_url) = {
            val e = { key: String => System.getenv(key: String) match {
                case s: String => s
                case null =>
                    offlineMode = true
                    println(s"Missing environment variable $key")
                    ""
            }
            }
            (e("BLOCKCHAIN_API_KEY"),
                e("MAILGUN_POST_URL"),
                e("MAILGUN_POST_USER"),
                e("MAILGUN_API_KEY"),
                new URI(e("REDISCLOUD_URL")))
        }
        if (offlineMode) {
            println("Missing environment variables running in offline mode")
            new Storyboard(new DebugProvider(), new DebugReplier(), new DebugStorage())
        } else {
            val _paymentProvider = new BlockchainPayments(blockchain_api)
            _paymentProvider.validateCredentials()
            val _replier = new MailgunReplier(email_post_url, email_user, email_password)
            if (!_replier.validateCredentials()) {
                throw new Exception("Can't validate replier")
            }
            val _storage = new RedisStorage(redis_url)
            if (!_storage.validateCredentials()) {
                throw new Exception("Can't validate storage")
            }
            new Storyboard(_paymentProvider, _replier, _storage)
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
                } ~ entity(as[FormData]) { emailData =>
                    Future(photoMoneyStoryboard.handleQuery(MessageProcessor.processEmail(emailData)))
                    complete("OK")
                }
            }
        } ~
            path("register") {
                post {
                    entity(as[FormData]) { formData =>
                        onComplete(Future(photoMoneyStoryboard.registerForm(formData))) {
                            case Success(Some(qr)) =>
                                redirect(
                                    s"https://snapcoin.net/register_thanks.html?qr=$qr",
                                    StatusCodes.SeeOther)
                            case Success(None) |
                                 Failure(_: Throwable) =>
                                println("Error signing up")
                                redirect(
                                    s"https://snapcoin.net/register_thanks.html?err=",
                                    StatusCodes.SeeOther)
                        }
                    }
                }
            }
    }
}