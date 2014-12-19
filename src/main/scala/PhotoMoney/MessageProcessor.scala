package PhotoMoney

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.mail.Address
import javax.mail.internet.InternetAddress
import QueryUnderstand.QueryUnderstand
import spray.http.HttpEntity.{Empty, NonEmpty}
import spray.http._

import scala.util.Try

object MessageProcessor {
    type MailResponse = (Option[Address], Option[Wallet], Either[String, AbstractQuery])

    def dataToBufferedImage(data: Array[Byte]): Option[BufferedImage] =
        Try(ImageIO.read(new ByteArrayInputStream(data))).toOption

    def processEmail(emailData: MultipartContent): MailResponse = {
        var wallet: Option[Wallet] = None
        var sender: Option[Address] = None
        var bodyText: String = ""
        var maybeImage: Option[BufferedImage] = None

        val attachmentPattern = "attachment-([0-9]+)".r
        val asStr = (_: BodyPart).entity.asString(HttpCharsets.`UTF-8`)

        for {emailPart <- emailData.parts; name <- emailPart.name} (name, emailPart) match {
            case ("recipient", address) =>
                wallet = Wallet.addressToWallet(new InternetAddress(asStr(address)))
            case ("sender", senderStr) =>
                sender = Try(new InternetAddress(asStr(senderStr))).toOption
            case ("body-plain", bodyPlain) => bodyText += asStr(bodyPlain)
            case (attachmentPattern(_), BodyPart(entity, _)) => entity match {
                case NonEmpty(ContentTypes.`text/plain`, data) =>
                    bodyText += data.asString(HttpCharsets.`UTF-8`)
                    bodyText = bodyText.trim
                case NonEmpty(_, data) =>
                    maybeImage = dataToBufferedImage(data.toByteArray)
                case Empty =>
            }
            case _ =>
        }

        val query: Either[String, AbstractQuery] = (bodyText, maybeImage) match {
            case (text, None) =>
                QueryUnderstand.decodeQuery(text)
            case ("", Some(image)) =>
                Right(SendMoneyImage(image))
            case (text, Some(image)) =>
                QueryUnderstand.decodeQuery(text) match {
                    case Right(SendMoneyContinuation(amount)) =>
                        Right(SendMoneyImageWithAmount(amount, image))
                    case _ =>
                        Right(SendMoneyImage(image))
                }
        }

        (sender, wallet, query)
    }

    def processEmail(emailData: FormData): MailResponse = {
        var wallet: Option[Wallet] = None
        var sender: Option[Address] = None
        var bodyText: String = ""
        emailData.fields foreach {
            case ("recipient", address) => wallet = Wallet.addressToWallet(new InternetAddress(address))
            case ("sender", senderStr) => sender = Try(new InternetAddress(senderStr)).toOption
            case ("body-plain", bodyPlain) => bodyText = bodyPlain
            case _ =>
        }
        (sender, wallet, QueryUnderstand.decodeQuery(bodyText))
    }
}
