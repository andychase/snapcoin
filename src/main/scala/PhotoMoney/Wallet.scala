package PhotoMoney

import java.security.SecureRandom
import javax.mail.Address
import javax.mail.internet.InternetAddress

import org.apache.commons.codec.binary.{Base32, Hex}

class Wallet(val id: String, val password: String) {
    def toAddress: Address = {
        val compressedAddress = Wallet.compress(id, password)
        new InternetAddress(compressedAddress+"@snc.io")
    }
}

object Wallet {
    val base32 = new Base32

    def addressToWallet(address: Address): Option[(Wallet)] = {
        val addressString = address.toString
        val atIndex = addressString.indexOf('@')
        if (atIndex > 0) {
            val compressedWalletString = addressString.substring(0, atIndex)
            if (compressedWalletString.length == 32) {
                val (walletID, walletPassword) = decompress(compressedWalletString)
                return Some(new Wallet(walletID, walletPassword))
            }
        }
        None
    }

    def generatePassword(): String = {
        val random = new SecureRandom()
        val bytes = new Array[Byte](4)
        random.nextBytes(bytes)
        // The underscores are to meet 10 character password minimum
        // 32 bits of entropy ought to be enough for a password
        new String(Hex.encodeHex(bytes)) + "__"
    }

    def compress(walletId: String, walletPassword:String): String = {
        val strippedString = walletId.split('-').mkString("")+walletPassword.substring(0, 8)
        base32.encodeToString(Hex.decodeHex(strippedString.toCharArray)).toLowerCase
    }

    def decompress(inputString:String): (String, String) = {
        val out = new String(Hex.encodeHex(base32.decode(inputString.toUpperCase)))
        val (idRaw, password) = out.splitAt(32)
        val p = idRaw.substring(_:Int, _:Int)
        val id = (p(0, 8) :: p(8, 12) :: p(12, 16) :: p(16, 20) :: idRaw.substring(20) :: Nil).mkString("-")
        (id, password+"__")
    }
}
