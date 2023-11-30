package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAliasType
import com.thoughtworks.xstream.annotations.XStreamAsAttribute


/**
 *
 * @author Vlad Kolotov
 */
@XStreamAliasType("Reserved")
class Reserved {
    @XStreamAsAttribute
    val start = 0

    @XStreamAsAttribute
    val end = 0
}

