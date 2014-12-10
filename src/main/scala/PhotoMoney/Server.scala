package PhotoMoney

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import PaymentProviders.{BlockchainPayments, PaymentProvider}
import QrCodeDecoders.ZbarDecoder
import Repliers.SmtpReplier
import akka.actor.ActorSystem
import spray.http.HttpData.NonEmpty
import spray.http.{BodyPart, MultipartContent}
import spray.routing.SimpleRoutingApp

object Server extends App with SimpleRoutingApp {
    implicit val system = ActorSystem("photomoney-system")
    System.loadLibrary("zbarjni")

    def setup(): (PaymentProvider, SmtpReplier, String) = {
        val (blockchain_api, smtp_host, email_user, email_password, port) = {
            val e = { key: String => System.getenv(key: String) match {
                case s: String => s
                case null => throw new Exception(s"Missing environment variable $key")
            }
            }
            (e("BLOCKCHAIN_API_KEY"), e("SMTP_HOST"), e("EMAIL_USER"), e("EMAIL_PASSWORD"), e("PORT"))
        }

        val _paymentProvider = new BlockchainPayments(blockchain_api)
        _paymentProvider.validateCredentials()
        val _replier = new SmtpReplier(smtp_host, email_user, email_password)
        (_paymentProvider, _replier, Option(port) getOrElse "5000")
    }

    val (paymentProvider, replier, port) = setup()

    def dataToBufferedImage(data: Array[Byte]): Option[BufferedImage] = {
        try {
            Some(ImageIO.read(new ByteArrayInputStream(data)))
        } catch {
            case e: Exception =>
                None
        }

    }

    def isEmailAbleToProcess(dataInPartsMap: Map[String, BodyPart]): Boolean = {
        val in = dataInPartsMap.contains _
        in("attachment-count") && in("recipient") && in("sender") &&
            (dataInPartsMap("attachment-count").entity.data.asString.toInt > 0)
    }

    def getPartsOfEmail(dataInPartsMap: Map[String, BodyPart]): (String, String, BodyPart) = {
        val get = dataInPartsMap(_: String).entity.data.asString
        (get("recipient"), get("sender"), dataInPartsMap("attachment-1"))
    }

    def getDataInPartsMap(emailData: MultipartContent): Map[String, BodyPart] = {
        val dataInParts = emailData.parts.seq map {
            emailPart => emailPart.name.get -> emailPart
        }
        Map(dataInParts: _*)
    }

    startServer(interface = "0.0.0.0", port = Integer.parseInt(port)) {
        path("msg") {
            post {
                entity(as[MultipartContent]) { emailData =>
                    val dataInPartsMap = getDataInPartsMap(emailData)

                    if (isEmailAbleToProcess(dataInPartsMap)) {
                        val (recipient, sender, attachment) = getPartsOfEmail(dataInPartsMap)

                        val qrCode = attachment.entity.data match {
                            case data: NonEmpty => dataToBufferedImage(data.toByteArray) match {
                                case Some(imageData) => ZbarDecoder.qrCodeImageDecode(imageData)
                                case None => ""
                            }
                            case _ => ""
                        }
                        println(s"s: $sender, r: $recipient, a:$qrCode")
                    }
                    complete("OK")
                }
            }
        }
    }


    def processEmailMessage(sender: String, recipient: String, image: BufferedImage): Unit = {

    }
}