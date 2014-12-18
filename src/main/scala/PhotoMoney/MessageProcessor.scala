package PhotoMoney

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.mail.Address
import javax.mail.internet.InternetAddress
import spray.http.HttpData.NonEmpty
import spray.http.{BodyPart, FormData, MultipartContent}
import QueryUnderstand.QueryUnderstand

object MessageProcessor {
    type EmailData = (Option[Wallet], Address, Option[String], Option[BodyPart])
    type MailResponse = (Option[Address], Option[Wallet], Either[String, AbstractQuery])

    def dataToBufferedImage(data: Array[Byte]): Option[BufferedImage] = {
        try {
            Option(ImageIO.read(new ByteArrayInputStream(data)))
        } catch {
            case e: Exception =>
                println(s"Exception reading image. ${e.getCause}")
                None
        }

    }

    def getAttachmentIfPossible(dataInPartsMap: Map[String, BodyPart]): Option[BodyPart] = {
        if (dataInPartsMap.contains("attachment-count") &&
            (dataInPartsMap("attachment-count").entity.data.asString.toInt > 0))
            Some(dataInPartsMap("attachment-1"))
        else None
    }

    def sortEmail(dataInPartsMap: Map[String, BodyPart]): EmailData = {
        val emailHas = dataInPartsMap.contains _
        val get = dataInPartsMap(_: String).entity.data.asString
        val getIfPossible = { s: String => if (emailHas(s)) get(s) else ""}
        val recipient = Wallet.addressToWallet(new InternetAddress(getIfPossible("recipient")))

        (recipient,
            new InternetAddress(getIfPossible("sender")),
            Option(getIfPossible("body-plain")),
            getAttachmentIfPossible(dataInPartsMap))
    }

    def getDataInPartsMap(emailData: MultipartContent): Map[String, BodyPart] = {
        val dataInParts = emailData.parts.seq map {
            emailPart => emailPart.name.get -> emailPart
        }
        Map(dataInParts: _*)
    }

    def processEmail(emailData: MultipartContent): MailResponse = {
        processEmailEnvelope(sortEmail(getDataInPartsMap(emailData)))
    }

    def processEmail(emailData: FormData): MailResponse = {
        var recipient: Option[Wallet] = None
        var sender: InternetAddress = null
        var bodyText: Option[String] = None
        emailData.fields foreach {
            case ("recipient", _addr) => recipient = Wallet.addressToWallet(new InternetAddress(_addr))
            case ("sender", _sender) => sender = new InternetAddress(_sender)
            case ("body-plain", _body_plain) => bodyText = Some(_body_plain)
            case _ =>
        }
        processEmailEnvelope(recipient, sender, bodyText, None)
    }

    def processEmailEnvelope(emailData: EmailData): MailResponse = {
        emailData match {
            case (wallet, null, _, _) =>
                (None, None, Left("No sender"))
            case (Some(wallet), sender, _, Some(attachment)) =>
                (Some(sender), Some(wallet), processEmailAttachment(attachment))
            case (Some(wallet), sender, Some(text), None) if !text.trim.isEmpty =>
                (Some(sender), Some(wallet), QueryUnderstand.decodeQuery(text))
            case (None, sender, Some(text), None) =>
                (Some(sender), None, Right(RegisterRequest()))
        }
    }

    def processEmailAttachment(attachment: BodyPart): Either[String, AbstractQuery] =
        attachment.entity.data match {
            case data: NonEmpty => dataToBufferedImage(data.toByteArray) match {
                case Some(imageData) =>
                    Right(SendMoneyImage(imageData))
                case None =>
                    // Could not
                    Left("I could not find any qr code attached")
            }
            case _ =>
                Left("I could not find any qr code attached")
        }
}
