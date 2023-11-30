package funny.buildapp.bluetoothgattparser

import funny.buildapp.bluetoothgattparser.num.TwosComplementNumberFormatter
import funny.buildapp.bluetoothgattparser.spec.*
import funny.buildapp.bluetoothgattparser.spec.Enumeration
import org.apache.commons.beanutils.converters.*
import java.io.UnsupportedEncodingException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*
import kotlin.math.pow

class FieldHolder() {
    private var field: Field? = null

    /**
     * Sets the field value to a raw value.
     * @param value a new field value
     */
    var value: Any? = null

    /**
     * Create a new field holder for a given GATT field.
     * @param field GATT field specification
     */
    constructor(field: Field?) : this() {
        this.field = field
    }


    /**
     * Creates a new field holder for a given GATT field and its raw value.
     * @param field GATT field specification
     * @param value field value
     */
    constructor(field: Field, value: Any?) : this(field) {
        this.value = value
    }


    /**
     * Returns the GATT field specification.
     * @return GATT field specification
     */
    fun getField(): Field? {
        return field
    }

    /**
     * Checks whether the field is a number.
     * @return true if a given field is a number, false otherwise
     */
    fun isNumber(): Boolean? {
        return field?.getFieldFormat()?.isNumber
    }

    /**
     * Checks whether the field is of boolean type.
     * @return true if a given field is of type boolean, false otherwise
     */
    fun isBoolean(): Boolean? {
        return field?.getFieldFormat()?.isBoolean
    }

    /**
     * Checks whether the field is of string type.
     * @return true if a given field is of type string, false otherwise
     */
    fun isString(): Boolean? {
        return field?.getFieldFormat()?.isString
    }

    /**
     * Checks whether the field is of struct type.
     * @return true if a given field is of type struct, false otherwise
     */
    fun isStruct(): Boolean? {
        return field?.getFieldFormat()?.isStruct
    }


    /**
     * Returns an Integer representation of the field or a default value in case if the field cannot
     * be converted to an Integer.
     * @param def the default value to be returned if an error occurs converting the field
     * @return an Integer representation of the field
     */
    fun getInteger(def: Int?): Int? {
        val result = IntegerConverter(null).convert(Int::class.java, prepareValue())
        return if (result != null) {
            val multiplier = getMultiplier()
            val offset = getOffset()
            if (multiplier != 1.0 || offset != 0.0) {
                Math.round(result * multiplier + offset).toInt()
            } else {
                result
            }
        } else {
            def
        }
    }

    /**
     * Returns a Long representation of the field or a default value in case if the field cannot
     * be converted to a Long.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a Long representation of the field
     */
    fun getLong(def: Long?): Long? {
        val result = LongConverter(null).convert(Long::class.java, prepareValue())
        return if (result != null) {
            val multiplier = getMultiplier()
            val offset = getOffset()
            if (multiplier != 1.0 || offset != 0.0) {
                Math.round(result * multiplier + offset)
            } else {
                result
            }
        } else {
            def
        }
    }

    /**
     * Returns a BigInteger representation of the field or a default value in case if the field cannot
     * be converted to a BigInteger.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a BigInteger representation of the field
     */
    fun getBigInteger(def: BigInteger?): BigInteger {
        val result = BigDecimalConverter(null).convert(BigDecimal::class.java, prepareValue())
        return if (result != null) result.multiply(BigDecimal.valueOf(getMultiplier()))
            .add(BigDecimal.valueOf(getOffset())).setScale(0, RoundingMode.HALF_UP).toBigInteger() else def!!
    }

    /**
     * Returns a BigDecimal representation of the field or a default value in case if the field cannot
     * be converted to a BigDecimal.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a BigDecimal representation of the field
     */
    fun getBigDecimal(def: BigDecimal?): BigDecimal {
        val result = BigDecimalConverter(null).convert(BigDecimal::class.java, prepareValue())
        return if (result != null) result.multiply(BigDecimal.valueOf(getMultiplier())) else def!!
    }

    /**
     * Returns a Float representation of the field or a default value in case if the field cannot
     * be converted to a Float.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a Float representation of the field
     */
    fun getFloat(def: Float?): Float? {
        val result = FloatConverter(null).convert(Float::class.java, prepareValue())
        return if (result != null) {
            return (result * getMultiplier() + getOffset()).toFloat()
        } else {
            def
        }
    }

