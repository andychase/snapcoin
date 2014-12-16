package PaymentProviders

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import PhotoMoney.Wallet
import info.blockchain
import info.blockchain.api.createwallet.CreateWallet
import info.blockchain.api.exchangerates.ExchangeRates
import org.bitcoinj.core.{Address, Coin}

import scala.language.implicitConversions
import scala.util.Try


class BlockchainPayments(private val api_key: String) extends PaymentProvider {

    implicit def getBlockchainWallet(wallet:PhotoMoney.Wallet):blockchain.api.wallet.Wallet = {
        val blockchainWallet = new blockchain.api.wallet.Wallet(wallet.id, wallet.password)
        blockchainWallet.setApiCode(api_key)
        blockchainWallet
    }

    def sendPayment(wallet:PhotoMoney.Wallet, address: String, amount: Long) {
        wallet.send(address, amount, null, null, null)
    }


    def createWallet(walletPassword: String): (String, String) = {
        val wallet = CreateWallet.create(walletPassword, api_key)
        (wallet.getIdentifier, wallet.getAddress)
    }

    def validateCredentials() = Try(convertUsdToBtc(1)).isSuccess

    def convertUsdToBtc(amountCents:Long):Coin = {
        val cents = (BigDecimal(amountCents) / 100).toDouble
        Coin.parseCoin(ExchangeRates.toBTC("USD", cents, api_key).toString)
    }

    def getBalance(wallet: Wallet): Coin = {
        Coin.valueOf(wallet.getBalance)
    }

    def getAddress(wallet: Wallet): Address = {
        val timeZone = TimeZone.getTimeZone("UTC")
        val isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
        isoDateFormat.setTimeZone(timeZone)
        val nowAsISO = isoDateFormat.format(new Date())
        val addressString = wallet.newAddress(s"Created from snapcoin.net on $nowAsISO").getAddress
        new Address(null, addressString)
    }
}
