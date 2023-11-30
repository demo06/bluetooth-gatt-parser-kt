package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias


/**
 *
 * @author Vlad Kolotov
 */
@XStreamAlias("Properties")
class Properties {
    @XStreamAlias("Read")
    val read: String? = null

    @XStreamAlias("Write")
    val write: String? = null

    @XStreamAlias("WriteWithoutResponse")
    val writeWithoutResponse: String? = null

    @XStreamAlias("SignedWrite")
    val signedWrite: String? = null

    @XStreamAlias("ReliableWrite")
    val reliableWrite: String? = null

    @XStreamAlias("Notify")
    val notify: String? = null

    @XStreamAlias("Indicate")
    val indicate: String? = null

    @XStreamAlias("WritableAuxiliaries")
    val writableAuxiliaries: String? = null

    @XStreamAlias("Broadcast")
    val broadcast: String? = null
}

