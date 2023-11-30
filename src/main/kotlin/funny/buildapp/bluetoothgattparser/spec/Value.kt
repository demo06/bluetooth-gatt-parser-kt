package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamImplicit
import java.util.*


/**
 *
 * @author Vlad Kolotov
 */
@XStreamAlias("Value")
class Value {
    @XStreamImplicit
    val fields: List<Field> = emptyList()
}

