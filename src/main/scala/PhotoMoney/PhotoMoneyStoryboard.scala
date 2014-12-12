package PhotoMoney

import java.util.UUID
import javax.mail.Address

import PaymentProviders.PaymentProvider
import QrCodeDecoders._
import Repliers.Replier
import org.bitcoinj.uri.BitcoinURI

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

    def sendMoney(sender: Address,
                  wallet:Wallet,
                  bitcoinRequest: BitcoinURI,
                  paymentProvider: PaymentProvider,
                  replier: Replier): Unit = {

        val paymentAddress = bitcoinRequest.getAddress.toString
        val paymentAmount = bitcoinRequest.getAmount.getValue
        paymentProvider.sendPayment(wallet, paymentAddress, paymentAmount)
        replier.sendMail(
            AddressUtilities.pixToTxt(sender),
            wallet,
            s"Sent ${bitcoinRequest.getAmount.toFriendlyString} to $paymentAddress",
            None
        )
    }
}
