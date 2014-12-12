package PhotoMoney

import java.util.UUID
import javax.mail.Address

import PaymentProviders.PaymentProvider
import Repliers.Replier
import info.blockchain.api.APIException
import org.bitcoinj.uri.BitcoinURI

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PhotoMoneyStoryboard {

    def register(sender: Address, paymentProvider: PaymentProvider, replier: Replier) {
        val walletPassword = UUID.randomUUID().toString.split('-').mkString("").substring(0, 12)
        // Create Bitcoin wallet
        Future {
            val (walletID, bitcoinAddress) = paymentProvider.createWallet(walletPassword)
            val wallet = new Wallet(walletID, walletPassword)

            // Send welcome
            val introMessage =
                s"Hello! Your Bitcoin address is $bitcoinAddress. Fill this then spend by" +
                " replying to this message with payment request qr codes. -Snapcoin.net"
            replier.sendMail(
                sender,
                wallet,
                introMessage
            )
        }
    }

    def sendMoney(sender: Address,
                  wallet: Wallet,
                  bitcoinRequest: BitcoinURI,
                  paymentProvider: PaymentProvider,
                  replier: Replier): Unit = {

        val paymentAddress = bitcoinRequest.getAddress.toString
        val paymentAmount = bitcoinRequest.getAmount.getValue
        val reply = replier.sendMail(AddressUtilities.pixToTxt(sender), wallet, _: String)

        Future {
            try {
                paymentProvider.sendPayment(wallet, paymentAddress, paymentAmount)
                reply(s"Sent ${bitcoinRequest.getAmount.toFriendlyString} to $paymentAddress")

            }
            catch {
                case e: APIException =>
                    reply(s"Problem sending payment: ${e.getMessage}")
            }
        }
    }
}
