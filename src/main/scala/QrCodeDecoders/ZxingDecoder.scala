package QrCodeDecoders

import java.awt.image.BufferedImage
import java.util
import com.google.zxing._
import com.google.zxing.common.{BitMatrix, HybridBinarizer}
import com.google.zxing.qrcode.QRCodeWriter
import misc.BufferedImageLuminanceSource

object ZxingDecoder extends QrCodeDecoder {

    def encode(string: String): BitMatrix = {
        new QRCodeWriter().encode(string, BarcodeFormat.QR_CODE, 128, 128)
    }

    def qrCodeImageDecode(image: BufferedImage): Option[String] = {
        // Build hints via Java Collection
        val hints = new util.HashMap[DecodeHintType, Any]()
        hints.put(DecodeHintType.TRY_HARDER, true)
        val formats = new util.ArrayList[BarcodeFormat]()
        formats.add(BarcodeFormat.QR_CODE)
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats)

        val source = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))
        try {
            Some(new MultiFormatReader().decode(source, hints).getText)
        } catch {
            case _: NotFoundException => None
        }
    }
}
