import javax.mail.Message.RecipientType

import com.google.zxing.common.HybridBinarizer
import com.google.zxing._
import com.sun.mail.util.BASE64DecoderStream
import java.net.URLDecoder
import java.security.MessageDigest
import java.util
import java.util.Collections
import javax.imageio.ImageIO
import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMultipart}
import info.blockchain.api.APIException

import scala.collection.mutable
import scala.Predef._
import scala.util.{Failure, Success, Try}
import org.bitcoinj.utils.MonetaryFormat

object EmailMainProcessor {
    val digest = MessageDigest.getInstance("SHA-256")
    val monetaryFormatParser = new MonetaryFormat()

    def qrCodeImageDecode(decoderStream: BASE64DecoderStream): String = {
        // Build hints via Java Collection
        val hints = new util.HashMap[DecodeHintType, Any]()
        hints.put(DecodeHintType.TRY_HARDER, true)
        val formats = new util.ArrayList[BarcodeFormat]()
        formats.add(BarcodeFormat.QR_CODE)
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats)

        val image = ImageIO.read(decoderStream)
        val source = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))
        try {
            new MultiFormatReader().decode(source, hints).getText
        } catch {
            case _: NotFoundException => "Not Found"
        }
    }

    class emailMultiPartIterator(multipart: MimeMultipart) extends Iterator[BodyPart] {
        override val length = multipart.getCount
        var current = 0

        def hasNext: Boolean = current < length

        def next() = {
            current += 1
            multipart.getBodyPart(current - 1)
        }
    }

    def headersToBytes(headers: util.Enumeration[_]): Array[Byte] =
        Collections.list(headers)
            .toArray
            .map({
            i: AnyRef => i.asInstanceOf[Header].getName ++ i.asInstanceOf[Header].getValue
        })
            .flatMap({
            i: String => i.getBytes("UTF-8")
        })


    def parseUri(query: String) =
        (query.split("&") map {
            pair =>
                val idx = pair.indexOf("=")
                URLDecoder.decode(pair.substring(0, idx), "UTF-8") -> URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
        } filter {
            item => item.isInstanceOf[(String, String)]
        }).toMap


    def buildReplier(fromAddresses: Array[Address], extension: String, replierMaybe: Option[SmtpReplier]) = {
        message: String => {
            for (from <- fromAddresses;
                 replier <- replierMaybe)
                replier.sendMail(from, extension, message)
        }
    }

    def parseFrom(from: String) = {
        val plusIndex = from.indexOf('+') + 1
        val atIndex = from.indexOf('@')
        if (atIndex == -1 || plusIndex == -1 || plusIndex > atIndex) {
            ""
        } else {
            from.substring(plusIndex, atIndex)
        }
    }

    type sendFunctionType = (String, String, Long) => Unit

    def getSecretIdOrRegister(address: String, photoMoneyServer: PhotoMoneyServer): (String, Option[String]) = {
        val secretId = parseFrom(address)
        val registered = photoMoneyServer.checkRegistered(secretId)
        if (registered) {
            (secretId, None)
        } else {
            val (secretId, bitcoinAddress) = photoMoneyServer.register()
            (secretId, Some(bitcoinAddress))
        }
    }


    def processMessage(msg: Message, receivedEmails: mutable.HashSet[Array[Byte]], photoMoneyServer: PhotoMoneyServer, replierMaybe: Option[SmtpReplier]) {
        try {
            val emailAddress = new InternetAddress(msg.getRecipients(RecipientType.TO).head.toString).getAddress
            val (secretId, bitcoinAddress) = getSecretIdOrRegister(emailAddress, photoMoneyServer)
            val reply = { m: String => println(m)} //buildReplier(msg.getFrom, secretId, replierMaybe)
            for (address <- bitcoinAddress) {
                reply(address)
                // Should stop processing now
                return
            }


            var gotImage = false
            msg.getContent match {
                // If email message has an attachment
                case multipart: MimeMultipart =>
                    for (part <- new emailMultiPartIterator(multipart)) {
                        part.getContent match {
                            // Part of email that is a binary attachment
                            case decoderStream: BASE64DecoderStream =>
                                gotImage = true
                                // Check a hash of headers to avoid repeat attacks
                                val messageHash = digest.digest(headersToBytes(msg.getAllHeaders))
                                if (receivedEmails contains messageHash) {
                                    // Message was already received, don't process
                                    reply("You've sent me that image the past. For security I need a new image each time.")
                                    return
                                } else {
                                    receivedEmails += messageHash
                                }

                                // Attempt to decode it
                                qrCodeImageDecode(decoderStream).split(":", 2).toList match {
                                    case "Not Found" :: Nil =>
                                        // No qr code was able to be found in attachment
                                        reply("I wasn't able to decode a qr code in that image." ++
                                            "Try getting a cleaner picture of it. " ++
                                            "(Be sure to fully capture the white edges around the code.)")
                                    case "bitcoin" :: addressFull :: Nil =>
                                        addressFull.split("\\?", 2).toList match {
                                            // No amount
                                            case address :: Nil =>
                                                println("The bitcoin qr code didn't include an amount to send.")
                                            // Address with amount
                                            case address :: uri :: Nil =>
                                                parseUri(uri).get("amount") match {
                                                    case Some(amount: String) =>
                                                        Try(monetaryFormatParser.parse(amount).getValue) match {
                                                            case Success(amountFloat) =>
                                                                photoMoneyServer.sendMoney(secretId, new String(address), amountFloat)
                                                            case Failure(_) =>
                                                                reply("I wasn't able to parse the amount: %s.".format(amount))
                                                        }

                                                    case None =>
                                                        reply("The bitcoin qr code didn't include an amount to send.")
                                                }
                                            case _ => // <- Not possible
                                        }
                                    // Qr code was found, but didn't contain a bitcoin:1234 type address
                                    case _ =>
                                        reply("I found a qr Code, but it wasn't in the correct format.")
                                }
                            // Text Part of email with attachment
                            case _ =>
                        }
                    }
                // No attachment in email message
                case _ =>
                    reply("I couldn't find a picture attachment in that message.")
            }
            if (!gotImage) {
                reply("I couldn't find a picture attachment in that message.")
            }
        } catch {
            case _: MessageRemovedException =>
            case e: APIException => println(e.getMessage)
        }
    }
}
