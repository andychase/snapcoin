class BitcoinTokenError(msg:String) extends Throwable(msg) {}
class BitcoinPasswordError(msg:String) extends Throwable(msg) {}
class BitcoinConnectionError(msg:String) extends Throwable(msg) {}

trait PaymentProvider {
    def sendPayment(address:String, amount:Float, reply:(String=>Unit))
    def validateCredentials()

    def tokenError(msg:String = "") {
        throw new BitcoinTokenError(msg)
    }
    def passwordError(msg:String = "") {
        throw new BitcoinPasswordError(msg)
    }
}
