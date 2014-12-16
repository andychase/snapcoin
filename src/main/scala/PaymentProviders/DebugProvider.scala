package PaymentProviders

import PhotoMoney.Wallet
import org.bitcoinj.core.{Coin, Address}

class DebugProvider extends PaymentProvider {
    val walletAddress = "1Archive1n2C579dMsAu3iC6tWzuQJz8dN"

    def sendPayment(wallet:Wallet, address: String, amount: Long): Unit = {
        println(s"sendPayment: walletID: ${wallet.id}, password: ${wallet.password}, address: $address, amount: $amount")
    }

    def validateCredentials(): Boolean = true

    def createWallet(walletPassword: String): (String, String) = {
        print(s"createWallet: $walletPassword")
        ("FAKEWALLET", walletAddress)
    }

    def getBalance(wallet: Wallet): Coin = Coin.ZERO

    def getAddress(wallet: Wallet): Address = new Address(null, walletAddress)

    def convertUsdToBtc(UsdCents: Long): Coin = Coin.ZERO
}
