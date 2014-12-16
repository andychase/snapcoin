package PhotoMoney

import java.awt.image.BufferedImage

import org.bitcoinj.core.{Address, Coin}

abstract class AbstractQuery

case class RegisterRequest() extends AbstractQuery
case class BalanceRequest() extends AbstractQuery
case class AddressRequest() extends AbstractQuery
case class SendMoneyText(address:Address, amount:Coin) extends AbstractQuery
case class SendMoneyContinuation(amount:Coin) extends AbstractQuery
case class SendMoneyContinuationUsd(amountCents:Long) extends AbstractQuery
case class SendMoneyTextUsd(address:Address, amountCents:Long) extends AbstractQuery
case class SendMoneyImage(image:BufferedImage) extends AbstractQuery
