package PaymentProviders

import PhotoMoney.Wallet
import org.bitcoinj.core.{Coin, Address}

class DebugProvider extends PaymentProvider {
    val walletAddress = "1Archive1n2C579dMsAu3iC6tWzuQJz8dN"

    def sendPayment(wallet:Wallet, address: String, amount: Long): Unit = {
        println(s"sendPayment: walletID: ${wallet.id}, password: ${wallet.password}, address: $address, amount: $amount")
    }

    def validateCredentials(): Boolean = true

    def createWallet(walletPassword: String): (Wallet, String) = {
        println(s"createWallet: $walletPassword")
        (new Wallet("FAFEFAFFE1", walletPassword), walletAddress)
    }

    def getBalance(wallet: Wallet): Coin = Coin.ZERO

    def getAddress(wallet: Wallet): Address = new Address(null, walletAddress)

    def convertUsdToBtc(UsdCents: Long): Coin = Coin.ZERO
}
