package PaymentProviders

class DebugProvider extends PaymentProvider {
    def sendPayment(walletID: String, password: String, address: String, amount: Long): Unit = {
        println(s"sendPayment: walletID: $walletID, password: $password, address: $address, amount: $amount")
    }

    def validateCredentials(): Boolean = true

    def createWallet(walletPassword: String): (String, String) = {
        print(s"createWallet: $walletPassword")
        ("", "")
    }
}
