/*
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Foundation
import XCTest
@testable import Wire

final class ProtoWriterTests: XCTestCase {

    // MARK: - Tests - Encoding Integers

    func testEncodeFixedInt32() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Int32(-5), encoding: .fixed)

        // 0D is (tag 1 << 3 | .fixed32)
        assertBufferEqual(writer, "0D_FBFFFFFF")
    }

    func testEncodeFixedInt64() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Int64(-5), encoding: .fixed)

        // 09 is (tag 1 << 3 | .fixed64)
        assertBufferEqual(writer, "09_FBFFFFFFFFFFFFFF")
    }

    func testEncodeFixedUInt32() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: UInt32(5), encoding: .fixed)

        // 0D is (tag 1 << 3 | .fixed32)
        assertBufferEqual(writer, "0D_05000000")
    }

    func testEncodeFixedUInt64() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: UInt64(5), encoding: .fixed)

        // 09 is (tag 1 << 3 | .fixed64)
        assertBufferEqual(writer, "09_0500000000000000")
    }

    func testEncodeSignedInt32() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Int32(-5), encoding: .signed)

        // 08 is (tag 1 << 3 | .varint)
        assertBufferEqual(writer, "08_09")
    }

    func testEncodeSignedInt64() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Int64(-5), encoding: .signed)

        // 08 is (tag 1 << 3 | .varint)
        assertBufferEqual(writer, "08_09")
    }

    func testEncodeVarintInt32() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Int32(5), encoding: .variable)

        // 08 is (tag 1 << 3 | .varint)
        assertBufferEqual(writer, "08_05")
    }

    func testEncodeVarintNegativeInt32() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Int32(-5), encoding: .variable)

        // 08 is (tag 1 << 3 | .varint)
        assertBufferEqual(writer, "08_FBFFFFFFFFFFFFFFFF01")
    }

    func testEncodeVarintNegativeInt64() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Int64(-5), encoding: .variable)

        // 08 is (tag 1 << 3 | .varint)
        assertBufferEqual(writer, "08_FBFFFFFFFFFFFFFFFF01")
    }

    func testEncodeVarintUInt32() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: UInt32(5), encoding: .variable)

        // 08 is (tag 1 << 3 | .varint)
        assertBufferEqual(writer, "08_05")
    }

    func testEncodeVarintUInt64() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: UInt64(5), encoding: .variable)

        // 08 is (tag 1 << 3 | .varint)
        assertBufferEqual(writer, "08_05")
    }

    // MARK: - Tests - Encoding Floats

    func testEncodeDouble() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Double(1.2345))

        // 09 is (tag 1 << 3 | .fixed64)
        assertBufferEqual(writer, "09_8D976E1283C0F33F")
    }

    func testEncodeFloat() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Float(1.2345))

        // 0D is (tag 1 << 3 | .fixed32)
        assertBufferEqual(writer, "0D_19049E3F")
    }

    // MARK: - Tests - Encoding Default Proto3 Values

    func testEncodeDefaultProto2Values() throws {
        let writer = ProtoWriter()
        let proto = SimpleOptional2 {
            $0.opt_int32 = 0
            $0.opt_int64 = 0
            $0.opt_uint32 = 0
            $0.opt_uint64 = 0
            $0.opt_float = 0
            $0.opt_double = 0
            $0.opt_bytes = .init()
            $0.opt_string = ""
            $0.opt_enum = .UNKNOWN
            $0.repeated_int32 = []
            $0.repeated_string = []
            $0.map_int32_string = [:]
        }
        try writer.encode(tag: 1, value: proto)

        // All values are encoded in proto2, including defaults.
        assertBufferEqual(writer, """
            0A1C                // Message tag and length
            0800                // tag 1 and value: opt_int32
            1000                // tag 2 and value: opt_int64
            1800                // tag 3 and value: opt_uint32
            2000                // tag 4 and value: opt_uint64
            2D00000000          // tag 5 and value: opt_float
            310000000000000000  // tag 6 and value: opt_float
            3A00                // tag 7 and length 0: opt_bytes
            4200                // tag 8 and length 0: opt_string
            4800                // tag 9 and value: opt_enum
        """)
    }

    // Re-enable this test when the Wire compiler properly outputs
    // nullable types for optional proto3 fields.
//    func testEncodeOptionalDefaultProto3Values() throws {
//        let writer = ProtoWriter()
//        let proto = SimpleOptional3(
//            opt_int32: 0,
//            opt_int64: 0,
//            opt_uint32: 0,
//            opt_uint64: 0,
//            opt_float: 0,
//            opt_double: 0
//        )
//        try writer.encode(tag: 1, value: proto)
//
//        // All values are encoded for optional fields in proto3, even ones matching defaults.
//        assertBufferEqual(writer,  """
//            0A16                // Message tag and length
//            0800                // tag 1 and value: opt_int32
//            1000                // tag 2 and value: opt_int64
//            1800                // tag 3 and value: opt_uint32
//            2000                // tag 4 and value: opt_uint64
//            2D00000000          // tag 5 and value: opt_float
//            310000000000000000  // tag 6 and value: opt_float
//        """)
//    }

    func testEncodeRequiredDefaultProto3Values() throws {
        let writer = ProtoWriter()
        let proto = SimpleRequired3(
            req_int32: 0,
            req_int64: 0,
            req_uint32: 0,
            req_uint64: 0,
            req_float: 0,
            req_double: 0,
            req_bytes: .init(),
            req_string: "",
            req_enum: .UNKNOWN
        ) {
            $0.repeated_int32 = []
            $0.repeated_string = []
            $0.map_int32_string = [:]
        }
        try writer.encode(tag: 1, value: proto)

        // No data should be encoded. Just the top-level message tag with a length of zero.
        assertBufferEqual(writer, "0A00")
    }

    // MARK: - Tests - Encoding Messages And More

    func testEncodeBool() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: true)

        // 08 is (tag 1 << 3 | .varint)
        assertBufferEqual(writer, "08_01")
    }

    func testEncodeData() throws {
        let writer = ProtoWriter()
        let data = Foundation.Data(hexEncoded: "001122334455")
        try writer.encode(tag: 1, value: data)

        assertBufferEqual(writer, """
            0A           // (Tag 1 | Length Delimited)
            06           // Length 6
            001122334455 // Data value
        """)
    }

    func testEncodeMessage() throws {
        let writer = ProtoWriter()
        let data = Data(json_data: "12")
        let message = Person(name: "Luke", id: 5, data: data)
        try writer.encode(tag: 1, value: message)

        assertBufferEqual(writer, """
            0A       // (Tag 1 | Length Delimited)
            0E       // Length 10 for Person
              0A       // (Tag 1 | Length Delimited)
              04       // Length 4 for name
              4C756B65 // UTF-8 Value "Luke"
              10       // (Tag 2 | Varint)
              05       // Value 5
              3A       // (Tag 7 | Data)
                04       // Length 4 for Data
                0A       // (Tag 1 | Length Delimited)
                02       // Length 2 for json_data
                3132     // UTF-8 Value '12'
        """)
    }

    func testEncodeMessageWithStackExpansion() throws {
        let writer = ProtoWriter()
        let message = Nested1(name: "name1") { v1 in
          v1.nested = Nested2(name: "name2") { v2 in
            v2.nested = Nested3(name: "name3") { v3 in
              v3.nested = Nested4(name: "name4") { v4 in
                v4.nested = Nested5(name: "name5") { v5 in
                  v5.nested = Nested6(name: "name6")
                }
              }
            }
          }
        }

        try writer.encode(tag: 1, value: message)

        assertBufferEqual(writer, """
            0A       // (Tag 1 | Length Delimited)
            34       // Length 52 for Nested1
              0A         // (Tag 1 | Length Delimited)
              05         // Length 5 for name1
              6E616D6531 // UTF-8 Value "name1"
              12         // (Tag 2 | Length Delimited)
              2B         // Length 43
                0A         // (Tag 1 | Length Delimited)
                05         // Length 5 for name2
                6E616D6532 // UTF-8 Value "name2"
                12         // (Tag 2 | Length Delimited)
                22         // Length 34
                  0A         // (Tag 1 | Length Delimited)
                  05         // Length 5 for name3
                  6E616D6533 // UTF-8 Value "name3"
                  12         // (Tag 2 | Length Delimited)
                  19         // Length 25
                    0A         // (Tag 1 | Length Delimited)
                    05         // Length 5 for name4
                    6E616D6534 // UTF-8 Value "name4"
                    12         // (Tag 2 | Length Delimited)
                    10         // Length 16
                      0A         // (Tag 1 | Length Delimited)
                      05         // Length 5 for name5
                      6E616D6535 // UTF-8 Value "name5" 
                      12         // (Tag 2 | Length Delimited)
                      07         // Length 7
                        0A         // (Tag 1 | Length Delimited)
                        05         // Length 5 for name6
                        6E616D6536 // UTF-8 Value "name6"
        """)
    }

    func testEncodeString() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: "foo")

        assertBufferEqual(writer, """
            0A     // (Tag 1 | Length Delimited)
            03     // Length 3
            666F6F // Value "foo"
        """)
    }

    // MARK: - Tests - Encoding Enums

    func testEncodeEnum() throws {
        let writer = ProtoWriter()
        let value: Person.PhoneType = .HOME
        try writer.encode(tag: 1, value: value)

        assertBufferEqual(writer, "08_01")
    }

    // MARK: - Tests - Encoding Proto3 Well-Known Types

    func testEncodeBoolValue() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: true, boxed: true)

        assertBufferEqual(writer, """
            0A   // (Tag 1 | Length Delimited)
            02   // Length 2
            08   // (Tag 1 | Varint)
            01   // Value "true"
        """)
    }

    func testEncodeNilBoolValue() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: nil as Bool?, boxed: true)

        assertBufferEqual(writer, "")
    }

    func testEncodeBytesValue() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Foundation.Data(hexEncoded: "001122334455")!, boxed: true)

        assertBufferEqual(writer, """
            0A           // (Tag 1 | Length Delimited)
            08           // Length 8
            0A           // (tag 1 | Length Delimited)
            06           // Data length 6
            001122334455 // Random data
        """)
    }

    func testEncodeNilBytesValue() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: nil as Foundation.Data?, boxed: true)

        assertBufferEqual(writer, "")
    }

    func testEncodeDoubleValue() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Double(1.2345), boxed: true)

        assertBufferEqual(writer, """
            0A                // (Tag 1 | Length Delimited)
            09                // Length 9
            09                // (Tag 1 | Fixed64)
            8D976E1283C0F33F  // Value 1.2345
        """)
    }

    func testEncodeNilDoubleValue() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: nil as Double?, boxed: true)

        assertBufferEqual(writer, "")
    }

    func testEncodeFloatValue() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Float(1.2345), boxed: true)

        assertBufferEqual(writer, """
            0A        // (Tag 1 | Length Delimited)
            05        // Length 9
            0D        // (Tag 1 | Fixed32)
            19049E3F  // Value 1.2345
        """)
    }

    func testEncodeNilFloatValue() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: nil as Float?, boxed: true)

        assertBufferEqual(writer, "")
    }

    func testEncodeInt32Value() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Int32(-5), boxed: true)

        assertBufferEqual(writer, """
            0A                   // (Tag 1 | Length Delimited)
            0B                   // Length 11
            08                   // (Tag 1 | Varint)
            FBFFFFFFFFFFFFFFFF01 // Value -5
        """)
    }

    func testEncodeNilInt32Value() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: nil as Int32?, boxed: true)

        assertBufferEqual(writer, "")
    }

    func testEncodeInt64Value() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: Int64(-5), boxed: true)

        assertBufferEqual(writer, """
            0A                   // (Tag 1 | Length Delimited)
            0B                   // Length 11
            08                   // (Tag 1 | Varint)
            FBFFFFFFFFFFFFFFFF01 // Value -5
        """)
    }

    func testEncodeNilInt64Value() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: nil as Int64?, boxed: true)

        assertBufferEqual(writer, "")
    }

    func testEncodeStringValue() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: "foo", boxed: true)

        assertBufferEqual(writer, """
            0A     // (Tag 1 | Length Delimited)
            05     // Length 5
            0A     // (tag 1 | Length Delimited)
            03     // String length 3
            666F6F // "foo"
        """)
    }

    func testEncodeNilStringValue() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: nil as String?, boxed: true)

        assertBufferEqual(writer, "")
    }

    func testEncodeUInt32Value() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: UInt32(5), boxed: true)

        assertBufferEqual(writer, """
            0A // (Tag 1 | Length Delimited)
            02 // Length 2
            08 // (Tag 1 | Varint)
            05 // Value 5
        """)
    }

    func testEncodeNilUInt32Value() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: nil as UInt32?, boxed: true)

        assertBufferEqual(writer, "")
    }

    func testEncodeUInt64Value() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: UInt64(5), boxed: true)

        assertBufferEqual(writer, """
            0A // (Tag 1 | Length Delimited)
            02 // Length 2
            08 // (Tag 1 | Varint)
            05 // Value 5
        """)
    }

    func testEncodeNilUInt64Value() throws {
        let writer = ProtoWriter()
        try writer.encode(tag: 1, value: nil as UInt64?, boxed: true)

        assertBufferEqual(writer, "")
    }

    // MARK: - Tests - Encoding Repeated Fields

    func testEncodeRepeatedBools() throws {
        let writer = ProtoWriter()
        let values: [Bool] = [true, false]
        try writer.encode(tag: 1, value: values, packed: false)

        assertBufferEqual(writer, "08_01_08_00")
    }

    func testEncodePackedRepeatedBools() throws {
        let writer = ProtoWriter()
        let values: [Bool] = [true, false]
        try writer.encode(tag: 1, value: values, packed: true)

        assertBufferEqual(writer, "0A_02_01_00")
    }

    func testEncodeRepeatedEnums() throws {
        let writer = ProtoWriter()
        let values: [Person.PhoneType] = [.HOME, .MOBILE]
        try writer.encode(tag: 1, value: values)

        assertBufferEqual(writer, "08_01_08_00")
    }

    func testEncodeUnpackedRepeatedEnums() throws {
        let writer = ProtoWriter()
        let values: [Person.PhoneType] = [.HOME, .MOBILE]
        try writer.encode(tag: 1, value: values, packed: false)

        assertBufferEqual(writer, "08_01_08_00")
    }

    func testEncodePackedRepeatedEnums() throws {
        let writer = ProtoWriter()
        let values: [Person.PhoneType] = [.HOME, .MOBILE]
        try writer.encode(tag: 1, value: values, packed: true)

        assertBufferEqual(writer, "0A_02_01_00")
    }

    func testEncodeRepeatedDoubles() throws {
        let writer = ProtoWriter()
        let doubles: [Double] = [1.2345, 6.7890]
        try writer.encode(tag: 1, value: doubles, packed: false)

        assertBufferEqual(writer, "09_8D976E1283C0F33F_09_0E2DB29DEF271B40")
    }

    func testEncodePackedRepeatedDoubles() throws {
        let writer = ProtoWriter()
        let doubles: [Double] = [1.2345, 6.7890]
        try writer.encode(tag: 1, value: doubles, packed: true)

        assertBufferEqual(writer, "0A_10_8D976E1283C0F33F_0E2DB29DEF271B40")
    }

    func testEncodeRepeatedFloats() throws {
        let writer = ProtoWriter()
        let floats: [Float] = [1.2345, 6.7890]
        try writer.encode(tag: 1, value: floats, packed: false)

        assertBufferEqual(writer, "0D_19049E3F_0D_7D3FD940")
    }

    func testEncodePackedRepeatedFloats() throws {
        let writer = ProtoWriter()
        let floats: [Float] = [1.2345, 6.7890]
        try writer.encode(tag: 1, value: floats, packed: true)

        assertBufferEqual(writer, "0A_08_19049E3F_7D3FD940")
    }

    func testEncodeRepeatedString() throws {
        let writer = ProtoWriter()
        let strings = ["foo", "bar"]
        try writer.encode(tag: 1, value: strings)

        assertBufferEqual(writer, "0A_03_666F6F_0A_03_626172")
    }

    func testEncodeRepeatedFixedUInt32s() throws {
        let writer = ProtoWriter()
        let values: [UInt32] = [1, .max]
        try writer.encode(tag: 1, value: values, encoding: .fixed, packed: false)

        assertBufferEqual(writer, "0D_01000000_0D_FFFFFFFF")
    }

    func testEncodePackedRepeatedFixedUInt32s() throws {
        let writer = ProtoWriter()
        let values: [UInt32] = [1, .max]
        try writer.encode(tag: 1, value: values, encoding: .fixed, packed: true)

        assertBufferEqual(writer, "0A_08_01000000_FFFFFFFF")
    }

    func testEncodeRepeatedVarintUInt32s() throws {
        let writer = ProtoWriter()
        let values: [UInt32] = [1, .max]
        try writer.encode(tag: 1, value: values, encoding: .variable, packed: false)

        assertBufferEqual(writer, "08_01_08_FFFFFFFF0F")
    }

    func testEncodePackedRepeatedVarintUInt32s() throws {
        let writer = ProtoWriter()
        let values: [UInt32] = [1, .max]
        try writer.encode(tag: 1, value: values, encoding: .variable, packed: true)

        assertBufferEqual(writer, "0A_06_01_FFFFFFFF0F")
    }

    func testEncodePackedRepeatedProto2Default() throws {
        let writer = ProtoWriter()
        let data = Data(json_data: "12")
        let person = Person(name: "name", id: 1, data: data) {
            $0.email = "email"
            $0.ids = [1, 2, 3]
        }
        try writer.encode(tag: 1, value: person)

        // Proto2 should used "packed: false" by default.
        assertBufferEqual(writer, """
            0A           // (Tag 1 | Length Delimited)
            1B           // Length 27 for Person message
            0A           // (Tag 1 | Length Delimited)
            04           // Length 4 for name
            6E616D65     // "name" data
            10           // (Tag 2 | Varint)
            01           // id 1
            1A           // (Tag 3 | Length Delimited)
            05           // Length 5 for email
            656D61696C   // "email" data
            // The relevant part
            30           // (Tag 6 | Varint)
            01           // repeated id 1
            30           // (Tag 6 | Varint)
            02           // repeated id 2
            30           // (Tag 6 | Varint)
            03           // repeated id 3
            3A           // (tag 7 | Data)
              04           // Length 4 for Data
              0A           // (Tag 1 | Length Delimited)
              02           // Length 2 for json_data
              3132         // UTF-8 Value '12'
        """)
    }

    func testEncodePackedRepeatedProto3Default() throws {
        let writer = ProtoWriter()
        let person = Person3(name: "name", id: 1) {
            $0.email = "email"
            $0.ids = [1, 2, 3]
        }
        try writer.encode(tag: 1, value: person)

        // Proto3 should used "packed: true" by default.
        assertBufferEqual(writer, """
            0A           // (Tag 1 | Length Delimited)
            14           // Length 20 for Person message
            0A           // (Tag 1 | Length Delimited)
            04           // Length 4 for name
            6E616D65     // "name" data
            10           // (Tag 2 | Varint)
            01           // id 1
            1A           // (Tag 3 | Length Delimited)
            05           // Length 5 for email
            656D61696C   // "email" data
            // The relevant part
            32           // (Tag 6 | Length Delimited)
            03           // Length 3 for repeated ids
            010203       // Repeated ids 1, 2, and 3
        """)
    }

    // MARK: - Tests - Encoding Maps

    func testEncodeUInt32ToUInt32FixedMap() throws {
        let writer = ProtoWriter()
        writer.outputFormatting = .sortedKeys
        let values: [UInt32: UInt32] = [1: 2, 3: 4]
        try writer.encode(tag: 1, value: values, keyEncoding: .fixed, valueEncoding: .fixed)

        assertBufferEqual(writer, """
            // Key/Value 1
            0A       // (Tag 1 | Length Delimited)
            0A       // Length 10
            0D       // (Tag 1 | Fixed32)
            01000000 // Value 1
            15       // (Tag 2 | Fixed32)
            02000000 // Value 2

            // Key/Value 2
            0A       // (Tag 1 | Length Delimited)
            0A       // Length 10
            0D       // (Tag 1 | Fixed32)
            03000000 // Value 3
            15       // (Tag 2 | Fixed32)
            04000000 // Value 4
        """)
    }

    func testEncodeUInt32ToUInt64VarintMap() throws {
        let writer = ProtoWriter()
        writer.outputFormatting = .sortedKeys
        let values: [UInt32: UInt64] = [1: 2, 3: 4]
        try writer.encode(tag: 1, value: values, keyEncoding: .variable, valueEncoding: .variable)

        assertBufferEqual(writer, """
            // Key/Value 1
            0A // (Tag 1 | Length Delimited)
            04 // Length 4
            08 // (Tag 1 | Varint)
            01 // Value 1
            10 // (Tag 2 | Varint)
            02 // Value 2

            // Key/Value 2
            0A // (Tag 1 | Length Delimited)
            04 // Length 4
            08 // (Tag 1 | Varint)
            03 // Value 3
            10 // (Tag 2 | Varint)
            04 // Value 4
        """)
    }

    func testEncodeSignedInt64ToEnumMap() throws {
        let writer = ProtoWriter()
        writer.outputFormatting = .sortedKeys
        let values: [Int64: Person.PhoneType] = [1: .HOME, 2: .MOBILE]
        try writer.encode(tag: 1, value: values, keyEncoding: .signed)

        assertBufferEqual(writer, """
            // Key/Value 1
            0A // (Tag 1 | Length Delimited)
            04 // Length 4
            08 // (Tag 1 | Varint)
            02 // Value 1
            10 // (Tag 2 | Varint)
            01 // Value .HOME

            // Key/Value 2
            0A // (Tag 1 | Length Delimited)
            04 // Length 4
            08 // (Tag 1 | Varint)
            04 // Value 3
            10 // (Tag 2 | Varint)
            00 // Value .MOBILE
        """)
    }

    func testEncodeUInt32ToStringMap() throws {
        let writer = ProtoWriter()
        writer.outputFormatting = .sortedKeys
        let values: [UInt32: String] = [1: "two", 3: "four"]
        try writer.encode(tag: 1, value: values, keyEncoding: .variable)

        assertBufferEqual(writer, """
            // Key/Value 1
            0A     // (Tag 1 | Length Delimited)
            07     // Length 7
            08     // (Tag 1 | Varint)
            01     // Value 1
            12     // (Tag 2 | Length Delimited)
            03     // Length 3
            74776F // Value "two"

            // Key/Value 2
            0A       // (Tag 1 | Length Delimited)
            08       // Length 8
            08       // (Tag 1 | Varint)
            03       // Value 3
            12       // (Tag 2 | Length Delimited)
            04       // Length 4
            666F7572 // Value "four"
        """)
    }

    func testEncodeStringToEnumMap() throws {
        let writer = ProtoWriter()
        writer.outputFormatting = .sortedKeys
        let values: [String: Person.PhoneType] = ["a": .HOME, "b": .MOBILE]
        try writer.encode(tag: 1, value: values)

        assertBufferEqual(writer, """
            // Key/Value 1
            0A // (Tag 1 | Length Delimited)
            05 // Length 5
            0A // (Tag 1 | Length Delimited)
            01 // Length 1
            61 // Value "a"
            10 // (Tag 2 | Varint)
            01 // Value .HOME

            // Key/Value 2
            0A // (Tag 1 | Length Delimited)
            05 // Length 5
            0A // (Tag 1 | Length Delimited)
            01 // Length 1
            62 // Value "b"
            10 // (Tag 2 | Varint)
            00 // Value .MOBILE
        """)
    }

    func testEncodeStringToInt32FixedMap() throws {
        let writer = ProtoWriter()
        writer.outputFormatting = .sortedKeys
        let values: [String: Int32] = ["a": -1, "b": -2]
        try writer.encode(tag: 1, value: values, valueEncoding: .fixed)

        assertBufferEqual(writer, """
            // Key/Value 1
            0A       // (Tag 1 | Length Delimited)
            08       // Length 8
            0A       // (Tag 1 | Length Delimited)
            01       // Length 1
            61       // Value "a"
            15       // (Tag 2 | Fixed32)
            FFFFFFFF // Value "two"

            // Key/Value 2
            0A       // (Tag 1 | Length Delimited)
            08       // Length 8
            0A       // (Tag 1 | Length Delimited)
            01       // Length 1
            62       // Value "b"
            15       // (Tag 2 | Fixed32)
            FEFFFFFF // Value "four"
        """)
    }

    func testEncodeStringToStringMap() throws {
        let writer = ProtoWriter()
        writer.outputFormatting = .sortedKeys
        let values: [String: String] = ["a": "two", "b": "four"]
        try writer.encode(tag: 1, value: values)

        assertBufferEqual(writer, """
            // Key/Value 1
            0A     // (Tag 1 | Length Delimited)
            08     // Length 8
            0A     // (Tag 1 | Length Delimited)
            01     // Length 1
            61     // Value "a"
            12     // (Tag 2 | Length Delimited)
            03     // Length 3
            74776F // Value "two"

            // Key/Value 2
            0A       // (Tag 1 | Length Delimited)
            09       // Length 9
            0A       // (Tag 1 | Length Delimited)
            01       // Length 1
            62       // Value "b"
            12       // (Tag 2 | Length Delimited)
            04       // Length 4
            666F7572 // Value "four"
        """)
    }

    // MARK: - Tests - Unknown Fields

    func testWriteUnknownFields() throws {
        let writer = ProtoWriter()
        let data = Foundation.Data(hexEncoded: "001122334455")!
        try writer.writeUnknownFields([1: data])

        XCTAssertEqual(Foundation.Data(writer.buffer, copyBytes: true), data)
    }

    // MARK: - Tests - Writing Primitives

    func testWriteFixed32() {
        let writer = ProtoWriter()
        writer.writeFixed32(UInt32(5))
        assertBufferEqual(writer, "05000000")

        writer.writeFixed32(UInt32.max)
        assertBufferEqual(writer, "05000000_FFFFFFFF")
    }

    func testWriteFixed64() {
        let writer = ProtoWriter()
        writer.writeFixed64(UInt64(5))
        assertBufferEqual(writer, "0500000000000000")

        writer.writeFixed64(UInt64.max)
        assertBufferEqual(writer, "0500000000000000_FFFFFFFFFFFFFFFF")
    }

    func testWriteVarint32() {
        let writer = ProtoWriter()
        writer.writeVarint(UInt32(5))
        assertBufferEqual(writer, "05")

        writer.writeVarint(UInt32(300))
        assertBufferEqual(writer, "05_AC02")

        writer.writeVarint(UInt32.max)
        assertBufferEqual(writer, "05_AC02_FFFFFFFF0F")
    }

    func testWriteVarint64() {
        let writer = ProtoWriter()
        writer.writeVarint(UInt64(5))
        assertBufferEqual(writer, "05")

        writer.writeVarint(UInt64(300))
        assertBufferEqual(writer, "05_AC02")

        writer.writeVarint(UInt64.max)
        assertBufferEqual(writer, "05_AC02_FFFFFFFFFFFFFFFFFF01")
    }

    // MARK: - Private Methods

    private func assertBufferEqual(
        _ writer: ProtoWriter,
        _ hexString: String,
        file: StaticString = #file,
        line: UInt = #line
    ) {
        print(Foundation.Data(writer.buffer, copyBytes: true).hexEncodedString())
        print(hexString)
        let actual = Foundation.Data(writer.buffer, copyBytes: true)
        let expected = Foundation.Data(hexEncoded: hexString)!
        XCTAssertEqual(
            actual,
            expected,
            "\(actual.hexEncodedString()) is not equal to \(expected.hexEncodedString())",
            file: file,
            line: line
        )
    }
}
