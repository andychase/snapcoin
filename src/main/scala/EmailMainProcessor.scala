import com.google.zxing.common.HybridBinarizer
import com.google.zxing._
import com.sun.mail.util.BASE64DecoderStream
import java.net.URLDecoder
import java.security.MessageDigest
import java.util
import java.util.Collections
import javax.imageio.ImageIO
import javax.mail.{Address, Message, Header, BodyPart}
import javax.mail.internet.MimeMultipart
import scala.collection.mutable
import scala.Predef._
import scala.util.{Failure, Success, Try}

object EmailMainProcessor {
    val digest = MessageDigest.getInstance("SHA-256")

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


    def buildReplier(fromAddresses: Array[Address], replierMaybe: Option[SmtpReplier]) = {
        message: String => {
            for (from <- fromAddresses;
                 replier <- replierMaybe)
                replier.sendMail(from, message)
        }
    }


    def processMessage(msg: Message, receivedEmails: mutable.HashSet[Array[Byte]], paymentProviderMaybe: Option[PaymentProvider], replierMaybe: Option[SmtpReplier]) {
        val reply = buildReplier(msg.getFrom, replierMaybe)
        val paymentProvider = paymentProviderMaybe.get

        msg.getContent match {
            // If email message has an attachment
            case multipart: MimeMultipart =>
                for (part <- new emailMultiPartIterator(multipart)) {
                    part.getContent match {
                        // Part of email that is a binary attachment
                        case decoderStream: BASE64DecoderStream =>
                            // Check a hash of headers to avoid repeat attacks
                            val messageHash = digest.digest(headersToBytes(msg.getAllHeaders))
                            if (receivedEmails contains messageHash) {
                                // Message was already received, don't process
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
                                                    Try(amount.toFloat) match {
                                                        case Success(amountFloat) =>
                                                            paymentProvider.sendPayment(new String(address), amountFloat, reply(_:String))
                                                        case Failure(_) =>
                                                            reply("I wasn't able to parse the amount: %s.".format(amount))
                                                    }

                                                case None =>
                                                    println("The bitcoin qr code didn't include an amount to send.")
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
    }
}
