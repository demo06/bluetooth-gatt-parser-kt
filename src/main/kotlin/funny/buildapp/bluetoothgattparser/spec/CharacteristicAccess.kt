package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamImplicit


/**
 *
 * @author Vlad Kolotov
 */
class CharacteristicAccess {
    @XStreamAsAttribute
    val name: String? = null

    @XStreamAsAttribute
    val type: String? = null

    @XStreamAlias("InformativeText")
    val informativeText: String? = null

    @XStreamAlias("Requirement")
    val requirement: String? = null

    @XStreamImplicit
    val properties: List<Properties>? = null
}

