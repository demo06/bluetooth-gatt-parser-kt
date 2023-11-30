package gattparser.spec;

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

import funny.buildapp.bluetoothgattparser.spec.FieldFormat;
import funny.buildapp.bluetoothgattparser.spec.FieldType;
import org.junit.Test;

import static org.gradle.internal.impldep.org.junit.Assert.assertEquals;

public class FieldFormatTest {

    @Test
    public void testValueOf() throws Exception {
        assertFieldType("boolean", FieldType.BOOLEAN, 1, FieldFormat.Companion.valueOf("bOoLean"));
        assertFieldType("nibble", FieldType.UINT, 4, FieldFormat.Companion.valueOf("nIbblE"));
        assertFieldType("float32", FieldType.FLOAT_IEE754, 32, FieldFormat.Companion.valueOf("fLoat32"));
        assertFieldType("float64", FieldType.FLOAT_IEE754, 64, FieldFormat.Companion.valueOf("fLoAt64"));
        assertFieldType("SFLOAT", FieldType.FLOAT_IEE11073, 16, FieldFormat.Companion.valueOf("SFLOAT"));
        assertFieldType("FLOAT", FieldType.FLOAT_IEE11073, 32, FieldFormat.Companion.valueOf("FLOAT"));
        //assertFieldType("duint16", FieldType.UINT, 16, FieldFormat.Companion.valueOf("duint16"));
        assertFieldType("utf8s", FieldType.UTF8S, FieldFormat.FULL_SIZE, FieldFormat.Companion.valueOf("uTf8s"));
        assertFieldType("utf16s", FieldType.UTF16S, FieldFormat.FULL_SIZE, FieldFormat.Companion.valueOf("Utf16s"));

        assertFieldType("7bit", FieldType.UINT, 7, FieldFormat.Companion.valueOf("7Bit"));
        assertFieldType("700bit", FieldType.UINT, 700, FieldFormat.Companion.valueOf("700biT"));
        assertFieldType("uint10", FieldType.UINT, 10, FieldFormat.Companion.valueOf("uiNT10"));
        assertFieldType("uint3", FieldType.UINT, 3, FieldFormat.Companion.valueOf("uiNT3"));

        assertFieldType("sint3", FieldType.SINT, 3, FieldFormat.Companion.valueOf("siNT3"));
        assertFieldType("sint65", FieldType.SINT, 65, FieldFormat.Companion.valueOf("siNT65"));
    }

    @Test(expected = IllegalStateException.class)
    public void testValueOfInvalidFormat() {
        FieldFormat.Companion.valueOf("siNTunknown");
    }

    @Test
    public void testIsReal() {
        assertIsReal(false, "sfloat", "float", "float32", "float64", "boolean", "utf8s", "utf16s", "struct");
        assertIsReal(true, "nibble", "uint2", "uint64", "sint2");
    }

    @Test
    public void testIsDecimal() {
        assertIsDecimal(false, "nibble", "uint2", "uint64", "sint2", "boolean", "utf8s", "utf16s", "struct");
        assertIsDecimal(true, "sfloat", "float", "float32", "float64");
    }

    @Test
    public void testIsBoolean() {
        assertIsBoolean(false, "nibble", "uint2", "uint64", "sint2", "utf8s", "utf16s", "struct", "sfloat",
                "float", "float32", "float64");
        assertIsBoolean(true, "boolean");
    }

    @Test
    public void testIsNumber() {
        assertIsNumber(false, "boolean", "utf8s", "utf16s", "struct");
        assertIsNumber(true, "nibble", "uint2", "uint64", "sint2", "sfloat", "float", "float32", "float64");
    }

    @Test
    public void testIsString() {
        assertIsString(false, "nibble", "uint2", "uint64", "sint2", "sfloat", "float", "float32", "float64",
                "boolean", "struct");
        assertIsString(true, "utf8s", "utf16s");
    }

    @Test
    public void testIsStruct() {
        assertIsStruct(false, "nibble", "uint2", "uint64", "sint2", "sfloat", "float", "float32", "float64",
                "boolean", "utf8s", "utf16s");
        assertIsStruct(true, "struct");
    }

    private void assertFieldType(String name, FieldType fieldType, int size, FieldFormat actual) {
        assertEquals(name, actual.getName());
        assertEquals(fieldType, actual.getType());
        assertEquals(size, actual.getSize());
    }

    private void assertIsReal(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.Companion.valueOf(format).isReal());
        }
    }

    private void assertIsDecimal(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.Companion.valueOf(format).isDecimal());
        }
    }

    private void assertIsBoolean(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.Companion.valueOf(format).isBoolean());
        }
    }

    private void assertIsNumber(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.Companion.valueOf(format).isNumber());
        }
    }

    private void assertIsString(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.Companion.valueOf(format).isString());
        }
    }

    private void assertIsStruct(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.Companion.valueOf(format).isStruct());
        }
    }

}
