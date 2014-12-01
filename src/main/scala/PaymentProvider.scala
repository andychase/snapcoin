
trait PaymentProvider {
    def sendPayment(walletID:String, password:String, address:String, amount:Long)

    def validateCredentials()

    def createWallet(secretKey:String):(String, String)
}
