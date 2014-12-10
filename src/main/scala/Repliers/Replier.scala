package Repliers

import javax.mail.Address
import javax.mail.internet.InternetAddress

trait Replier {
    def sendMail(to: Address, extension: String, text: String)

    def getFromAddress(extension: String): Address = {
        new InternetAddress(s"$extension@p.snapcoin.net")
    }
}
