package Repliers

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.mail.Address

import com.ning.http.client.ByteArrayPart
import com.ning.http.multipart.StringPart
import dispatch.{Http, url}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


class MailgunReplier(postUrl: String, username: String, password: String) extends Replier {
    def sendMail(to: Address, from: Address, text: String, maybeImage: Option[BufferedImage]) {

        val request = url(postUrl + "/messages").as_!(username, password)
            .setMethod("POST")
            .addBodyPart(new StringPart("to", to.toString))
            .addBodyPart(new StringPart("from", from.toString))
            .addBodyPart(new StringPart("text", text.toString))

        for (img <- maybeImage)
            request.addBodyPart(new ByteArrayPart("attachment", "qrCodeImage.png", img2png(img), "image/png", "UTF-8"))

        for (r <- Http(request))
            println(r.getResponseBody.replace('\n', ' '))
    }


    def img2png(image: BufferedImage): Array[Byte] = {
        val output = new ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        output.flush()
        val imageInByte = output.toByteArray
        output.close()
        imageInByte
    }

    def validateCredentials(): Boolean = {
        val request = url(postUrl + "/log?limit=1").as_!(username, password)
        val waitDuration = 10.seconds
        Await.result(Http(request), waitDuration).getStatusCode == 200
    }
}
