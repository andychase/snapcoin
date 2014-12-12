package PhotoMoney

import java.util.UUID
import javax.mail.Address

import PaymentProviders.PaymentProvider
import QrCodeDecoders._
import Repliers.Replier

object PhotoMoneyStoryboard {

    def register(sender: Address, paymentProvider: PaymentProvider, replier: Replier) {
        val walletPassword = UUID.randomUUID().toString
        // Create Bitcoin wallet
        val (walletID, bitcoinAddress) = paymentProvider.createWallet(walletPassword)
        val wallet = new Wallet(walletID, walletPassword)

        // Send welcome
        val qrcodeImage = ZxingDecoder.encode("bitcoin:" + bitcoinAddress)
        val introMessage = "Welcome!"
        replier.sendMail(
            AddressUtilities.txtToPix(sender),
            wallet,
            introMessage,
            Some(qrcodeImage)
        )
    }

    def sendMoney(wallet:Wallet, bitcoinAddress: String, amount: Long, paymentProvider: PaymentProvider, replier: Replier): Unit = {
        paymentProvider.sendPayment(wallet, bitcoinAddress, amount)
    }
}
