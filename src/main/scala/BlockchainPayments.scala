import com.google.gson.Gson
import java.io.InputStreamReader
import java.util.regex.Pattern
import org.apache.http.impl.client.HttpClients
import org.apache.http.concurrent.FutureCallback
import org.apache.http.client.methods.HttpGet
import org.apache.http.HttpResponse
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy


class BlockchainPayments(private val token: String, private val password: String) extends PaymentProvider {
    def url(address: String, satoshi: Long) =
        s"https://blockchain.info/merchant/$token/payment?password=$password&to=$address&amount=$satoshi"

    def satoshi(amount: Float) = (amount * 100000000).toLong

    def btc(amount: Long): Double = amount / 100000000.0

    class response_class {
        var error = ""
        var message = ""
    }

    val fundsMatcher = Pattern.compile("Insufficient Funds Available: ([0-9]+) Needed: ([0-9]+)")

    def fundsFormatConvert(err: String) = {
        val matches = fundsMatcher.matcher(err)
        matches.find()
        val (avail, needed) = (matches.group(1).toLong, matches.group(2).toLong)
        "Insufficient Funds Available: %f btc Needed: %f btc".format(btc(avail), btc(needed))
    }

    def sendPayment(address: String, amount: Float, reply: (String => Unit)) {
        get(url(address, satoshi(amount)), new FutureCallback[HttpResponse] {
            def failed(e: Exception) {
                reply("Couldn't get a response from wallet")
            }

            def completed(httpResponse: HttpResponse) {
                val reader = new InputStreamReader(httpResponse.getEntity.getContent)
                val response: response_class = new Gson().fromJson(reader, classOf[response_class])

                (response.message, response.error) match {
                    case (msg: String, _) =>
                        reply(msg)
                    case (_, err: String) if err startsWith "Insufficient Funds Available" =>
                        reply(fundsFormatConvert(err))
                    case (_, err: String) =>
                        reply(err)
                    case _ =>
                        reply("Couldn't understand response from wallet")
                }
            }

            def cancelled() {
                reply("Couldn't get a response from wallet")
            }
        })
    }

    def validateCredentials() {
        // 'asdf' will be invalid first, before Bitcoins are attempted
        // to be spend. Using maximum for good measure.
        val maximumBitcoins = 21000000
        val httpResponse = get(url("asdf", satoshi(maximumBitcoins)))

        val reader = new InputStreamReader(httpResponse.getEntity.getContent)
        val response: response_class = new Gson().fromJson(reader, classOf[response_class])
        response.error match {
            case "GUID not found" =>
                tokenError("Bitcoin Token/Username not found")
            case "Error Decrypting Wallet" | "pad block corrupted" =>
                passwordError("Bitcoin password incorrect")
            case "com.google.bitcoin.core.AddressFormatException: Input to short" =>
                // Success! "asdf" isn't a good address
            case _ =>
                throw new BitcoinConnectionError("Error connecting to bitcoin wallet")
        }
    }

    def get(url: String) = {
        val client = HttpClients.createDefault()
        client.execute(new HttpGet(url))
    }

    def get(url: String, callback: FutureCallback[HttpResponse]) {
        val httpClient = HttpAsyncClients.custom.setSSLStrategy(SSLIOSessionStrategy.getDefaultStrategy).build
        httpClient.start()
        httpClient.execute(new HttpGet(url), callback)
    }
}