    /**
     * Returns a Double representation of the field or a default value in case if the field cannot
     * be converted to a Double.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a Double representation of the field
     */
    fun getDouble(def: Double?): Double? {
        val result = FloatConverter(null).convert(Double::class.java, prepareValue())
        return if (result != null) {
            result * getMultiplier() + getOffset()
        } else {
            def
        }
    }

    /**
     * Returns a Boolean representation of the field or a default value in case if the field cannot
     * be converted to a Boolean.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a Boolean representation of the field
     */
    fun getBoolean(def: Boolean?): Boolean {
        return BooleanConverter(def).convert(Boolean::class.java, prepareValue())
    }

    /**
     * Returns a String representation of the field or a default value in case if the field cannot
     * be converted to a String.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a String representation of the field
     */
    fun getString(def: String?): String {
        return StringConverter(def).convert(String::class.java, prepareValue())
    }

    /**
     * Returns an array representation of the field or a default value in case if the field cannot
     * be converted to array.
     * @param def the default value to be returned if an error occurs converting the field
     * @return an array representation of the field
     */
    fun getBytes(def: ByteArray?): ByteArray {
        return ArrayConverter(ByteArray::class.java, ByteConverter()).convert(
            ByteArray::class.java, value
        )
    }

    /**
     * Returns an Integer representation of the field or null in case if the field cannot
     * be converted to an Integer.
     * @return an Integer representation of the field
     */
    fun getInteger(): Int? {
        return getInteger(null)
    }

    /**
     * Returns a Long representation of the field or null in case if the field cannot
     * be converted to a Long.
     * @return a Long representation of the field
     */
    fun getLong(): Long? {
        return getLong(null)
    }

    /**
     * Returns a BigDecimal representation of the field or null in case if the field cannot
     * be converted to a BigDecimal.
     * @return a BigDecimal representation of the field
     */
    fun getBigDecimal(): BigDecimal {
        return getBigDecimal(null)
    }

    /**
     * Returns a Float representation of the field or null in case if the field cannot
     * be converted to a Float.
     * @return a Float representation of the field
     */
    fun getFloat(): Float? {
        return getFloat(null)
    }

    /**
     * Returns a Double representation of the field or null in case if the field cannot
     * be converted to a Double.
     * @return a Double representation of the field
     */
    fun getDouble(): Double? {
        return getDouble(null)
    }


    /**
     * Returns a BigInteger representation of the field or null in case if the field cannot
     * be converted to a BigInteger.
     * @return a BigInteger representation of the field
     */
    fun getBigInteger(): BigInteger {
        return getBigInteger(null)
    }


    /**
     * Returns a Boolean representation of the field or null in case if the field cannot
     * be converted to a Boolean.
     * @return a Boolean representation of the field
     */
    fun getBoolean(): Boolean {
        return getBoolean(null)
    }

    /**
     * Returns a String representation of the field or null in case if the field cannot
     * be converted to a String.
     * @return a String representation of the field
     */
    fun getString(): String {
        return getString(null)
    }

    /**
     * Returns an array representation of the field or null in case if the field cannot
     * be converted to an array.
     * @return a String representation of the field
     */
    fun getBytes(): ByteArray {
        return getBytes(null)
    }

    /**
     * Returns field raw value.
     * @return field raw value
     */
    fun getRawValue(): Any? {
        return value
    }


    /**
     * Returns field enumeration according to the field value.
     * @return fields enumeration according to the field value
     */
    fun getEnumeration(): Enumeration? {
        val key: BigInteger
        key = if (field?.getFieldFormat()?.isStruct == true && value is ByteArray) {
            val data = value as ByteArray
            TwosComplementNumberFormatter().deserializeBigInteger(
                BitSet.valueOf(data),
                data.size * 8, false
            )
        } else if (field?.getFieldFormat()?.isString == true && value is String) {
            val encoding = if (field?.getFieldFormat()!!.getType() === FieldType.UTF8S) "UTF-8" else "UTF-16"
            try {
                val data = (value as String).toByteArray(charset(encoding))
                TwosComplementNumberFormatter().deserializeBigInteger(
                    BitSet.valueOf(data),
                    data.size * 8, false
                )
            } catch (e: UnsupportedEncodingException) {
                throw IllegalStateException(e)
            }
        } else {
            getBigInteger()
        }
        return field?.let { FlagUtils.getEnumeration(it, key) }
    }

