package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamImplicit

@XStreamAlias("BitField")
class BitField {
    @XStreamImplicit
    val bits: List<Bit>? = null
}

