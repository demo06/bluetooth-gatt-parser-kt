package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamImplicit
import java.math.BigInteger


/**
 *
 * @author Vlad Kolotov
 */
@XStreamAlias("Field")
class Field {
    @XStreamAsAttribute
    private val name: String? = null

    @XStreamAlias("InformativeText")
    val informativeText: String? = null

    @XStreamImplicit(itemFieldName = "Requirement")
    val requirements: List<String>? = null

    @XStreamAlias("Reference")
    val reference: String? = null

    @XStreamAlias("Format")
    val format: String? = null

    @XStreamAlias("BitField")
    val bitField: BitField? = null

    @XStreamAlias("DecimalExponent")
    val decimalExponent: Int? = null

    @XStreamAlias("BinaryExponent")
    val binaryExponent: Int? = null

    @XStreamAlias("Multiplier")
    val multiplier: Int? = null

    @XStreamAlias("Unit")
    val unit: String? = null

    @XStreamAlias("Minimum")
    val minimum: Double? = null

    @XStreamAlias("Maximum")
    val maximum: Double? = null

    @XStreamAlias("Offset")
    val offset: Double? = null

    @XStreamAlias("Enumerations")
    val enumerations: Enumerations? = null

    // extensions
    @XStreamAsAttribute
    val isUnknown = false

    @XStreamAsAttribute
    val isSystem = false

    fun getName(): String? {
        return name?.trim { it <= ' ' }
    }

    val isFlagField: Boolean
        get() = FlagUtils.isFlagsField(this)
    val isOpCodesField: Boolean
        get() = FlagUtils.isOpCodesField(this)

    fun hasEnumerations(): Boolean {
        return enumerations?.enumerations != null && enumerations.enumerations!!.isNotEmpty()
    }

    fun getFieldFormat(): FieldFormat? {
        return FieldFormat.valueOf(format)
    }

    fun getEnumeration(key: BigInteger?): Enumeration? {
        return FlagUtils.getEnumeration(this, key)
    }

    fun getEnumerations(value: String?): List<Enumeration> {
        return FlagUtils.getEnumerations(this, value)
    }
}

