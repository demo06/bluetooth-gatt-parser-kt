package funny.buildapp.bluetoothgattparser

import funny.buildapp.bluetoothgattparser.spec.BitField
import funny.buildapp.bluetoothgattparser.spec.Enumeration
import funny.buildapp.bluetoothgattparser.spec.Field
import funny.buildapp.bluetoothgattparser.spec.FlagUtils
import java.math.BigInteger
import java.util.*
import java.util.function.BiConsumer

/*-
 * #%L
 * org.sputnikdev:bluetooth-gatt-parser
 * %%
 * Copyright (C) 2017 Sputnik Dev
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * Defines an object for capturing field values of a Bluetooth GATT characteristic. A GattRequest provides some methods
 * for identifying mandatory and optional fields as well as some convenient methods for capturing field values.
 *
 * @author Vlad Kolotov
 */
class GattRequest {
    /**
     * Returns associated to this request GATT characteristic UUID.
     * @return GATT characteristic UUID
     */
    val characteristicUUID: String

    /**
     * Returns map of holders with preserved order of fields.
     * @return map of holders with preserved order of fields
     */
    val holders: Map<String, FieldHolder>

    /**
     * Returns operational codes field holder (if exists) for a given GATT characteristic.
     * Normally this field is used to identify a list of mandatory fields based on its value and
     * control field GATT specification, see [Field.getEnumerations]
     * and [FieldHolder.getEnumerationRequires] and [GattRequest.getRequiredFieldHolders].
     * @return an operational codes field
     */
    private val opCodesHolder: FieldHolder?

    /**
     * Creates a GATT request for a given GATT characteristic and its fields.
     * @param characteristicUUID an UUID of a characteristic
     * @param fields a list of characteristic fields
     */
    internal constructor(characteristicUUID: String, fields: List<Field>) {
        require(fields.isNotEmpty()) { "Fields cannot be empty" }
        this.characteristicUUID = characteristicUUID
        holders = getHolders(fields)
        opCodesHolder = findOpCodesField()
    }

    /**
     * Creates a GATT request for a given GATT characteristic and its field holders.
     * @param characteristicUUID an UUID of a characteristic
     * @param holders a list of characteristic field hodlers
     */
    internal constructor(characteristicUUID: String, holders: Map<String, FieldHolder>) {
        require(holders.isNotEmpty()) { "Fields cannot be empty" }
        this.characteristicUUID = characteristicUUID
        this.holders = HashMap<String, FieldHolder>(holders)
        opCodesHolder = findOpCodesField()
    }

    /**
     * Returns operational codes field holder (if exists) for a given GATT characteristic.
     * Normally this field is used to identify a list of mandatory fields based on its value and
     * control field GATT specification, see [Field.getEnumerations]
     * and [FieldHolder.getEnumerationRequires] and [GattRequest.getRequiredFieldHolders].
     * @return an operational codes field
     */
    fun getOpCodesFieldHolder(): FieldHolder {
        return opCodesHolder!!
    }


    /**
     * Checks whether an OpCodes field exists.
     * @return true if an OpCodes field exists
     */
    fun hasOpCodesField(): Boolean {
        return opCodesHolder != null
    }


    /**
     * Sets a Boolean value for a field by its name.
     * @param name field name
     * @param value field value
     */
    fun setField(name: String, value: Boolean?) {
        validate(name)
        holders[name]?.setBoolean(value)
    }

    /**
     * Sets an Integer value for a field by its name.
     * @param name field name
     * @param value field value
     */
    fun setField(name: String, value: Int) {
        setField<Int>(name, FieldHolder::setInteger, value)
    }

    /**
     * Sets a Long value for a field by its name.
     * @param name field name
     * @param value field value
     */
    fun setField(name: String, value: Long) {
        setField<Long>(name, FieldHolder::setLong, value)
    }

    /**
     * Sets a BigInteger value for a field by its name.
     * @param name field name
     * @param value field value
     */
    fun setField(name: String, value: BigInteger) {
        setField<BigInteger>(name, FieldHolder::setBigInteger, value)
    }

    /**
     * Sets a Float value for a field by its name.
     * @param name field name
     * @param value field value
     */
    fun setField(name: String, value: Float) {
        setField<Float>(name, FieldHolder::setFloat, value)
    }

