package PaymentProviders

import PhotoMoney.Wallet

trait PaymentProvider {
    def sendPayment(wallet:Wallet, address: String, amount: Long)

    def validateCredentials(): Boolean

    def createWallet(walletPassword: String): (String, String)
}
