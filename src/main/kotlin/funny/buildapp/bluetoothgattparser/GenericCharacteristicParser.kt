package funny.buildapp.bluetoothgattparser

import funny.buildapp.bluetoothgattparser.num.FloatingPointNumberFormatter
import funny.buildapp.bluetoothgattparser.num.RealNumberFormatter
import funny.buildapp.bluetoothgattparser.spec.*
import org.slf4j.LoggerFactory
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.math.ceil

/**
 * A generic implementation of a GATT characteristic parser capable of reading and writing standard/approved
 * Bluetooth GATT characteristics as well as user defined GATT characteristics. Quite often some parts of the Bluetooth
 * GATT specification is misleading and also incomplete, furthermore some "approved" GATT XML fields do not
 * follow the specification, therefore the implementation of this parser is based not only on Bluetooth GATT
 * specification (Core v5) but also based on some heuristic methods, e.g. by studying/following GATT XML files for
 * some services and characteristics.
 *
 * @author Vlad Kolotov
 */
class GenericCharacteristicParser(val reader: BluetoothGattSpecificationReader) : CharacteristicParser {
    private val logger = LoggerFactory.getLogger(GenericCharacteristicParser::class.java)


    @Throws(CharacteristicFormatException::class)
    override fun parse(characteristic: Characteristic?, raw: ByteArray): LinkedHashMap<String, FieldHolder> {
        val result = LinkedHashMap<String, FieldHolder>()
        validate(characteristic)
        var offset = 0
        val fields: List<Field>? = characteristic?.value?.fields
        val requires: MutableSet<String>? = fields?.let { FlagUtils.getReadFlags(it, raw).toMutableSet() }
        requires?.add("Mandatory")
        if (fields != null) {
            for (field: Field in fields) {
                val requirements: List<String>? = field.requirements
                if (requirements != null && !requirements.isEmpty() && !requires?.containsAll(requirements)!!) {
                    // skipping field as per requirement in the Flags field
                    continue
                }
                if (field.reference != null) {
                    val subCharacteristic =
                        reader.getCharacteristicByType(field.reference.trim())
                            ?.let { parse(it, getRemainder(raw, offset)) }
                    if (subCharacteristic != null) {
                        result.putAll(subCharacteristic)
                    }
                    val size = subCharacteristic?.values?.let { getSize(it) }
                    if (size == FieldFormat.FULL_SIZE) {
                        break
                    }
                    offset += size ?: 0
                } else {
                    if (FlagUtils.isFlagsField(field)) {
                        // skipping flags field
                        offset += field.getFieldFormat()?.size ?: 0
                        continue
                    }
                    val fieldFormat: FieldFormat? = field.getFieldFormat()
                    result[field.getName() ?: ""] = parseField(field, raw, offset)
                    if (fieldFormat?.size === FieldFormat.FULL_SIZE) {
                        // full size field, e.g. a string
                        break
                    }
                    offset += field.getFieldFormat()?.size ?: 0
                }
            }
        }
        return result
    }

    @Throws(CharacteristicFormatException::class)
    override fun serialize(fieldHolders: List<FieldHolder>): ByteArray {
        val bitSet = BitSet()
        var offset = 0
        for (holder: FieldHolder in fieldHolders) {
            if (holder.isValueSet()) {
                var size: Int = holder.getField()?.getFieldFormat()?.size ?: 0
                val serialized = serialize(holder)
                if (size == FieldFormat.FULL_SIZE) {
                    size = serialized.length()
                }
                concat(bitSet, serialized, offset, size)
                offset += size
            }
        }
        // BitSet does not keep 0, fields could be set all to 0, resulting bitSet to be of 0 length,
        // however data array must not be empty, hence forcing to return an array with first byte of 0 value
        val data = if (bitSet.isEmpty) byteArrayOf(0) else bitSet.toByteArray()
        return if (data.size > 20) bitSet.toByteArray().copyOf(20) else data
    }

