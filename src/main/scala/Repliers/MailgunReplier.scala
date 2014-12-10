package Repliers

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.LinkedBlockingQueue
import javax.imageio.ImageIO
import javax.mail.Address

import PhotoMoney.Wallet
import com.mashape.unirest.http.async.Callback
import com.mashape.unirest.http.exceptions.UnirestException
import com.mashape.unirest.http.{HttpResponse, Unirest}


/**
 * Non-blocking email sender
 */
class MailgunReplier(postUrl: String, username: String, password: String) extends Replier {
    private val sendingQueue = new LinkedBlockingQueue[(Address, Wallet, String, Option[BufferedImage])]()
    new Thread(new Sender(sendingQueue), "SenderKeepAlive").start()

    def sendMail(to: Address, wallet: Wallet, text: String, image: Option[BufferedImage]) {
        print(text)
        sendingQueue.add((to, wallet, text, image))
    }

    def post(to: Address, wallet: Wallet, text: String, maybeImage: Option[BufferedImage]): Unit = {
        val req = Unirest.post(postUrl)
            .header("accept", "application/json")
            .field("to", to)
            .field("to", wallet.toAddress)
        for (image <- maybeImage)
            req.field("inline", img2jpg(image))

        req.basicAuth(username, password)
            .asStringAsync(
                new Callback[String]() {
                    def failed(e: UnirestException) {
                        println("Request failed")
                        sendingQueue.put(to, wallet, text, maybeImage)
                    }

                    def completed(response: HttpResponse[String]) {}

                    def cancelled() {
                        failed(null)
                    }
                })
    }


    def img2jpg(image: BufferedImage): Array[Byte] = {
        val baos = new ByteArrayOutputStream()
        ImageIO.write(image, "jpg", baos)
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
}
