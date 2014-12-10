package PhotoMoney

import java.util.UUID

import PaymentProviders.PaymentProvider
import QrCodeDecoders._
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

class PhotoMoneyStoryboard(paymentProvider: PaymentProvider) {

    def register(): (String, String, String) = {
        val walletPassword = UUID.randomUUID().toString
        // Create Bitcoin wallet
        val (walletID, bitcoinAddress) = paymentProvider.createWallet(walletPassword)
        // Save walletID
        (walletID, walletPassword, bitcoinAddress)
    }

    def introMessage(phoneNumber: PhoneNumber, carrier: String, bitcoinAddress: String) = {
        val qrcodeImage = ZxingDecoder.encode("bitcoin:"+bitcoinAddress)
        val introMessage = ""
        (qrcodeImage, introMessage)
    }

    def sendMoney(walletId: String, walletPassword:String, bitcoinAddress: String, amount: Long): Unit = {
        paymentProvider.sendPayment(walletId, walletPassword, bitcoinAddress, amount)
    }
}
