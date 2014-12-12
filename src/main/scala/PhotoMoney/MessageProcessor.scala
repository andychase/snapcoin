package PhotoMoney

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.mail.Address
import javax.mail.internet.InternetAddress

import PaymentProviders.PaymentProvider
import QrCodeDecoders.CombinedDecoder
import Repliers.Replier
import org.bitcoinj.uri.{BitcoinURIParseException, BitcoinURI}
import spray.http.HttpData.NonEmpty
import spray.http.{FormData, BodyPart, MultipartContent}

object MessageProcessor {
    type EmailData = (Option[Wallet], Address, Option[BodyPart])

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

        (recipient, new InternetAddress(getIfPossible("sender")), getAttachmentIfPossible(dataInPartsMap))
    }

    def getDataInPartsMap(emailData: MultipartContent): Map[String, BodyPart] = {
        val dataInParts = emailData.parts.seq map {
            emailPart => emailPart.name.get -> emailPart
        }
        Map(dataInParts: _*)
    }

    def processEmail(emailData: MultipartContent, paymentProvider: PaymentProvider, replier: Replier): Unit = {
        processEmail(sortEmail(getDataInPartsMap(emailData)), paymentProvider, replier)
    }

    def processEmail(emailData: FormData, paymentProvider: PaymentProvider, replier: Replier): Unit = {
        var recipient: Option[Wallet] = None
        var sender: InternetAddress = null
        emailData.fields foreach {
            case ("recipient", _addr) => recipient = Wallet.addressToWallet(new InternetAddress(_addr))
            case ("sender", _sender) => sender = new InternetAddress(_sender)
            case _ =>
        }
        processEmail((recipient, sender, None), paymentProvider, replier)
    }

    def processEmail(emailData: EmailData, paymentProvider: PaymentProvider, replier: Replier): Unit = {
        emailData match {
            case (None, sender, _) =>
                PhotoMoneyStoryboard.register(sender, paymentProvider, replier)

            case (Some(wallet), sender, Some(attachment)) =>
                attachment.entity.data match {
                    case data: NonEmpty => dataToBufferedImage(data.toByteArray) match {
                        case Some(imageData) =>
                            CombinedDecoder.qrCodeImageDecode(imageData) match {
                                case Some(codeString) => try {
                                    PhotoMoneyStoryboard.sendMoney(
                                        sender,
                                        wallet, new BitcoinURI(codeString),
                                        paymentProvider, replier
                                    )
                                } catch {
                                    case e: BitcoinURIParseException =>
                                    // Could not parse qr code
                                }
                                case None =>
                                // Could not decode image
                            }

                        case None =>
                        // Did not find attachment
                    }
                    case _ =>
                }
            case (Some(wallet), sender, None) =>
                // Did not send image
                replier.sendMail(sender, wallet, "You did not attach a qr code!")
            case _ =>
            // No sender can't do anything
        }
    }
}
