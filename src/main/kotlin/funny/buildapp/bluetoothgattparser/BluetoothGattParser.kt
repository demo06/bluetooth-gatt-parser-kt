package funny.buildapp.bluetoothgattparser

import funny.buildapp.bluetoothgattparser.spec.BluetoothGattSpecificationReader
import funny.buildapp.bluetoothgattparser.spec.Characteristic
import funny.buildapp.bluetoothgattparser.spec.Field
import funny.buildapp.bluetoothgattparser.spec.Service
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*

class BluetoothGattParser(
    private var specificationReader: BluetoothGattSpecificationReader,
    private var defaultParser: CharacteristicParser
) {
    private val logger = LoggerFactory.getLogger(GenericCharacteristicParser::class.java)
    private val customParsers: MutableMap<String, CharacteristicParser> = HashMap()


    /**
     * Checks whether a provided characteristic UUID is known by the parser.
     * @param characteristicUUID UUID of a GATT characteristic
     * @return true if the parser has loaded definitions for that characteristic, false otherwise
     */
    fun isKnownCharacteristic(characteristicUUID: String): Boolean {
        return specificationReader.getCharacteristicByUUID(getShortUUID(characteristicUUID)) != null
    }

    /**
     * Checks whether a provided service UUID is known by the parser.
     * @param serviceUUID UUID of a GATT service
     * @return true if the parser has loaded definitions for that service, false otherwise
     */
    fun isKnownService(serviceUUID: String): Boolean {
        return specificationReader.getService(getShortUUID(serviceUUID)) != null
    }

    /**
     * Performs parsing of a GATT characteristic value (byte array) into a user-friendly format
     * (a map of parsed characteristic fields represented by [GattResponse]).
     *
     * @param characteristicUUID UUID of a GATT characteristic
     * @param raw byte array of data received from bluetooth device
     * @return a map of parsed characteristic fields represented by [GattResponse]
     * @throws CharacteristicFormatException if a characteristic cannot be parsed
     */
    @Throws(CharacteristicFormatException::class)
    fun parse(characteristicUUID: String, raw: ByteArray): GattResponse {
        return GattResponse(parseFields(characteristicUUID, raw))
    }

    /**
     * Returns a list of fields represented by [GattRequest] for a write operation
     * (see [BluetoothGattParser.serialize]) of a specified GATT characteristic.
     * Some of the returned fields can be mandatory so they have to be set before serialization,
     * check [GattRequest.getRequiredFieldHolders] and [BluetoothGattParser.validate]
     *
     * @param characteristicUUID UUID of a GATT characteristic
     * @return list of fields represented by [GattRequest] for a write operation
     */
    fun prepare(characteristicUUID: String): GattRequest {
        var characteristicUUID = characteristicUUID
        characteristicUUID = getShortUUID(characteristicUUID)
        return GattRequest(
            characteristicUUID,
            specificationReader.getFields(specificationReader.getCharacteristicByUUID(characteristicUUID))
        )
    }

    /**
     * Returns a list of fields represented by [GattRequest] for a write operation
     * (see [BluetoothGattParser.serialize]) of a specified GATT characteristic which is to be
     * initialized with the provided initial data.
     * Some of the returned fields can be mandatory so they have to be set before serialization,
     * check [GattRequest.getRequiredFieldHolders] and [BluetoothGattParser.validate]
     *
     * @param characteristicUUID UUID of a GATT characteristic
     * @param initial initial data
     * @return list of fields represented by [GattRequest] for a write operation
     */
    fun prepare(characteristicUUID: String, initial: ByteArray): GattRequest {
        var characteristicUUID = characteristicUUID
        characteristicUUID = getShortUUID(characteristicUUID)
        return GattRequest(characteristicUUID, parseFields(characteristicUUID, initial))
    }

    /**
     * Performs serialization of a GATT request prepared by [BluetoothGattParser.prepare]
     * and filled by user (see [GattRequest.setField]) for a further communication to a bluetooth device.
     * Some of the fields can be mandatory so they have to be set before serialization,
     * check [GattRequest.getRequiredFieldHolders] and [BluetoothGattParser.validate].
     *
     * @param gattRequest a GATT request object
     * @return serialized fields as an array of bytes ready to send to a bluetooth device
     * @throws IllegalArgumentException if provided GATT request is not valid
     */
    fun serialize(gattRequest: GattRequest): ByteArray? {
        return serialize(gattRequest, true)
    }

    /**
     * Performs serialization of a GATT request prepared by [BluetoothGattParser.prepare]
     * and filled by user (see [GattRequest.setField]) for a further communication to a bluetooth device.
     * Some of the fields can be mandatory so they have to be set before serialization,
     * check [GattRequest.getRequiredFieldHolders] and [BluetoothGattParser.validate].
     *
     * @param gattRequest a GATT request object
     * @param strict dictates whether validation has to be performed before serialization
     * (see [BluetoothGattParser.validate])
     * @return serialized fields as an array of bytes ready to send to a bluetooth device
     * @throws IllegalArgumentException if provided GATT request is not valid and strict parameter is set to true
     */
    fun serialize(gattRequest: GattRequest, strict: Boolean): ByteArray? {
//        require(!(strict && !validate(gattRequest))) { "GATT request is not valid" }
        synchronized(customParsers) {
            val characteristicUUID = getShortUUID(gattRequest.characteristicUUID)
            if (strict && !isValidForWrite(characteristicUUID)) {
                throw CharacteristicFormatException(
                    "Characteristic is not valid for write: $characteristicUUID"
                )
            }
            return if (customParsers.containsKey(characteristicUUID)) {
                customParsers[characteristicUUID]!!.serialize(gattRequest.allFieldHolders)
            } else defaultParser.serialize(gattRequest.allFieldHolders)
        }
    }

    /**
     * Returns a GATT service specification by its UUID.
     * @param serviceUUID UUID of a GATT service
     * @return a GATT service specification by its UUID
     */
    fun getService(serviceUUID: String): Service? {
        return specificationReader.getService(getShortUUID(serviceUUID))
    }

    /**
     * Returns a GATT characteristic specification by its UUID.
     * @param characteristicUUID UUID of a GATT characteristic
     * @return a GATT characteristic specification by its UUID
     */
    fun getCharacteristic(characteristicUUID: String): Characteristic? {
        return specificationReader.getCharacteristicByUUID(getShortUUID(characteristicUUID))
    }

    /**
     * Returns a list of field specifications for a given characteristic.
     * Note that field references are taken into account. Referencing fields are not returned,
     * referenced fields returned instead (see [Field.getReference]).
     *
     * @param characteristicUUID UUID of a GATT characteristic
     * @return a list of field specifications for a given characteristic
     */
    fun getFields(characteristicUUID: String): List<Field> {
        return specificationReader.getFields(getCharacteristic(getShortUUID(characteristicUUID)))
    }

    /**
     * Registers a new characteristic parser (see [CharacteristicParser]) for a given characteristic.
     * @param characteristicUUID UUID of a GATT characteristic
     * @param parser a new instance of a characteristic parser
     */
    fun registerParser(characteristicUUID: String, parser: CharacteristicParser?) {
        synchronized(customParsers) {
            if (parser != null) {
                customParsers[getShortUUID(characteristicUUID)] = parser
            }
        }
    }

    /**
     * Checks whether a given characteristic is valid for read operation
     * (see [BluetoothGattParser.parse]).
     * Note that not all standard and approved characteristics are valid for automatic read operations due to
     * malformed or incorrect GATT XML specification files.
     *
     * @param characteristicUUID UUID of a GATT characteristic
     * @return true if a given characteristic is valid for read operation
     */
    fun isValidForRead(characteristicUUID: String): Boolean {
        val characteristic: Characteristic? =
            specificationReader.getCharacteristicByUUID(getShortUUID(characteristicUUID))
        return characteristic != null && characteristic.isValidForRead
    }

    /**
     * Checks whether a given characteristic is valid for write operation
     * (see [BluetoothGattParser.serialize]).
     * Note that not all standard and approved characteristics are valid for automatic write operations due to
     * malformed or incorrect GATT XML specification files.
     *
     * @param characteristicUUID UUID of a GATT characteristic
     * @return true if a given characteristic is valid for write operation
     */
    fun isValidForWrite(characteristicUUID: String): Boolean {
        val characteristic: Characteristic? =
            specificationReader.getCharacteristicByUUID(getShortUUID(characteristicUUID))
        return characteristic != null && characteristic.isValidForWrite
    }

    /**
     * Checks if a GATT request object has all mandatory fields set (see [BluetoothGattParser.prepare]).
     *
     * @param gattRequest a GATT request object
     * @return true if a given GATT request is valid for write operation
     * (see [BluetoothGattParser.serialize])
     */
    fun validate(gattRequest: GattRequest): Boolean {
        val controlPointField: FieldHolder = gattRequest.getOpCodesFieldHolder()
        val requirement: String? = if (controlPointField != null) controlPointField.getEnumerationRequires() else null
        if (requirement != null) {
            val required = gattRequest.getRequiredHolders(requirement)
            if (required.isEmpty()) {
                logger.info("GATT request is invalid; could not find any field by requirement: {}", requirement)
                return false
            }
            for (holder in required) {
                if (!holder!!.isValueSet()) {
                    logger.info("GATT request is invalid; field is not set: {}", holder.getField()?.getName())
                    return false
                }
            }
        }
        for (holder in gattRequest.getRequiredHolders("Mandatory")) {
            if (!holder!!.isValueSet()) {
                logger.info("GATT request is invalid; field is not set: {}", holder.getField()?.getName())
                return false
            }
        }
        return true
    }

    /**
     * This method is used to load/register custom services and characteristics
     * (defined in GATT XML specification files,
     * see an example [here](https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.battery_level.xml))
     * from a folder. The folder must contain two sub-folders for services and characteristics respectively:
     * "path"/service and "path"/characteristic. It is also possible to override existing services and characteristics
     * by matching UUIDs of services and characteristics in the loaded files.
     * @param path a root path to a folder containing definitions for custom services and characteristics
     */
    fun loadExtensionsFromFolder(path: String?) {
        specificationReader.loadExtensionsFromFolder(path!!)
    }

    /**
     * This method is used to load/register custom services and characteristics
     * (defined in GATT XML specification files,
     * see an example [here](https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.battery_level.xml))
     * from a resource URLs. The URLs must point to json object, holding filenames (types) of gatt xml specs as values
     * and their short uuid's as keys.
     * @param servicesCatalogResource a path to a folder containing definitions for custom services
     * @param characteristicsCatalogResource a path to a folder containing definitions for custom characteristics
     * @throws IllegalStateException when either argument is null
     */
    @Throws(IllegalStateException::class)
    fun loadExtensionsFromCatalogResources(servicesCatalogResource: URL?, characteristicsCatalogResource: URL?) {
        specificationReader.loadExtensionsFromCatalogResources(
            servicesCatalogResource!!,
            characteristicsCatalogResource!!
        )
    }

    /**
     * Returns text representation of the provided array of bytes. Example: [01, 05, ab]
     * @param raw bytes array
     * @param radix the radix to use in the string representation
     * @return array text representation
     */
    private fun parse(raw: ByteArray, radix: Int): String {
        val hexFormatted = arrayOfNulls<String>(raw.size)
        for ((index, b) in raw.withIndex()) {
            val num = Integer.toUnsignedString(java.lang.Byte.toUnsignedInt(b), radix)
            hexFormatted[index] = "00$num".substring(num.length)
        }
        return hexFormatted.contentToString()
    }

    /**
     * Serializes a string that represents an array of bytes (comma separated, e.g: [01, 05, ab]),
     * see ([.parse]).
     * @param raw a string representing an array of bytes
     * @param radix the radix to use in the string representation
     * @return serialized array
     */
    private fun serialize(raw: String, radix: Int): ByteArray {
        val data = raw.replace("[", "").replace("]", "")
        val tokens = data.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val bytes = ByteArray(tokens.size)
        for (i in tokens.indices) {
            bytes[i] = tokens[i].trim { it <= ' ' }.toInt(radix).toByte()
        }
        return bytes
    }

    private fun getShortUUID(uuid: String): String {
        return if (uuid.length < 8) {
            uuid.uppercase(Locale.getDefault())
        } else java.lang.Long.toHexString(uuid.substring(0, 8).toLong(16)).uppercase(Locale.getDefault())
    }

    private fun parseFields(characteristicUUID: String, raw: ByteArray): LinkedHashMap<String, FieldHolder> {
        var characteristicUUID = characteristicUUID
        characteristicUUID = getShortUUID(characteristicUUID)
        synchronized(customParsers) {
            if (!isValidForRead(characteristicUUID)) {
                throw CharacteristicFormatException("Characteristic is not valid for read: $characteristicUUID")
            }
            val characteristic: Characteristic? = specificationReader.getCharacteristicByUUID(characteristicUUID)
            return if (customParsers.containsKey(characteristicUUID)) {
                customParsers[characteristicUUID]?.parse(characteristic, raw)
                    ?: defaultParser.parse(characteristic, raw)
            } else defaultParser.parse(characteristic, raw)
        }
    }

}
