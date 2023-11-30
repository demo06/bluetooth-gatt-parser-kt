package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import java.math.BigInteger


@XStreamAlias("Bit")
class Bit {
    @XStreamAsAttribute
    val index = 0

    @XStreamAsAttribute
    val size = 0

    @XStreamAsAttribute
    val name: String? = null

    @XStreamAlias("Enumerations")
    val enumerations: Enumerations? = null

    fun getFlag(value: Byte): String? {
        if (enumerations == null) {
            return null
        }
        for (enumeration in enumerations.enumerations!!) {
            if (enumeration.key!! == BigInteger.valueOf(value.toLong())) {
                return enumeration.requires
            }
        }
        return null
    }
}
