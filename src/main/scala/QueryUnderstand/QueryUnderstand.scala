package QueryUnderstand

import java.text.ParseException

import PhotoMoney._
import org.apache.commons.lang3.StringUtils
import org.bitcoinj.core._
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.utils.BtcFormat

import scala.util.{Try, Success, Failure}
import scala.util.matching.Regex

object QueryUnderstand {
    val oneSpaceCommands =
        "addr" :: "address" :: "balance" :: "bal" :: "help" :: Nil

    val bitcoinAddressRegex = new Regex("[13][a-km-zA-HJ-NP-Z0-9]{26,33}")
    val numberRegex = new Regex("[0-9]+([\\.|,][0-9]*)*")
    val unitRegex = new Regex("(btc|bitcoin|ubtc|µbtc|microcoin|mbtc|millicoin|satoshi|sat|usd|\\$|¢)")

    val microcoinFormatter = BtcFormat.getInstance(BtcFormat.MICROCOIN_SCALE)
    val millicoinFormatter = BtcFormat.getInstance(BtcFormat.MILLICOIN_SCALE)
    val satoshiFormatter = BtcFormat.getInstance(8)

    def decodeSendRequest(query: String): Either[String, AbstractQuery] =
        parseSendRequestQuery(query) match {
            case (None, None, None) =>
                Left("I don't understand that command. Attach image or text 'address', 'balance', or 'send [amount] [unit] [address]'.")
            case (None, Some(amount), None) =>
                Left("I got an amount but no unit. send [amount] [unit] [address]")
            case (None, None, Some(_)) =>
                Left("I got units but no amount or address. send [amount] [unit] [address]")
            case (Some(_), None, Some(_)) =>
                Left("I got an address and units but no amount. send [amount] [unit] [address]")
            case (Some(address), None, None) =>
                Left("I got an address but not an amount for sending. send [amount] [unit] [address]")
            case (None, Some(amount), Some(unit)) =>
                parseAmountUnit(amount, unit) match {
                    case error: String =>
                        Left(error)
                    case amountUsd: Long =>
                        Right(SendMoneyContinuationUsd(amountUsd))
                    case amountCoin: Coin =>
                        Right(SendMoneyContinuation(amountCoin))
                }
            case (Some(address), Some(amount), None) =>
                Try(new Address(new MainNetParams(), address)) match {
                    case Success(_) =>
                        Left("I got a valid address and an amount for sending, but no unit. Add: [btc/ubtc/mbtc/satoshi].")
                    case Failure(_: AddressFormatException) |
                         Failure(_: WrongNetworkException) =>
                        Left("That address is not valid. Also include a unit! Add: [btc/ubtc/mbtc/satoshi].")
                    case Failure(e: Throwable) => println(e.getStackTraceString); Left("Internal Error")
                }
            case (Some(address), Some(amount), Some(unit)) =>
                Try(new Address(new MainNetParams(), address)) match {
                    case Success(a: Address) =>
                        parseAmountUnit(amount, unit) match {
                            case error: String =>
                                Left(error)
                            case amountUsd: Long =>
                                Right(SendMoneyTextUsd(a, amountUsd))
                            case amountCoin: Coin =>
                                Right(SendMoneyText(a, amountCoin))
                        }
                    case Failure(_: AddressFormatException) |
                         Failure(_: WrongNetworkException) =>
                        Left("That recipient address is not valid. Check for typos.")
                    case Failure(e: Throwable) => println(e.getStackTraceString); Left("Internal Error")
                }
        }


