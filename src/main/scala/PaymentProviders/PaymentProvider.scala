package PaymentProviders

trait PaymentProvider {
    def sendPayment(walletID: String, password: String, address: String, amount: Long)

    def validateCredentials(): Boolean

    def createWallet(walletPassword: String): (String, String)
}
