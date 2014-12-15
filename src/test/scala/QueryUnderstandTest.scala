import PhotoMoney.{SendMoneyTextUsd, SendMoneyText, AddressRequest, BalanceRequest}
import org.bitcoinj.core.{Coin, Address}
import org.bitcoinj.params.MainNetParams
import org.specs2.mutable._
import QueryUnderstand.QueryUnderstand

class QueryUnderstandTest extends Specification with org.specs2.mutable.Tables {
    "QueryUnderstand" should {
        val addr = "1Archive1n2C579dMsAu3iC6tWzuQJz8dN"
        val address = new Address(new MainNetParams(), addr)

        "understand these valid bal/addr commands" in {
            "Command" | "Expected Result" |
                "bal         " ! BalanceRequest() |
                "b           " ! BalanceRequest() |
                "balance     " ! BalanceRequest() |
                "address     " ! AddressRequest() |
                "ad          " ! AddressRequest() |
                " Address!   " ! AddressRequest() |
                "addr        " ! AddressRequest() |> { (command, result) =>
                QueryUnderstand.decodeQuery(command) must beRight(result)
            }
        }

        "understand these valid send commands" in {
            "Command" | "Expected Result" |
                s" send 1 ubtc $addr    " ! SendMoneyText(address, Coin.MICROCOIN) |
                s" send  1ubtc $addr    " ! SendMoneyText(address, Coin.MICROCOIN) |
                s" send $addr 1ubtc     " ! SendMoneyText(address, Coin.MICROCOIN) |
                s" send $addr 1 mbtc    " ! SendMoneyText(address, Coin.MILLICOIN) |
                s" send $addr 1$$       " ! SendMoneyTextUsd(address, 100) |
                s" send $addr 1¢        " ! SendMoneyTextUsd(address, 1) |
                s" send $addr 1 usd     " ! SendMoneyTextUsd(address, 100) |
                s" send $addr 10.01 usd " ! SendMoneyTextUsd(address, 1001) |
                s" send $addr 1,000 mbtc" ! SendMoneyText(address, Coin.MILLICOIN.multiply(1000)) |
                s" send $addr 2 mbtc    " ! SendMoneyText(address, Coin.MILLICOIN.multiply(2)) |
                s" send $addr 1.5 mbtc  " ! SendMoneyText(address, Coin.MILLICOIN.multiply(3).divide(2)) |
                s" $addr 1ubtc          " ! SendMoneyText(address, Coin.MICROCOIN) |
                s" $addr 1 btc          " ! SendMoneyText(address, Coin.COIN) |
                s" $addr 1 satoshi      " ! SendMoneyText(address, Coin.SATOSHI) |
                "addr                   " ! AddressRequest() |> { (command, result) =>
                QueryUnderstand.decodeQuery(command) must beRight(result)
            }
        }

        "reject these commands" in {
            "" ::
                s"send ${addr.toLowerCase} 1 btc" ::
                s"send $addr 1.0 " ::
                " oopsie" ::
                "oops wrong person" :: Nil map { command:String =>
                QueryUnderstand.decodeQuery(command) must beLeft
            }
        }
    }
}