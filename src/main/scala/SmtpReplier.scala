import com.sun.mail.smtp.SMTPMessage
import java.util.concurrent.LinkedBlockingQueue
import java.util.Properties
import javax.mail.internet.InternetAddress
import javax.mail.{Address, Session, Message}

/**
 * Non-blocking email sender
 */
class SmtpReplier(host: String, username: String, password: String) {
    private val sendingQueue = new LinkedBlockingQueue[(Address, String)]()
    new Thread(new Sender(sendingQueue), "SenderKeepAlive").start()

    def sendMail(to: Address, text:String) {
        print(text)
        sendingQueue.add((to, text))
    }

    private class Sender(private val queue: LinkedBlockingQueue[(Address, String)]) extends Runnable {
        val props = new Properties()
        props.setProperty("mail.smtps.host", host)
        val session = Session.getInstance(props, null)
        val transport = session.getTransport("smtps")
        transport.connect(username, password)

        def run() {
            while(true) {
                val (to:Address, text:String) = queue.take()
                val message = new SMTPMessage(session)
                message.setText(text)
                message.setFrom(new InternetAddress(username))
                message.setRecipient(Message.RecipientType.TO, PhoneTranslation.CheckAndTranslate(to))
                // Final check to make sure the from and to aren't the same
                // Otherwise the bot would talk to itself forever
                if (new InternetAddress(username) != to)
                    transport.sendMessage(message, message.getAllRecipients)
            }
        }
    }
}
