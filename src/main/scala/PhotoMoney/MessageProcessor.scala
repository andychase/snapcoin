package PhotoMoney

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.mail.internet.InternetAddress

import PaymentProviders.PaymentProvider
import QrCodeDecoders.CombinedDecoder
import Repliers.Replier
import org.bitcoinj.uri.{BitcoinURIParseException, BitcoinURI}
import spray.http.HttpData.NonEmpty
import spray.http.{BodyPart, MultipartContent}

object MessageProcessor {
    def dataToBufferedImage(data: Array[Byte]): Option[BufferedImage] = {
        try {
            Some(ImageIO.read(new ByteArrayInputStream(data)))
        } catch {
            case e: Exception =>
                None
        }

    }

    def getAttachmentIfPossible(dataInPartsMap: Map[String, BodyPart]): Option[BodyPart] = {
        if (dataInPartsMap.contains("attachment-count") &&
            (dataInPartsMap("attachment-count").entity.data.asString.toInt > 0))
            Some(dataInPartsMap("attachment-count"))
        else None
    }

    def sortEmail(dataInPartsMap: Map[String, BodyPart]): (Option[Wallet], String, Option[BodyPart]) = {
        val emailHas = dataInPartsMap.contains _
        val get = dataInPartsMap(_: String).entity.data.asString
        val getIfPossible = { s: String => if (emailHas(s)) get(s) else ""}
        val recipient = Wallet.addressToWallet(new InternetAddress(getIfPossible("recipient")))

        (recipient, getIfPossible("sender"), getAttachmentIfPossible(dataInPartsMap))
    }

    def getDataInPartsMap(emailData: MultipartContent): Map[String, BodyPart] = {
        val dataInParts = emailData.parts.seq map {
            emailPart => emailPart.name.get -> emailPart
        }
        Map(dataInParts: _*)
    }

    def processEmail(emailData: MultipartContent, paymentProvider: PaymentProvider, replier: Replier): Unit = {
        sortEmail(getDataInPartsMap(emailData)) match {
            case (None, sender, _) =>
                PhotoMoneyStoryboard.register(sender, paymentProvider, replier)

            case (Some(wallet), sender, Some(attachment)) =>
                attachment.entity.data match {
                    case data: NonEmpty => dataToBufferedImage(data.toByteArray) match {
                        case Some(imageData) =>
                            CombinedDecoder.qrCodeImageDecode(imageData) match {
                                case Some(codeString) => try {
                                    val bitcoinRequest = new BitcoinURI(codeString)
                                    val paymentAddress = bitcoinRequest.getAddress.toString
                                    val paymentAmount = bitcoinRequest.getAmount.getValue
                                    paymentProvider.sendPayment(wallet, paymentAddress, paymentAmount)
                                    replier.sendMail(
                                        AddressUtilities.pixToTxt(new InternetAddress(sender)),
                                        wallet,
                                        s"Sent $paymentAmount to $paymentAddress",
                                        None
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
        }
    }
}
