package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias


/**
 *
 * @author Vlad Kolotov
 */
@XStreamAlias("InformativeText")
class InformativeText {
    @XStreamAlias("Abstract")
    val abstract: String? = null

    @XStreamAlias("Summary")
    val summary: String? = null

    @XStreamAlias("Examples")
    val examples: Examples? = null

    @XStreamAlias("Note")
    val note: String? = null
}

