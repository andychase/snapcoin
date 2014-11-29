import javax.mail.Address
import javax.mail.internet.InternetAddress

/**
 * Some carriers have different mms addresses
 * from sms address. Match and convert these carriers
 * so if you send a picture message to the service
 * you get a plain text message response.
 */
object PhoneTranslation {

    def CheckAndTranslate(address: Address): InternetAddress =
        new InternetAddress(CheckAndTranslate(address.toString.split("@").toList))

    private def CheckAndTranslate(addressParts: List[String]): String =
        addressParts.head ++ "@" ++ EndingsTransform(addressParts.tail.mkString(""))

    private def EndingsTransform(ending: String): String = ending match {
        case "vzwpix.com" => "vtext.com"
        case "mms.att.net" => "txt.att.net"
        case "pm.sprint.com " => "messaging.sprintpcs.com"
        case "messaging.nextel.com" => "page.nextel.com"
        case "mms.uscc.net " => "email.uscc.net"
        case _ => ending
    }
}
