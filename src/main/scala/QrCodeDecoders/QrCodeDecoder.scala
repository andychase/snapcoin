package QrCodeDecoders

import java.awt.image.BufferedImage

trait QrCodeDecoder {
    def qrCodeImageDecode(image: BufferedImage): Option[String]
}
