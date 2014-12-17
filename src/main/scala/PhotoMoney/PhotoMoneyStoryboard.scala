package PhotoMoney

import java.awt.image.BufferedImage

import PaymentProviders.PaymentProvider
import QrCodeDecoders.CombinedDecoder
import Repliers.Replier
import com.google.i18n.phonenumbers.PhoneNumberUtil
import info.blockchain.api.APIException
import org.bitcoinj.core.Coin
import org.bitcoinj.uri.{BitcoinURI, BitcoinURIParseException}
import spray.http.FormData

import scala.util.{Failure, Success, Try}

class PhotoMoneyStoryboard(paymentProvider: PaymentProvider, replier: Replier) {
    type BitcoinAddress = org.bitcoinj.core.Address
    type EmailAddress = javax.mail.Address


    def register(sender: EmailAddress): Option[String] = {
        val walletPassword = Wallet.generatePassword()
        // Create Bitcoin wallet
        Try(paymentProvider.createWallet(walletPassword)) match {
            case Success((wallet, bitcoinAddress)) =>
                // Send welcome
                replier.sendMail(sender, wallet,
                    s"Snapcoin.net! SAVE THIS CONTACT. Address: $bitcoinAddress. Fill this then reply with qr codes to spend.")
                Some(bitcoinAddress)
            case Failure(e: Throwable) =>
                println(e.getStackTraceString)
                None
        }
    }

    def registerForm(formData: FormData): Option[String] = {
        var phoneNumberMaybe: Option[String] = None
        var carrierMaybe: Option[String] = None
        formData.fields foreach {
            case ("phone_number", _phoneNumber) => phoneNumberMaybe = Some(_phoneNumber)
            case ("carrier", _carrier) => carrierMaybe = Some(_carrier)
            case _ =>
        }

        for (phoneNumberString <- phoneNumberMaybe;
             carrier <- carrierMaybe;
             phoneNumber <- Try(PhoneNumberUtil.getInstance().parse(phoneNumberString, "US")).toOption;
             emailAddress <- AddressUtilities.numberToEmail(phoneNumber, carrier);
             bitcoinAddress <- register(emailAddress))
            return Some(bitcoinAddress)
        None
    }

    def sendMoney(wallet: Wallet, address: BitcoinAddress, amount: Coin): String = {
        Try(paymentProvider.sendPayment(wallet, address.toString, amount.getValue)) match {
            case Success(_) =>
                s"Sent ${amount.toFriendlyString} to $address"
            case Failure(e: APIException) =>
                s"Problem sending payment: ${e.getMessage}"
            case Failure(e: Throwable) =>
                println(e.getStackTrace)
                s"Problem sending payment"
        }
    }

    def getBalance(wallet: Wallet): String = {
        "Your balance is: " + paymentProvider.getBalance(wallet).toFriendlyString
    }

    def getAddress(wallet: Wallet): String = {
        "Here's a Bitcoin address for your account: " + paymentProvider.getAddress(wallet)
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

    def handleQuery(args: (Option[EmailAddress], Option[Wallet], Either[String, AbstractQuery])): Unit =
        args match {
            case (Some(sender), Some(wallet), Right(query)) =>
                replier.sendMail(AddressUtilities.pixToTxt(sender), wallet, handleQuery(sender, wallet, query))
            case (Some(sender), Some(wallet), Left(errorMessage)) =>
                replier.sendMail(AddressUtilities.pixToTxt(sender), wallet, errorMessage)
            case (Some(sender), None, Right(RegisterRequest())) =>
                register(sender)
            case _ =>
        }

    def handleQuery(sender: EmailAddress, wallet: Wallet, query: AbstractQuery): String = query match {
        case RegisterRequest() => ""
        case HelpRequest() =>
            "Snapcoin.net! Commands: [balance]/[address]/send [amount] [unit] [address]. Or send a QR Code!"
        case BalanceRequest() => getBalance(wallet)
        case AddressRequest() => getAddress(wallet)
        case SendMoneyText(address, amount) =>
            sendMoney(wallet, address, amount)
        case SendMoneyContinuation(amount) => "Amount text continuation not yet implemented"
        case SendMoneyContinuationUsd(amountCents) =>
            val amount = paymentProvider.convertUsdToBtc(amountCents)
            handleQuery(sender, wallet, SendMoneyContinuation(amount))
        case SendMoneyTextUsd(address, amountCents) =>
            val amount = paymentProvider.convertUsdToBtc(amountCents)
            handleQuery(sender, wallet, SendMoneyText(address, amount))
        case SendMoneyImage(image) =>
            processQrCode(image) match {
                case Right(requestUri) => (requestUri.getAddress, requestUri.getAmount) match {
                    case (address, null) =>
                        "Amount text continuation not yet implemented"
                    case (address, amount) =>
                        sendMoney(wallet, requestUri.getAddress, requestUri.getAmount)
                }
                case Left(msg) => msg
            }
    }
}
