package QrCodeDecoders

import java.awt.image.BufferedImage
import misc.BufferedImageLuminanceSource
import net.sourceforge.zbar.{Image, ImageScanner}
import net.sourceforge.zbar

object ZbarDecoder extends QrCodeDecoder {
    def qrCodeImageDecode(image: BufferedImage): Option[String] = {
        try {
            val source = new BufferedImageLuminanceSource(image)
            val barcode = new Image(source.getWidth, source.getHeight, "Y800")
            barcode.setData(source.getMatrix)
            val scanner = new ImageScanner()
            scanner.scanImage(barcode)
            if (scanner.getResults.size() > 0) {
                scanner.getResults.toArray()(0) match {
                    case r: zbar.Symbol => Some(r.getData)
                    case _ => None
                }
            } else {
                None
            }
        }
        catch {
            case e: Exception => None
        }
    }

}