    def parseSendRequestQuery(query: String): (Option[String], Option[String], Option[String]) = {
        for ((address, queryNoAddress) <- finderReplacer(query, bitcoinAddressRegex)) {
            for ((amount, queryNoAddressOrAmount) <- finderReplacer(queryNoAddress, numberRegex)) {
                for ((unit, _) <- finderReplacer(queryNoAddressOrAmount, unitRegex))
                    return (Some(address), Some(amount), Some(unit))
                return (Some(address), Some(amount), None)
            }
            return (Some(address), None, None)
        }
        for ((amount, queryNoAddressOrAmount) <- finderReplacer(query, numberRegex)) {
            for ((unit, _) <- finderReplacer(queryNoAddressOrAmount, unitRegex))
                return (None, Some(amount), Some(unit))
            return (None, Some(amount), None)
        }
        (None, None, None)
    }

    def finderReplacer(query: String, regex: Regex): (Option[(String, String)]) = {
        regex.findFirstIn(query) match {
            case Some(result) => Some(result, regex.replaceFirstIn(query, ""))
            case None => None
        }
    }

    def parseUsd(amount: String, unit: String) =
        if (("usd" :: "$" :: Nil) contains unit.toLowerCase)
            (BigDecimal(amount) * 100).toBigIntExact()
        else
            BigDecimal(amount).toBigIntExact()


    def parseWithUnit(amount: String, unit: String): Coin = unit.toLowerCase match {
        case "btc" | "bitcoin" => Coin.parseCoin(amount)
        case "ubtc" | "µbtc" | "microcoin" => microcoinFormatter.parse(amount)
        case "mbtc" | "millicoin" => millicoinFormatter.parse(amount)
        case "s" | "satoshi" | "sat" => satoshiFormatter.parse(amount)
        case _ => Coin.parseCoin(amount)
    }

    def parseAmountUnit(amount: String, unit: String) = {
        if (("usd" :: "$" :: "¢" :: Nil) contains unit.toLowerCase)
            parseUsd(amount, unit) match {
                case Some(amountParsed) =>
                    amountParsed.toLong
                case None =>
                    "I couldn't understand that USD value"
            }
        else
            Try(parseWithUnit(amount, unit)) match {
                case Success(amountParsed) =>
                    amountParsed
                case Failure(e: ParseException) =>
                    "I couldn't understand that Bitcoin unit or value"
                case Failure(e: Throwable) => println(e.getStackTraceString); "Internal Error"
            }
    }

    def getClosest(inputCommand: String): String = {
        val distanceCommandMap =
            for (testCommand <- oneSpaceCommands)
            yield StringUtils.getLevenshteinDistance(inputCommand, testCommand, 3) -> testCommand
        val withoutErrors = distanceCommandMap.filter(_._1 != -1)
        if (withoutErrors.nonEmpty)
            withoutErrors.min._2
        else
            ""
    }

    def decodeSingleCommandQuery(command: String): Either[String, AbstractQuery] = command match {
        case "?" | "help" => Right(HelpRequest())
        case "address" | "addr" => Right(AddressRequest())
        case "balance" | "bal" => Right(BalanceRequest())
        case "register" => Right(RegisterRequest())
        case _ => Left("I don't understand that command. Attach image or text 'address', 'balance', or 'send [amount] [unit] [address]'.")
    }

    def decodeQuery(query: String): Either[String, AbstractQuery] = {
        val trimmedQuery = query.trim()
        val newlineIndex = trimmedQuery.indexOf('\n')
        val splitQuery =
            if (newlineIndex > 0)
                trimmedQuery.substring(0, newlineIndex).trim.split(' ').toList
            else
                query.trim.split(' ').toList

        splitQuery.length match {
            case _ if splitQuery.head.isEmpty =>
                Left("Attach image or text: 'address', 'balance', or 'send [amount] [unit] [address]'.")
            case 1 =>
                val cmd = splitQuery.head.toLowerCase
                if (cmd == "register")
                    Right(RegisterRequest())
                else if (cmd == "?")
                    Right(HelpRequest())
                else if (oneSpaceCommands.contains(cmd))
                    decodeSingleCommandQuery(cmd)
                else
                    decodeSingleCommandQuery(getClosest(cmd))
            case _ =>
                decodeSendRequest(query)
        }
    }
}
