package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamImplicit


/**
 *
 * @author Vlad Kolotov
 */
@XStreamAlias("Characteristics")
class Characteristics {
    @XStreamImplicit(itemFieldName = "Characteristic")
    val characteristics: List<CharacteristicAccess>? = null
}
