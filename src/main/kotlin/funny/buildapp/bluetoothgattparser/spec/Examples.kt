package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAliasType
import com.thoughtworks.xstream.annotations.XStreamImplicit
import java.util.*


/**
 *
 * @author Vlad Kolotov
 */
@XStreamAliasType("Examples")
class Examples {
    @XStreamImplicit
    val examples: List<String>? = null
        get() = Collections.unmodifiableList(field)
}

