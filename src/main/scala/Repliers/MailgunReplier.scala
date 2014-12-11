package Repliers

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.LinkedBlockingQueue
import javax.imageio.ImageIO
import javax.mail.Address

import PhotoMoney.Wallet
import com.ning.http.client.ByteArrayPart
import com.ning.http.multipart.StringPart
import dispatch.{Http, url}
import scala.concurrent.Await
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global


class MailgunReplier(postUrl: String, username: String, password: String) extends Replier {
    private val sendingQueue = new LinkedBlockingQueue[(Address, Wallet, String, Option[BufferedImage])]()
    new Thread(new Sender(sendingQueue), "SenderKeepAlive").start()

    def sendMail(to: Address, wallet: Wallet, text: String, image: Option[BufferedImage]) {
        sendingQueue.add((to, wallet, text, image))
    }

    def post(to: Address, wallet: Wallet, text: String, maybeImage: Option[BufferedImage]): Unit = {

        val img = img2jpg(maybeImage.get)
        val request = url(postUrl + "/messages").as_!(username, password)
            .setMethod("POST")
            .addBodyPart(new StringPart("to", to.toString))
            .addBodyPart(new StringPart("from", wallet.toAddress.toString))
            .addBodyPart(new StringPart("text", text.toString))
            .addBodyPart(new ByteArrayPart("attachment", "qrCodeImage.png", img, "image/png", "UTF-8"))

        for (r <- Http(request))
            println(r.getResponseBody)
    }


    def img2jpg(image: BufferedImage): Array[Byte] = {
        val baos = new ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        baos.flush()
        val imageInByte = baos.toByteArray
        baos.close()
        imageInByte
    }

    private class Sender(private val queue: LinkedBlockingQueue[(Address, Wallet, String, Option[BufferedImage])]) extends Runnable {
        def run() {
            while (true) {
                val obj = queue.take()
                val (to: Address, wallet: Wallet, text: String, image: Option[BufferedImage]) = obj
                post(to, wallet, text, image)
            }
        }
    }

    def validateCredentials(): Boolean = {
        val request = url(postUrl + "/log?limit=1").as_!(username, password)
        val waitDuration = 10.seconds
        Await.result(Http(request), waitDuration).getStatusCode == 200
    }
}
