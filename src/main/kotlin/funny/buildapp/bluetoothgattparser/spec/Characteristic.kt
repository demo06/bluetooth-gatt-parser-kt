package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute


/**
 *
 * @author Vlad Kolotov
 */
@XStreamAlias("Characteristic")
class Characteristic {
    @XStreamAsAttribute
    val name: String? = null

    @XStreamAsAttribute
    val uuid: String? = null

    @XStreamAsAttribute
    val type: String? = null

    @XStreamAlias("InformativeText")
    val informativeText: InformativeText? = null

    @XStreamAlias("Value")
    val value: Value? = null
    var isValidForRead = false
    var isValidForWrite = false
}

