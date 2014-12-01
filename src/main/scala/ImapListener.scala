import java.util.Properties

import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.protocol.IMAPProtocol
import javax.mail._

/**
 * This class implements an imap listener,
 * that performs callbacks on every message
 * as they come in.
 */
class ImapListener(
                      val server: String,
                      val username: String,
                      val password: String,
                      val expunge: Boolean = false,
                      val imapIdleSessionLength: Long = 300000,
                      val callbacks: List[(Message) => Unit]) {

    val props = new Properties()
    val session = Session.getInstance(props)
    val store = session.getStore("imaps")
    store.connect(server, username, password)

    val folder: IMAPFolder = store.getFolder("inbox").asInstanceOf[IMAPFolder]
    folder.open(Folder.READ_WRITE)

    new Thread(new InterruptRunnable(folder), "InterruptConnectionKeepAlive").start()

    /**
     * Runnable used to keep alive the connection to the IMAP server
     * @author Juan Martín Sotuyo Dodero <jmsotuyo@monits.com>
     */
    private class KeepAliveRunnable(private val folder: IMAPFolder) extends Runnable {
        def run() {
            while (!Thread.interrupted) {
                Thread.sleep(imapIdleSessionLength)
                folder.doCommand(new IMAPFolder.ProtocolCommand {
                    def doCommand(p: IMAPProtocol): AnyRef = {
                        p.simpleCommand("NOOP", null)
                        null
                    }
                })

            }
        }
    }

    private class InterruptRunnable(private val folder: IMAPFolder) extends Runnable {
        def run() {
            val thread: Thread = new Thread(new KeepAliveRunnable(folder), "IdleConnectionKeepAlive")
            thread.start()
            while (!Thread.interrupted) {
                val messages = folder.getMessages
                for (message <- messages;
                     callback <- callbacks) {
                    callback(message)
                }

                if (messages.length > 0 && expunge) {
                    folder.expunge(messages)
                }
                folder.idle(true)
                print("~")
            }

            if (thread.isAlive)
                thread.interrupt()
        }

    }

}
