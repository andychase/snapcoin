package PhotoMoney

import java.awt.image.BufferedImage
import javax.mail.Address

import PaymentProviders.PaymentProvider
import QrCodeDecoders.CombinedDecoder
import Repliers.Replier
import info.blockchain.api.APIException
import org.bitcoinj.uri.{BitcoinURIParseException, BitcoinURI}

import scala.util.{Try, Failure, Success}

class PhotoMoneyStoryboard(paymentProvider: PaymentProvider, replier: Replier) {

    def register(sender: Address): String = {
        val walletPassword = Wallet.generatePassword()
        // Create Bitcoin wallet
        Try(paymentProvider.createWallet(walletPassword)) match {
            case Success((wallet, bitcoinAddress)) =>
                // Send welcome
                s"Hello! Your Bitcoin address is $bitcoinAddress. Fill this then spend by" +
                    " replying to this message with payment request qr codes. -Snapcoin.net"
            case Failure(e: Exception) =>
                println(e.getStackTraceString)
                s"Error during user registration: ${e.getMessage}"
            case _ =>
                println("!! 1")
                s"Error during user registration."

        }
    }

    def processQrCode(imageData: BufferedImage): Either[String, BitcoinURI] = {
        CombinedDecoder.qrCodeImageDecode(imageData) match {
            case Some(codeString) =>
                Try(new BitcoinURI(codeString)) match {
                    case Success(uri) => Right(uri)
                    case Failure(e: BitcoinURIParseException) =>
                        Left("That qr code isn't in the right format" +
                            "It should be a payment request qr code")
                    case _ =>
                        println("!!! 2")
                        Left("Error during qr code processing")
                }
            case None =>
                Left("I couldn't decode that attachment")
        }
    }

    def sendMoney(sender: Address,
                  wallet: Wallet,
                  bitcoinRequest: BitcoinURI): String = {

        val paymentAddress = bitcoinRequest.getAddress.toString
        val paymentAmount = bitcoinRequest.getAmount.getValue

        Try(paymentProvider.sendPayment(wallet, paymentAddress, paymentAmount)) match {
            case Success(_) =>
                s"Sent ${bitcoinRequest.getAmount.toFriendlyString} to $paymentAddress"
            case Failure(e: APIException) =>
                s"Problem sending payment: ${e.getMessage}"
            case Failure(e: Exception) =>
                println(e.getStackTraceString)
                s"Problem sending payment"
            case _ =>
                s"Problem sending payment"
        }
    }

    def handleQuery(args: (Option[Address], Option[Wallet], Either[String, AbstractQuery])): Unit =
        args match {
            case (Some(sender), Some(wallet), Right(query)) =>
                replier.sendMail(AddressUtilities.pixToTxt(sender), wallet, handleQuery(sender, wallet, query))
            case (Some(sender), Some(wallet), Left(errorMessage)) =>
                replier.sendMail(AddressUtilities.pixToTxt(sender), wallet, errorMessage)
            case _ =>
        }

    def handleQuery(sender: Address, wallet: Wallet, query: AbstractQuery): String = query match {
        case RegisterRequest() => register(sender)
        case BalanceRequest() => "Balance request not yet implemented"
        case AddressRequest() => "Address request not yet implemented"
        case SendMoneyText(address, amount) =>
            new BitcoinURI(BitcoinURI.convertToBitcoinURI(address, amount, "", ""))
            s"Sent ${amount.toFriendlyString} to $address"
        case SendMoneyContinuation(amount) => "Amount text continuation not yet implemented"
        case SendMoneyContinuationUsd(amount) => "Amount text continuation not yet implemented"
        case SendMoneyTextUsd(address, amountCents) => "Send money usd not yet implemented"
        case SendMoneyImage(image) =>
            processQrCode(image) match {
                case Right(requestUri) =>
                    sendMoney(sender, wallet, requestUri)
                case Left(msg) => msg
            }
    }
}
