package funny.buildapp.bluetoothgattparser.spec

import java.util.*


/**
 *
 * @author Vlad Kolotov
 */
class FieldFormat internal constructor(val name: String, type: FieldType, size: Int) {
    private val type: FieldType
    val size: Int

    init {
        this.type = type
        this.size = size
    }

    fun getType(): FieldType {
        return type
    }

    val isReal: Boolean
        get() = type === FieldType.UINT || type === FieldType.SINT
    val isDecimal: Boolean
        get() = type === FieldType.FLOAT_IEE754 || type === FieldType.FLOAT_IEE11073
    val isBoolean: Boolean
        get() = type === FieldType.BOOLEAN
    val isString: Boolean
        get() = type === FieldType.UTF8S || type === FieldType.UTF16S
    val isStruct: Boolean
        get() = type === FieldType.STRUCT
    val isNumber: Boolean
        get() = isReal || isDecimal

    companion object {
        const val FULL_SIZE = -1
        private val PREDEFINED = mapOf(
            "boolean" to FieldFormat("boolean", FieldType.BOOLEAN, 1),
            "nibble" to FieldFormat("nibble", FieldType.UINT, 4),
            "float32" to FieldFormat("float32", FieldType.FLOAT_IEE754, 32),
            "float64" to FieldFormat("float64", FieldType.FLOAT_IEE754, 64),
            "sfloat" to FieldFormat("SFLOAT", FieldType.FLOAT_IEE11073, 16),
            "float" to FieldFormat("FLOAT", FieldType.FLOAT_IEE11073, 32),
            // "duint16" to FieldFormat("duint16", FieldType.UINT, 16),
            "utf8s" to FieldFormat("utf8s", FieldType.UTF8S, FULL_SIZE),
            "utf16s" to FieldFormat("utf16s", FieldType.UTF16S, FULL_SIZE),
            "struct" to FieldFormat("struct", FieldType.STRUCT, FULL_SIZE),
            "reg-cert-data-list" to FieldFormat("struct", FieldType.STRUCT, FULL_SIZE)
        )


        fun valueOf(name: String?): FieldFormat? {
            if (name == null) {
                return null
            }
            val fieldName = name.lowercase(Locale.getDefault())
            return if (PREDEFINED.containsKey(fieldName)) {
                PREDEFINED[fieldName]
            } else if (fieldName.startsWith("uint") || fieldName.endsWith("bit")) {
                FieldFormat(fieldName, FieldType.UINT, parseSize(fieldName))
            } else if (fieldName.startsWith("sint")) {
                FieldFormat(fieldName, FieldType.SINT, parseSize(fieldName))
            } else {
                null
            }
        }

        private fun parseSize(name: String): Int {
            return try {
                name.replace("uint", "").replace("sint", "").replace("bit", "").toInt()
            } catch (ex: NumberFormatException) {
                throw IllegalStateException(ex)
            }
        }
    }
}
