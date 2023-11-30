package funny.buildapp.bluetoothgattparser.spec

import funny.buildapp.bluetoothgattparser.BluetoothGattParserFactory
import java.math.BigInteger
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream


/**
 *
 * @author Vlad Kolotov
 */
object FlagUtils {
    fun getReadFlags(fields: List<Field>, data: ByteArray?): Set<String> {
        val flags: MutableSet<String> = HashSet()
        var index = 0
        for (field in fields) {
            if (isFlagsField(field)) {
                val values = parseReadFlags(field, data, index)
                var bitIndex = 0
                for (bit in field.bitField?.bits!!) {
                    val requires = bit.getFlag(values[bitIndex++].toByte())
                    if (requires != null) {
                        val flgs = listOf(*requires.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray())
                        if (flgs.isNotEmpty()) {
                            flags.addAll(flgs)
                        }
                    }
                }
                break
            }
            if (field.reference != null) {
                // if flags field goes after a reference field, then it is not possible to parse the such characteristic
                // simply because we don't know if this reference field if optional or not
                break
            }
            checkNotNull(field.getFieldFormat()) {
                // This is a strange field without format!
                "A filed is missing its format: " + field.getName()
            }
            index += field.getFieldFormat()?.size ?: 0
        }
        return flags
    }

    fun getRequires(field: Field, key: BigInteger?): Any? {
        return getEnumeration(field, key)?.requires
    }


    fun getEnumeration(field: Field, key: BigInteger?): Enumeration? {
        if (key == null) {
            return null
        }
        return field.enumerations?.enumerations?.first {
            it.key == key
        }
    }

    fun getEnumerations(field: Field, value: String?): List<Enumeration> {
        return if (value == null) {
            emptyList()
        } else field.enumerations?.enumerations?.filter { it.value == value } ?: emptyList()
    }


    fun isFlagsField(field: Field): Boolean {
        return "flags".equals(field.getName(), ignoreCase = true) && field.bitField != null
    }

    fun isOpCodesField(field: Field): Boolean {
        val name = field.getName()
        return ("op code".equals(name, ignoreCase = true) || "op codes".equals(
            name,
            ignoreCase = true
        )) && field.enumerations != null && field.enumerations.enumerations?.isNotEmpty() == true
    }

    fun getAllFlags(flagsField: Field?): Set<String> {
        val result: MutableSet<String> = HashSet()
        if (flagsField?.bitField != null) {
            for (bit in flagsField.bitField.bits!!) {
                for (enumeration in bit.enumerations?.enumerations!!) {
                    if (enumeration.requires != null) {
                        result.add(enumeration.requires)
                    }
                }
            }
        }
        return result
    }

    fun getAllOpCodes(field: Field): MutableSet<out Any?> {
        val result: MutableSet<String?> = HashSet()
        if (field.enumerations?.enumerations == null) {
            return Collections.EMPTY_SET
        }
        for (enumeration in field.enumerations.enumerations!!) {
            result.add(enumeration.requires)
        }
        return result
    }

    fun getFlags(fields: List<Field>): Field? {
        for (field in fields) {
            if (isFlagsField(field)) {
                return field
            }
        }
        return null
    }

    fun getOpCodes(fields: List<Field>): Field? {
        for (field in fields) {
            if (isOpCodesField(field)) {
                return field
            }
        }
        return null
    }

    fun parseReadFlags(flagsField: Field, raw: ByteArray?, index: Int): IntArray {
        val bitSet = BitSet.valueOf(raw)[index, index + flagsField.getFieldFormat()?.size!!]
        val bits: List<Bit> = flagsField.bitField?.bits!!
        val flags = IntArray(bits.size)
        var offset = 0
        for (i in bits.indices) {
            val size: Int = bits[i].size
            flags[i] = BluetoothGattParserFactory.twosComplementNumberFormatter.deserializeInteger(
                bitSet[offset, offset + size], size, false
            )
            offset += size
        }
        return flags
    }
}
