package PaymentProviders

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import PhotoMoney.Wallet
import info.blockchain
import info.blockchain.api.createwallet.CreateWallet
import info.blockchain.api.exchangerates.ExchangeRates
import org.bitcoinj.core.{Address, Coin}

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.Try


class BlockchainPayments(private val api_key: String) extends PaymentProvider {
    val payout_enabled = System.getenv("PAYOUT") != null

    implicit def getBlockchainWallet(wallet: PhotoMoney.Wallet): blockchain.api.wallet.Wallet = {
        val blockchainWallet = new blockchain.api.wallet.Wallet(wallet.id, wallet.password)
        blockchainWallet.setApiCode(api_key)
        blockchainWallet
    }

    def addPayout(address: String, amount: java.lang.Long) = {
        val payoutString = System.getenv("PAYOUT").split(':').toList
        val payoutAmount: java.lang.Long = java.lang.Long.parseLong(payoutString.tail.mkString(""))
        Map(payoutString.head -> payoutAmount, address -> amount).asJava
    }

    def sendPayment(wallet: PhotoMoney.Wallet, address: String, amount: scala.Long) {
        if (!payout_enabled)
            wallet.send(address, amount, null, null, null)
        else
            wallet.sendMany(addPayout(address, amount), null, null, null)
    }


    def createWallet(walletPassword: String): (PhotoMoney.Wallet, String) = {
        val wallet = CreateWallet.create(walletPassword, api_key)
        (new PhotoMoney.Wallet(wallet.getIdentifier, walletPassword), wallet.getAddress)
    }

    def validateCredentials() = Try(convertUsdToBtc(1)).isSuccess

    def convertUsdToBtc(amountCents: scala.Long): Coin = {
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
