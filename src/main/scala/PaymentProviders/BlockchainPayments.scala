package PaymentProviders

import info.blockchain.api.createwallet.CreateWallet
import info.blockchain.api.exchangerates.ExchangeRates
import info.blockchain


class BlockchainPayments(private val api_key: String) extends PaymentProvider {

    def sendPayment(wallet:PhotoMoney.Wallet, address: String, amount: Long) {
        val blockchainWallet = new blockchain.api.wallet.Wallet(wallet.id, wallet.password)
        blockchainWallet.send(address, amount, null, null, null)
    }


    def createWallet(walletPassword: String): (String, String) = {
        val wallet = CreateWallet.create(walletPassword, api_key)
        (wallet.getIdentifier, wallet.getAddress)
    }

    def validateCredentials() = {
        ExchangeRates.toBTC("USD", 1L, api_key)
        true
    }

}
