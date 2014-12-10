package PaymentProviders

import PhotoMoney.Wallet

class DebugProvider extends PaymentProvider {
    def sendPayment(wallet:Wallet, address: String, amount: Long): Unit = {
        println(s"sendPayment: walletID: ${wallet.id}, password: ${wallet.password}, address: $address, amount: $amount")
    }

    def validateCredentials(): Boolean = true

    def createWallet(walletPassword: String): (String, String) = {
        print(s"createWallet: $walletPassword")
        ("", "")
    }
}