    fun parse(field: Field, raw: ByteArray, offset: Int): Any {
        val fieldFormat: FieldFormat? = field.getFieldFormat()
        val size: Int = fieldFormat?.size ?: 0
        when (fieldFormat?.getType()) {
            FieldType.BOOLEAN -> return parseBoolean(raw, offset)
            FieldType.UINT -> return deserializeReal(raw, offset, size, false)
            FieldType.SINT -> return deserializeReal(raw, offset, size, true)
            FieldType.FLOAT_IEE754 -> return deserializeFloat(
                BluetoothGattParserFactory.iEEE754FloatingPointNumberFormatter, raw, offset, size
            )

            FieldType.FLOAT_IEE11073 -> return deserializeFloat(
                BluetoothGattParserFactory.iEEE11073FloatingPointNumberFormatter, raw, offset, size
            )

            FieldType.UTF8S -> return deserializeString(raw, offset, "UTF-8")
            FieldType.UTF16S -> return deserializeString(raw, offset, "UTF-16")
            FieldType.STRUCT -> return BitSet.valueOf(raw)[offset, offset + raw.size * 8].toByteArray()
            else -> throw IllegalStateException("Unsupported field format: " + fieldFormat?.getType())
        }
    }

    fun serialize(value: Boolean): BitSet {
        val bitSet = BitSet()
        if (value) {
            bitSet.set(0)
        }
        return bitSet
    }

    fun concat(target: BitSet, source: BitSet, offset: Int, size: Int) {
        for (i in 0 until size) {
            if (source[i]) {
                target.set(offset + i)
            }
        }
    }

    private fun serialize(holder: FieldHolder): BitSet {
        val fieldFormat: FieldFormat? = holder.getField()?.getFieldFormat()
        when (fieldFormat?.getType()) {
            FieldType.BOOLEAN -> return serialize(holder.getBoolean(null))
            FieldType.UINT, FieldType.SINT -> return serializeReal(holder)
            FieldType.FLOAT_IEE754 -> return serializeFloat(
                BluetoothGattParserFactory.iEEE754FloatingPointNumberFormatter, holder
            )

            FieldType.FLOAT_IEE11073 -> return serializeFloat(
                BluetoothGattParserFactory.iEEE11073FloatingPointNumberFormatter, holder
            )

            FieldType.UTF8S -> return serializeString(holder, "UTF-8")
            FieldType.UTF16S -> return serializeString(holder, "UTF-16")
            FieldType.STRUCT -> return BitSet.valueOf(holder.getRawValue() as ByteArray?)
            else -> throw IllegalStateException("Unsupported field format: " + fieldFormat?.getType())
        }
    }

    private fun parseBoolean(raw: ByteArray, offset: Int): Boolean {
        return BitSet.valueOf(raw)[offset]
    }

    private fun parseField(field: Field, raw: ByteArray, offset: Int): FieldHolder {
        val fieldFormat: FieldFormat? = field.getFieldFormat()
        if (fieldFormat?.size ?: 0 !== FieldFormat.FULL_SIZE && offset + (fieldFormat?.size ?: 0) > raw.size * 8) {
            throw CharacteristicFormatException(
                ("Not enough bits to parse field \"" + field.getName()).toString() + "\". "
                        + "Data length: " + raw.size + " bytes. "
                        + "Looks like your device does not conform SIG specification."
            )
        }
        val value = parse(field, raw, offset)
        return FieldHolder(field, value)
    }

    private fun validate(characteristic: Characteristic?) {
        if (!characteristic?.isValidForRead!!) {
            logger.error("Characteristic cannot be parsed: \"{}\".", characteristic.name)
            throw CharacteristicFormatException(
                (("Characteristic cannot be parsed: \"" +
                        characteristic.name) + "\".")
            )
        }
    }

