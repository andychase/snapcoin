package PhotoMoney

import javax.mail.Address
import javax.mail.internet.InternetAddress

import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

/**
 * Some carriers have different mms addresses
 * from sms address. Match and convert these carriers
 * so if you send a picture message to the service
 * you get a plain text message response.
 */
object AddressUtilities {
    val carrierToTxtMapping = Map(
        "verizon" -> "vtext.com"
    )

    val pixToTxtMapping = Map(
        "vzwpix.com" -> "vtext.com",
        "mms.att.net" -> "txt.att.net",
        "pm.sprint.com " -> "messaging.sprintpcs.com",
        "messaging.nextel.com" -> "page.nextel.com",
        "mms.uscc.net " -> "email.uscc.net"
    )

    val txtToPixMapping = pixToTxtMapping.map(_.swap)

    def txtToPix(address: Address) = splitAndMap(address, txtToPixMapping)

    def pixToTxt(address: Address) = splitAndMap(address, pixToTxtMapping)

    def numberToEmail(phone: PhoneNumber, carrier: String): Option[Address] =
        carrierToTxtMapping.get(carrier) match {
            case Some(carrierTxtAddress) =>
                Some(new InternetAddress(phone.getNationalNumber.toString + "@" + carrierTxtAddress))
            case None => None
        }

    private def splitAndMap(address: Address, mapping: Map[String, String]): Address =
        new InternetAddress(splitAndMap(address.toString.split("@").toList, mapping))

    private def splitAndMap(addressParts: List[String], mapping: Map[String, String]): String =
        mapping.get(addressParts.tail.mkString("")) match {
            case Some(ending) => addressParts.head ++ "@" ++ ending
            case None => addressParts.mkString("@")
        }
}
