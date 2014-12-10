package Repliers

import javax.mail.Address

class DebugReplier extends Replier {
    def sendMail(to: Address, extension: String, text: String): Unit = {
        print(
            s"""
              | --------------
              | To: $to
              | From: ${getFromAddress(extension)}
              | Text: $text
              | --------------
            """.stripMargin)
    }
}
