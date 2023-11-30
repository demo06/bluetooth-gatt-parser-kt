package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamImplicit
import java.util.*



/**
 *
 * @author Vlad Kolotov
 */
@XStreamAlias("Enumerations")
class Enumerations {
    @XStreamImplicit
    val enumerations: List<Enumeration>? = null
        get() = if (field != null) Collections.unmodifiableList(field) else null

    @XStreamImplicit
    val reserves: List<Reserved>? = null
        get() = if (field != null) Collections.unmodifiableList(field) else field
}

