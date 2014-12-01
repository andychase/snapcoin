import java.util.UUID

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.apache.commons.codec.digest.DigestUtils
import org.jasypt.util.text.BasicTextEncryptor

class PhotoMoneyServer(paymentProvider: PaymentProvider) {

    def getHash(input: String) = {
        DigestUtils.sha256Hex(input)
    }

    def encrypt(password: String, data: String) = {
        val textEncryptor = new BasicTextEncryptor()
        textEncryptor.setPassword(password)
        textEncryptor.encrypt(data)
    }

    def decrypt(password: String, data: String) = {
        val textEncryptor = new BasicTextEncryptor()
        textEncryptor.setPassword(password)
        textEncryptor.decrypt(data)
    }

    def checkRegistered(secretId: String): Boolean = PersistentStorage.get(getHash(secretId)).isDefined

    def register(): (String, String) = {
        // Generate Secret Code + Hash
        val secretCode = UUID.randomUUID().toString
        // Create Bitcoin wallet
        val (walletID, bitcoinAddress) = paymentProvider.createWallet(secretCode)
        // Save walletID
        val encryptedWalletID = encrypt(secretCode, walletID)
        PersistentStorage.put(getHash(secretCode), encryptedWalletID)
        (secretCode, bitcoinAddress)
    }

    def introMessage(phoneNumber: PhoneNumber, carrier: String, bitcoinAddress: String) = {
        val qrcodeImage = new QRCodeWriter().encode("bitcoin:" + bitcoinAddress, BarcodeFormat.QR_CODE, 128, 128)
        val introMessage = ""
        (qrcodeImage, introMessage)
    }

    def sendMoney(secretCode: String, bitcoinAddress: String, amount: Long): Unit = {
        val encryptedWalletID = PersistentStorage.get(getHash(secretCode)).get
        val walletID = decrypt(secretCode, encryptedWalletID)
        paymentProvider.sendPayment(walletID, secretCode, bitcoinAddress, amount)
    }
}
