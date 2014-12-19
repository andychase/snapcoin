package Repliers

import java.awt.image.BufferedImage
import javax.mail.Address

class DebugReplier extends Replier {
    def sendMail(to: Address, from: Address, text: String, img: Option[BufferedImage]): Unit = {
        print(
            s"""
              | --------------
              | To: $to
              | From: $from
              | Text: $text
              | --------------
            """.stripMargin)
    }

    def validateCredentials(): Boolean = true
}
