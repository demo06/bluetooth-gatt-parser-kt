package funny.buildapp.bluetoothgattparser

import funny.buildapp.bluetoothgattparser.spec.Characteristic


/**
 * A root interface for all GATT characteristic parsers in the framework. It defines simple read and write operations.
 *
 * @author Vlad Kolotov
 */
interface CharacteristicParser {
    /**
     * Read operation. This method reads raw data and converts it to a user-friends format: a map of parsed
     * characteristic field holders. The order of fields is guaranteed by [LinkedHashMap]
     *
     * @param characteristic an instance of characteristic specification object
     * @param raw byte array of data received from bluetooth device
     * @return a map of parsed characteristic fields
     * @throws CharacteristicFormatException if provided data cannot be parsed,
     * see [BluetoothGattParser.isValidForRead]
     */
    @Throws(CharacteristicFormatException::class)
    fun parse(characteristic: Characteristic?, raw: ByteArray): LinkedHashMap<String, FieldHolder>

    /**
     * Write operation. This method serialises characteristic fields into a raw array of bytes ready to send
     * to a bluetooth device.
     *
     * @param fieldHolders a collection of field holders populated with user input
     * @return a raw array of bytes which is ready to be sent to a bluetooth device
     * @throws CharacteristicFormatException if provided fields cannot be serialized,
     * see [BluetoothGattParser.isValidForWrite]
     */
    @Throws(CharacteristicFormatException::class)
    fun serialize(fieldHolders: List<FieldHolder>): ByteArray?
}
