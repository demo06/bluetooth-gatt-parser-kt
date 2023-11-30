package funny.buildapp.bluetoothgattparser

import funny.buildapp.bluetoothgattparser.num.*
import funny.buildapp.bluetoothgattparser.spec.BluetoothGattSpecificationReader

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
 * A factory class for some main objects in the library:
 * [BluetoothGattParser], [BluetoothGattSpecificationReader].
 *
 * @author Vlad Kolotov
 */
object BluetoothGattParserFactory {
    private val TWOS_COMPLEMENT_NUMBER_FORMATTER: RealNumberFormatter = TwosComplementNumberFormatter()
    private val IEEE_754_FLOATING_POINT_NUMBER_FORMATTER: FloatingPointNumberFormatter =
        IEEE754FloatingPointNumberFormatter()
    private val IEEE_11073_FLOATING_POINT_NUMBER_FORMATTER: FloatingPointNumberFormatter =
        IEEE11073FloatingPointNumberFormatter()

    @Volatile
    private var reader: BluetoothGattSpecificationReader? = null

    @Volatile
    private var defaultParser: BluetoothGattParser? = null
    val specificationReader: BluetoothGattSpecificationReader?
        /**
         * Returns GATT specification reader.
         *
         * @return GATT specification reader
         */
        get() {
            if (reader == null) {
                synchronized(BluetoothGattParserFactory::class.java) {
                    if (reader == null) {
                        reader = BluetoothGattSpecificationReader()
                    }
                }
            }
            return reader
        }
    val default: BluetoothGattParser?
        /**
         * Returns Bluetooth GATT parser.
         * @return Bluetooth GATT parser
         */
        get() {
            if (defaultParser == null) {
                synchronized(BluetoothGattParserFactory::class.java) {
                    if (defaultParser == null) {
                        val reader: BluetoothGattSpecificationReader? = specificationReader
                        reader?.let {
                            defaultParser = BluetoothGattParser(reader, GenericCharacteristicParser(reader))
                        }

                    }
                }
            }
            return defaultParser
        }
    val twosComplementNumberFormatter: RealNumberFormatter
        /**
         * Returns two's complement number formatter.
         * @return two's complement number formatter
         */
        get() = TWOS_COMPLEMENT_NUMBER_FORMATTER
    val iEEE754FloatingPointNumberFormatter: FloatingPointNumberFormatter
        /**
         * Returns IEEE754 floating point number formatter.
         * @return IEEE754 floating point number formatter
         */
        get() = IEEE_754_FLOATING_POINT_NUMBER_FORMATTER
    val iEEE11073FloatingPointNumberFormatter: FloatingPointNumberFormatter
        /**
         * Returns IEEE11073 floating point number formatter.
         * @return IEEE11073 floating point number formatter
         */
        get() = IEEE_11073_FLOATING_POINT_NUMBER_FORMATTER
}
