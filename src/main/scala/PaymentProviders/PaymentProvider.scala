package PaymentProviders

import PhotoMoney.Wallet
import org.bitcoinj.core.{Address, Coin}

trait PaymentProvider {
    def sendPayment(wallet:Wallet, address: String, amount: Long)

    def validateCredentials(): Boolean

    def createWallet(walletPassword: String): (String, String)

    def getBalance(wallet:Wallet):Coin

    def getAddress(wallet:Wallet):Address

    def convertUsdToBtc(UsdCents:Long):Coin
}
