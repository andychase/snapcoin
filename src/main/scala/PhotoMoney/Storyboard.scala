package PhotoMoney

import java.awt.image.BufferedImage

import PaymentProviders.PaymentProvider
import QrCodeDecoders.CombinedDecoder
import Repliers.Replier
import TemporaryStorage.TemporaryStorage
import com.google.i18n.phonenumbers.PhoneNumberUtil
import info.blockchain.api.APIException
import org.bitcoinj.core.Coin
import org.bitcoinj.uri.{BitcoinURI, BitcoinURIParseException}
import spray.http.FormData

import scala.util.{Failure, Success, Try}

class Storyboard(paymentProvider: PaymentProvider, replier: Replier, storage: TemporaryStorage) {
    type BitcoinAddress = org.bitcoinj.core.Address
    type EmailAddress = javax.mail.Address


    def register(sender: EmailAddress): Option[String] = {
        val walletPassword = Wallet.generatePassword()
        // Create Bitcoin wallet
        Try(paymentProvider.createWallet(walletPassword)) match {
            case Success((wallet, bitcoinAddress)) =>
                // Send welcome
                replier.sendMail(sender, wallet, Words.welcome(wallet, bitcoinAddress))
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
                Words.sendMoneySuccess(amount, address)
            case Failure(e: APIException) =>
                Words.sendMoneyApiFailure(e)
            case Failure(e: Throwable) =>
                println(e.getStackTrace)
                Words.sendMoneyOtherFailure()
        }
    }

    def getBalance(wallet: Wallet): String = {
        Words.balance(paymentProvider.getBalance(wallet).toFriendlyString)
    }

    def getAddress(wallet: Wallet): String = {
         Words.address(paymentProvider.getAddress(wallet))
    }

    def processQrCode(imageData: BufferedImage): Either[String, BitcoinURI] = {
        CombinedDecoder.qrCodeImageDecode(imageData) match {
            case Some(codeString) =>
                Try(new BitcoinURI(codeString)) match {
                    case Success(uri) => Right(uri)
                    case Failure(e: BitcoinURIParseException) =>
                        Left(Words.wrongFormat())
                    case _ =>
                        Left(Words.errorProcessing())
                }
            case None =>
                Left(Words.errorDecodingAttachment())
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
            Words.help()
        case BalanceRequest() => getBalance(wallet)
        case AddressRequest() => getAddress(wallet)
        case SendMoneyText(address, amount) =>
            sendMoney(wallet, address, amount)
        case SendMoneyContinuation(amount) => storage.getAddress(wallet) match {
            case Some(address) =>
                handleQuery(sender, wallet, SendMoneyText(address, amount))
            case None =>
                Words.none()
        }
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
                        storage.putAddress(wallet, address)
                        Words.howMuchToSpend(address)
                    case (address, amount) =>
                        sendMoney(wallet, requestUri.getAddress, requestUri.getAmount)
                }
                case Left(msg) => msg
            }
        case SendMoneyImageWithAmount(amount, image) => processQrCode(image) match {
            case Right(requestUri) =>
                sendMoney(wallet, requestUri.getAddress, amount)
            case Left(msg) => msg
        }
    }
}
