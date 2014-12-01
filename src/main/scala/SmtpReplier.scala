import com.sun.mail.smtp.SMTPMessage
import java.util.concurrent.LinkedBlockingQueue
import java.util.Properties
import javax.mail.internet.InternetAddress
import javax.mail._

/**
 * Non-blocking email sender
 */
class SmtpReplier(host: String, username: String, password: String) {
    private val sendingQueue = new LinkedBlockingQueue[(Address, String, String)]()
    new Thread(new Sender(sendingQueue), "SenderKeepAlive").start()

    def sendMail(to: Address, extension: String, text: String) {
        print(text)
        sendingQueue.add((to, extension, text))
    }

    private class Sender(private val queue: LinkedBlockingQueue[(Address, String, String)]) extends Runnable {
        val props = new Properties()
        props.put("mail.smtps.host", host)
        props.put("mail.smtp.auth", "true")
        props.put("mail.smtp.starttls.enable", "true")
        props.put("mail.smtp.port", "587")
        val session = Session.getInstance(props,
            new javax.mail.Authenticator() {
                override def getPasswordAuthentication = {
                    new PasswordAuthentication(username, password)
                }
            })

        def run() {
            while (true) {
                val (to: Address, extension: String, text: String) = queue.take()
                val message = new SMTPMessage(session)
                message.setText(text)
                message.setFrom(new InternetAddress(s"snapcoinmail+$extension@gmail.com"))
                message.setRecipient(Message.RecipientType.TO, PhoneTranslation.CheckAndTranslate(to))
                // Final check to make sure the from and to aren't the same
                // Otherwise the bot would talk to itself forever
                if (new InternetAddress(username) != to)
                    Transport.send(message, message.getAllRecipients)
            }
        }
    }

}