    /**
     * Returns field enumeration "requires" according to the field value.
     * @return fields enumeration "requires" (or a its flag) according to the field value
     */
    fun getEnumerationRequires(): String? {
        val enumeration = getEnumeration()
        return enumeration?.requires
    }


    /**
     * Sets the field value into a new boolean value.
     * @param value a new field value
     */
    fun setBoolean(value: Boolean?) {
        this.value = value
    }

    /**
     * Sets the field value into a new Integer value.
     * @param value a new field value
     */
    fun setInteger(value: Int?) {
        if (value == null) {
            this.value = null
        } else {
            val maximum: Double = field?.maximum ?: 0.0
            require(!(maximum != null && maximum < value)) { "Value [$value] is greater than maximum: $maximum" }
            val minimum: Double = field?.minimum ?: 0.0
            require(!(minimum != null && minimum > value)) { "Value [$value] is less than minimum: $minimum" }
            val multiplier = getMultiplier()
            val offset = getOffset()
            if (multiplier != 1.0 || offset != 0.0) {
                this.value = getConverter().convert(null, Math.round((value - offset) / multiplier))
            } else {
                this.value = getConverter().convert(null, value)
            }
        }
    }

    /**
     * Sets the field value into a new Long value.
     * @param value a new field value
     */
    fun setLong(value: Long?) {
        if (value == null) {
            this.value = null
        } else {
            val maximum: Double = field?.maximum ?: 0.0
            require(!(maximum != null && maximum < value)) { "Value [$value] is greater than maximum: $maximum" }
            val minimum: Double = field?.minimum ?: 0.0
            require(!(minimum != null && minimum > value)) { "Value [$value] is less than minimum: $minimum" }
            val multiplier = getMultiplier()
            val offset = getOffset()
            if (multiplier != 1.0 || offset != 0.0) {
                this.value = getConverter().convert(null, Math.round((value - offset) / multiplier))
            } else {
                this.value = getConverter().convert(null, value)
            }
        }
    }

    /**
     * Sets the field value into a new BigInteger value.
     * @param value a new field value
     */
    fun setBigInteger(value: BigInteger?) {
        if (value == null) {
            this.value = null
        } else {
            val vl = BigDecimal(value)
            val maximum: Double = field?.maximum ?: 0.0
            require(!(maximum != null && vl.compareTo(BigDecimal(maximum)) > 0)) { "Value [$value] is greater than maximum: $maximum" }
            val minimum: Double = field?.minimum ?: 0.0
            require(!(minimum != null && vl.compareTo(BigDecimal(minimum)) < 0)) { "Value [$value] is less than minimum: $minimum" }
            val multiplier = getMultiplier()
            val offset = getOffset()
            val adjusted: BigInteger
            adjusted = if (multiplier != 1.0 || offset != 0.0) {
                vl.subtract(BigDecimal.valueOf(offset)).setScale(0, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(multiplier)).toBigInteger()
            } else {
                value
            }
            if (field?.getFieldFormat()?.isStruct == true) {
                this.value = TwosComplementNumberFormatter().serialize(
                    adjusted,
                    adjusted.bitLength(), false
                ).toByteArray()
            } else {
                this.value = getConverter().convert(null, adjusted)
            }
        }
    }

    /**
     * Sets the field value into a new Float value.
     * @param value a new field value
     */
    fun setFloat(value: Float?) {
        if (value == null) {
            this.value = null
        } else {
            val maximum: Double = field?.maximum ?: 0.0
            require(!(maximum != null && maximum < value)) { "Value [$value] is greater than maximum: $maximum" }
            val minimum: Double = field?.minimum ?: 0.0
            require(!(minimum != null && minimum > value)) { "Value [$value] is less than minimum: $minimum" }
            this.value = getConverter().convert(null, (value - getOffset()) / getMultiplier())
        }
    }

