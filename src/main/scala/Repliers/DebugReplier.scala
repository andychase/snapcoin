package Repliers

import java.awt.image.BufferedImage
import javax.mail.Address

import PhotoMoney.Wallet

class DebugReplier extends Replier {
    def sendMail(to: Address, wallet: Wallet, text: String, img: Option[BufferedImage]): Unit = {
        print(
            s"""
              | --------------
              | To: $to
              | From: ${wallet.toAddress}
              | Text: $text
              | --------------
            """.stripMargin)
    }

    def validateCredentials(): Boolean = true
}
