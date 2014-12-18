package TemporaryStorage

import PhotoMoney.Wallet
import org.apache.commons.codec.digest.DigestUtils
import org.bitcoinj.core.Address

trait TemporaryStorage {
    val salt = "bottle series basket count"
    val expireTimeSeconds: Int = 300

    def hash(input: Wallet): String = DigestUtils.sha256Hex(input.id.toString + salt)

    def putAddress(wallet: Wallet, address: Address) = put(hash(wallet), address.toString)

    def getAddress(wallet: Wallet): Option[Address] = get(hash(wallet)) map (new Address(null, _))

    def validateCredentials(): Boolean = {
        val wallet = new Wallet("FAKEWALLET", "password")
        val address = new Address(null, "1Archive1n2C579dMsAu3iC6tWzuQJz8dN")
        putAddress(wallet, address)
        getAddress(wallet).get.toString == address.toString
    }

    protected def put(key: String, value: String): Unit

    protected def get(key: String): Option[String]
}
