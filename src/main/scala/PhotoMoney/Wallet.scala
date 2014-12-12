package PhotoMoney

import javax.mail.Address
import javax.mail.internet.InternetAddress

class Wallet(val id: String, val password: String) {
    def toAddress: Address = {
        new InternetAddress(s"wallet+$id|$password@p.snapcoin.net")
    }
}

object Wallet {
    def addressToWallet(address: Address): Option[(Wallet)] = {
        val addressString = address.toString
        val plusIndex = addressString.indexOf('+')
        val pipeIndex = addressString.indexOf('|')
        val atIndex = addressString.indexOf('@')
        if (plusIndex > 0 && pipeIndex > 0 && atIndex > pipeIndex && pipeIndex > plusIndex) {
            val walletPassword = addressString.substring(plusIndex+1, pipeIndex)
            val walletID = addressString.substring(pipeIndex+1, atIndex)
            Some(new Wallet(walletID, walletPassword))
        } else {
            None
        }
    }
}
