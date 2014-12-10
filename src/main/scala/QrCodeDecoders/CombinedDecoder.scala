package QrCodeDecoders

import java.awt.image.BufferedImage

object CombinedDecoder extends QrCodeDecoder {
    def qrCodeImageDecode(image: BufferedImage): Option[String] = {
        ZxingDecoder.qrCodeImageDecode(image) orElse { ZbarDecoder.qrCodeImageDecode(image) }
    }
}