    /**
     * Sets a Double value for a field by its name.
     * @param name field name
     * @param value field value
     */
    fun setField(name: String, value: Double) {
        setField<Double>(name, FieldHolder::setDouble, value)
    }

    /**
     * Sets the field value from the given enumeration (enumeration key).
     * @param name field name
     * @param value field value
     */
    fun setField(name: String, value: Enumeration) {
        setField(name, FieldHolder::setEnumeration, value)
    }

    /**
     * Sets a new struct value for a field by its name.
     * @param name field name
     * @param value field value
     */
    fun setField(name: String, value: ByteArray) {
        setField(name, FieldHolder::setStruct, value)
    }

    /**
     * Sets a String value for a field by its name.
     * @param name field name
     * @param value field value
     */
    fun setField(name: String, value: String) {
        val field = FieldHolder()
        setField(name, FieldHolder::setString, value)
    }

    val allFieldHolders: List<FieldHolder>
        /**
         * Returns a list of all fields.
         * @return a list of all fields
         */
        get() = ArrayList(holders.values)

    /**
     * Returns a list of mandatory fields only.
     * @return a list of mandatory fields only
     */
    fun getRequiredFieldHolders(): List<FieldHolder?> {
        val controlPointField: FieldHolder = getOpCodesFieldHolder()
        val requirement: String? = controlPointField.getEnumerationRequires()
        val required: MutableList<FieldHolder?> = ArrayList()
        required.addAll(getRequiredHolders("Mandatory"))
        if (requirement != null) {
            required.addAll(getRequiredHolders(requirement))
        }
        return required.toList()
    }


    /**
     * Returns a field holder by its field name.
     * @param name requested field name
     * @return a field holder
     */
    fun getFieldHolder(name: String): FieldHolder? {
        validate(name)
        return holders[name]
    }

    /**
     * Checks whether a field is present in the request.
     * @param name field name
     * @return true if present, false otherwise
     */
    fun hasFieldHolder(name: String?): Boolean {
        return holders.containsKey(name)
    }

    fun getRequiredHolders(requirement: String?): List<FieldHolder?> {
        val result: MutableList<FieldHolder?> = ArrayList()
        for (holder in holders.values) {
            if (holder?.getField()?.requirements?.contains(requirement) == true) {
                result.add(holder)
            }
        }
        return result
    }


    private fun validate(name: String) {
        require(holders.containsKey(name)) { "Unknown field: $name" }
    }

    private fun findOpCodesField(): FieldHolder? {
        return holders.values.stream()
            .filter { field: FieldHolder? ->
                field?.getField()?.let { FlagUtils.isOpCodesField(it) } ?: false
            }
            .findFirst().orElse(null)
    }

    private fun getHolders(fields: List<Field>): Map<String, FieldHolder> {
        val result: MutableMap<String?, FieldHolder> = LinkedHashMap()
        for (field in fields) {
            result[field.getName()] = FieldHolder(field)
        }
        return Collections.unmodifiableMap(result)
    }


    private fun setOpCode(requirements: List<String>?) {
        if (!requirements.isNullOrEmpty()) {
            if (opCodesHolder != null) {
                val bitField: BitField? = opCodesHolder.getField()?.bitField
                if (bitField == null) {
                    requirements.stream()
                        .filter { req: String? -> !"Mandatory".equals(req, ignoreCase = true) }
                        .findFirst().ifPresent { requirement: String ->
                            opCodesHolder.getField()?.enumerations?.enumerations?.stream()
                                ?.filter { enm -> requirement.equals(enm.requires, ignoreCase = true) }
                                ?.findFirst()?.ifPresent { enm -> opCodesHolder.setBigInteger(enm.key) }
                        }

                } else {
                    //TODO handle bitmap
                }
            }
        }
    }

    private fun <T> setField(name: String, setter: BiConsumer<FieldHolder, T>, value: T) {
        validate(name)
        val fieldHolder = holders[name]
        if (fieldHolder != null) {
            setter.accept(fieldHolder, value)
        }
        setOpCode(fieldHolder!!.getField()?.requirements)
    }
}
