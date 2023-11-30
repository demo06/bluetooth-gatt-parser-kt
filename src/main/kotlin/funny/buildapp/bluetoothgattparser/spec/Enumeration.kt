package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import java.math.BigInteger


/**
 *
 * @author Vlad Kolotov
 */
@XStreamAlias("Enumeration")
class Enumeration {
    @XStreamAsAttribute
    val key: BigInteger? = null

    @XStreamAsAttribute
    val value: String? = null

    @XStreamAsAttribute
    val requires: String? = null
}



