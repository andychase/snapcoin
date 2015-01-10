package PhotoMoney

import info.blockchain.api.APIException
import org.bitcoinj.core.{Address, Coin}

object Words {
  def welcome(wallet: Wallet, bitcoinAddress: String) =
    s"Snapcoin! SAVE/send replies: ${wallet.toAddress}. 'help' for cmds. Addr: $bitcoinAddress"

  def howMuchToSpend(address: Address) =
    s"How much do you want to send to $address ? Reply with: [amount] [unit]."

  def sendMoneySuccess(amount: Coin, address: Address) =
    s"Sent ${amount.toFriendlyString} to $address"

  def sendMoneyApiFailure(error: APIException) =
    s"Problem sending payment: ${error.getMessage}"

  def sendMoneyOtherFailure() =
    s"Problem sending payment"

  def balance(friendlyBalance: String) =
    s"Your balance is: $friendlyBalance"

  def address(address: Address) =
    s"Here's a Bitcoin address for your account: $address"

  def help() =
    "Snapcoin.net! Commands: [balance]/[address]/send [amount] [unit] [address]. Or send a QR Code!"

  def none() =
    "You sent [amount] [unit] but no address." +
      "If you sent a qr code with an address more then 5 minutes ago it may have expired."

  def wrongFormat() = "That qr code isn't in the right format. It should be a payment request qr code."

  def errorProcessing() = "Error during qr code processing"

  def errorDecodingAttachment() = "I couldn't decode that attachment"
}