    /**
     * Sets the field value into a new Double value.
     * @param value a new field value
     */
    fun setDouble(value: Double?) {
        if (value == null) {
            this.value = null
        } else {
            val maximum: Double = field?.maximum ?: 0.0
            require(!(maximum != null && maximum < value)) { "Value [$value] is greater than maximum: $maximum" }
            val minimum: Double = field?.minimum ?: 0.0
            require(!(minimum != null && minimum > value)) { "Value [$value] is less than minimum: $minimum" }
            this.value = getConverter().convert(null, (value - getOffset()) / getMultiplier())
        }
    }

    /**
     * Sets the field value into a new String value.
     * @param value a new field value
     */
    open fun setString(value: String?) {
        this.value = value
    }

    /**
     * Sets the field value to a "struct" (array) value from an array.
     * @param struct a new field value
     */
    fun setStruct(struct: ByteArray) {
        value = struct
    }

    /**
     * Sets the field value from the given enumeration (enumeration key).
     * @param value a new field value
     */
    fun setEnumeration(value: Enumeration?) {
        if (value == null) {
            this.value = null
        } else {
            val key: BigInteger? = value.key
            key?.let {
                if (field?.getFieldFormat()?.isStruct == true) {
                    this.value = TwosComplementNumberFormatter().serialize(key, key.bitLength(), false).toByteArray()
                } else if (field?.getFieldFormat()?.isString == true) {
                    val encoding = if (field?.getFieldFormat()!!.getType() === FieldType.UTF8S) "UTF-8" else "UTF-16"
                    try {
                        this.value = String(
                            TwosComplementNumberFormatter().serialize(key, key.bitLength(), false)
                                .toByteArray(), charset(encoding)
                        )
                    } catch (e: UnsupportedEncodingException) {
                        throw java.lang.IllegalStateException(e)
                    }
                } else {
                    setBigInteger(key)
                }
            }

        }
    }

    /**
     * Sets the field value to a raw value.
     * @param value a new field value
     */
    fun setRawValue(value: Any?) {
        this.value = value
    }


    /**
     * Checks whether field value is set.
     * @return true if field value is set, false otherwise
     */
    fun isValueSet(): Boolean {
        return value != null
    }

    override fun toString(): String {
        return getString()
    }

    private fun getMultiplier(): Double {
        var multiplier = 1.0
        if (field?.decimalExponent != null) {
            multiplier = 10.0.pow(field?.decimalExponent ?: 0)
        }
        if (field?.binaryExponent != null) {
            multiplier *= 2.0.pow(field?.binaryExponent ?: 0)
        }
        if (field?.multiplier != null && field?.multiplier !== 0) {
            multiplier *= field?.multiplier as Double
        }
        return multiplier
    }

    /**
     * Reads offset-to-be-added to field value received from request.
     * This is an extension to official GATT characteristic field specification,
     * allowing to implement subset of proprietary devices that almost follow standard
     * GATT specifications.
     * @return offset as double if set, 0 if not present
     */
    private fun getOffset(): Double {
        return field?.offset ?: 0.0
    }

    private fun getConverter(): AbstractConverter {
        val fieldFormat: FieldFormat? = field?.getFieldFormat()
        val size: Int = fieldFormat?.size ?: 0
        return when (fieldFormat?.getType()) {
            FieldType.BOOLEAN -> BooleanConverter()
            FieldType.UINT -> if (size < 32) {
                IntegerConverter()
            } else if (size < 64) {
                LongConverter()
            } else {
                BigIntegerConverter()
            }

            FieldType.SINT -> if (size <= 32) {
                IntegerConverter()
            } else if (size <= 64) {
                LongConverter()
            } else {
                BigIntegerConverter()
            }

            FieldType.FLOAT_IEE754, FieldType.FLOAT_IEE11073 -> if (size <= 32) FloatConverter() else DoubleConverter()
            FieldType.UTF8S, FieldType.UTF16S -> StringConverter()
            else -> throw java.lang.IllegalStateException("Unsupported field format: " + fieldFormat?.getType())
        }
    }

    private fun prepareValue(): Any? {
        return if (field?.getFieldFormat()?.isStruct == true && value is ByteArray) {
            val data = value as ByteArray
            TwosComplementNumberFormatter().deserializeBigInteger(
                BitSet.valueOf(data),
                data.size * 8, false
            )
        } else {
            value
        }
    }
}
