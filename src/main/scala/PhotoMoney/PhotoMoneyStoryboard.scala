package PhotoMoney

import javax.mail.Address

import PaymentProviders.PaymentProvider
import Repliers.Replier
import info.blockchain.api.APIException
import org.bitcoinj.uri.BitcoinURI

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object PhotoMoneyStoryboard {

    def register(sender: Address, paymentProvider: PaymentProvider, replier: Replier) {
        val walletPassword = Wallet.generatePassword()
        Future {
            // Create Bitcoin wallet
            val (walletID, bitcoinAddress) = paymentProvider.createWallet(walletPassword)
            (new Wallet(walletID, walletPassword), bitcoinAddress)
        } onComplete {
            case Success((wallet, bitcoinAddress)) =>
                // Send welcome
                val introMessage =
                    s"Hello! Your Bitcoin address is $bitcoinAddress. Fill this then spend by" +
                        " replying to this message with payment request qr codes. -Snapcoin.net"
                replier.sendMail(
                    sender,
                    wallet,
                    introMessage
                )
            case Failure(e: APIException) =>
                println(s"Error during user registration: ${e.getMessage}")
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
            paymentProvider.sendPayment(wallet, paymentAddress, paymentAmount)
        } onComplete {
            case Success(_) =>
                reply(s"Sent ${bitcoinRequest.getAmount.toFriendlyString} to $paymentAddress")
            case Failure(e: APIException) =>
                reply(s"Problem sending payment: ${e.getMessage}")
            case Failure(_) =>
        }

    }
}