    private fun getSize(holders: Collection<FieldHolder>): Int {
        var size = 0
        for (holder: FieldHolder in holders) {
            val field: Field? = holder.getField()
            if (field?.getFieldFormat()?.size ?: 0 === FieldFormat.FULL_SIZE) {
                return FieldFormat.FULL_SIZE
            }
            size += field?.getFieldFormat()?.size ?: 0
        }
        return size
    }

    private fun serializeReal(holder: FieldHolder): BitSet {
        val realNumberFormatter: RealNumberFormatter = BluetoothGattParserFactory.twosComplementNumberFormatter
        val size: Int = holder.getField()?.getFieldFormat()?.size ?: 0
        val signed = holder.getField()?.getFieldFormat()?.getType() === FieldType.SINT
        if ((signed && size <= 32) || (!signed && size < 32)) {
            return realNumberFormatter.serialize(holder.getRawValue() as Int, size, signed)
        } else return if ((signed && size <= 64) || (!signed && size < 64)) {
            realNumberFormatter.serialize(holder.getRawValue() as Long, size, signed)
        } else {
            realNumberFormatter.serialize(holder.getRawValue() as BigInteger, size, signed)
        }
    }

    private fun deserializeReal(raw: ByteArray, offset: Int, size: Int, signed: Boolean): Any {
        val realNumberFormatter: RealNumberFormatter = BluetoothGattParserFactory.twosComplementNumberFormatter
        val toIndex = offset + size
        if ((signed && size <= 32) || (!signed && size < 32)) {
            return realNumberFormatter.deserializeInteger(BitSet.valueOf(raw)[offset, toIndex], size, signed)
        } else return if ((signed && size <= 64) || (!signed && size < 64)) {
            realNumberFormatter.deserializeLong(BitSet.valueOf(raw).get(offset, toIndex), size, signed)
        } else {
            realNumberFormatter.deserializeBigInteger(BitSet.valueOf(raw).get(offset, toIndex), size, signed)
        }
    }

    private fun deserializeFloat(formatter: FloatingPointNumberFormatter, raw: ByteArray, offset: Int, size: Int): Any {
        val toIndex = offset + size
        if (size == 16) {
            return formatter.deserializeSFloat(BitSet.valueOf(raw)[offset, toIndex])
        } else if (size == 32) {
            return formatter.deserializeFloat(BitSet.valueOf(raw)[offset, toIndex])
        } else return if (size == 64) {
            formatter.deserializeDouble(BitSet.valueOf(raw).get(offset, toIndex))
        } else {
            throw IllegalStateException("Unknown bit size for float numbers: $size")
        }
    }

    private fun serializeFloat(formatter: FloatingPointNumberFormatter, holder: FieldHolder): BitSet {
        val size: Int = holder.getField()?.getFieldFormat()?.size ?: 0
        if (size == 16) {
            return formatter.serializeSFloat(holder.getFloat(null) ?: 0f)
        } else if (size == 32) {
            return formatter.serializeFloat(holder.getFloat(null) ?: 0f)
        } else return if (size == 64) {
            formatter.serializeDouble(holder.getDouble(null) ?: 0.0)
        } else {
            throw IllegalStateException("Invalid bit size for float numbers: $size")
        }
    }

    private fun deserializeString(raw: ByteArray, offset: Int, encoding: String): String {
        try {
            return String(BitSet.valueOf(raw)[offset, offset + raw.size * 8].toByteArray(), charset(encoding))
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException(e)
        }
    }

    private fun serializeString(holder: FieldHolder, encoding: String): BitSet {
        try {
            return BitSet.valueOf(holder.getString(null).toByteArray(Charset.forName(encoding)))
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException(e)
        }
    }

    private fun getRemainder(raw: ByteArray, offset: Int): ByteArray {
        val remained = BitSet.valueOf(raw)[offset, raw.size * 8].toByteArray()
        val remainedWithTrailingZeros = ByteArray((raw.size - ceil(offset / 8.0).toInt()))
        System.arraycopy(remained, 0, remainedWithTrailingZeros, 0, remained.size)
        return remainedWithTrailingZeros
    }

}
