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

final class ProtoReaderTests: XCTestCase {

    // MARK: - Tests - Decoding Integers

    func testDecodeFixedInt32() throws {
        let data = Foundation.Data(hexEncoded: "0D_FBFFFFFF")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Int32.self, encoding: .fixed) }
            XCTAssertEqual(value, -5)
        }
    }

    func testDecodeFixedInt64() throws {
        let data = Foundation.Data(hexEncoded: "09_FBFFFFFFFFFFFFFF")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Int64.self, encoding: .fixed) }
            XCTAssertEqual(value, -5)
        }
    }

    func testDecodeFixedUInt32() throws {
        let data = Foundation.Data(hexEncoded: "0D_05000000")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(UInt32.self, encoding: .fixed) }
            XCTAssertEqual(value, 5)
        }
    }

    func testDecodeFixedUInt64() throws {
        let data = Foundation.Data(hexEncoded: "09_0500000000000000")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(UInt64.self, encoding: .fixed) }
            XCTAssertEqual(value, 5)
        }
    }

    func testDecodeSignedInt32() throws {
        let data = Foundation.Data(hexEncoded: "08_09")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Int32.self, encoding: .signed) }
            XCTAssertEqual(value, -5)
        }
    }

    func testDecodeSignedInt64() throws {
        let data = Foundation.Data(hexEncoded: "08_09")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Int64.self, encoding: .signed) }
            XCTAssertEqual(value, -5)
        }
    }

    func testDecodeVarintInt32() throws {
        let data = Foundation.Data(hexEncoded: "08_FBFFFFFFFFFFFFFFFF01")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Int32.self, encoding: .variable) }
            XCTAssertEqual(value, -5)
        }
    }

    func testDecodeVarintInt64() throws {
        let data = Foundation.Data(hexEncoded: "08_FBFFFFFFFFFFFFFFFF01")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Int64.self, encoding: .variable) }
            XCTAssertEqual(value, -5)
        }
    }

    func testDecodeVarintUInt32() throws {
        let data = Foundation.Data(hexEncoded: "08_05")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(UInt32.self, encoding: .variable) }
            XCTAssertEqual(value, 5)
        }
    }

    func testDecodeVarintUInt64() throws {
        let data = Foundation.Data(hexEncoded: "08_05")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(UInt64.self, encoding: .variable) }
            XCTAssertEqual(value, 5)
        }
    }

    // MARK: - Tests - Decoding Messages And More

    func testDecodeBool() throws {
        let data = Foundation.Data(hexEncoded: "08_01")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Bool.self) }
            XCTAssertEqual(value, true)
        }
    }

    func testDecodeBytes() throws {
        let data = Foundation.Data(hexEncoded: """
            0A           // (tag 1 | Length Delimited)
            06           // Data length
            001122334455 // Random data
        """)!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Foundation.Data.self) }
            XCTAssertEqual(value, Foundation.Data(hexEncoded: "001122334455")!)
        }
    }

    func testDecodeMessage() throws {
        let data = Foundation.Data(hexEncoded: """
            0A       // (tag 1 | Length Delimited)
            04       // Length of name
            4C756B65 // Name value "Luke"
            10       // (tag 2 | Varint)
            05       // ID value 5
            3A       // (tag 7 | Data)
              04       // Length 4 for Data
              0A       // (Tag 1 | Length Delimited)
              02       // Length 2 for json_data
              3132     // UTF-8 Value '12'
        """)!
        try test(data: data) { reader in
            let data = Data(json_data: "12")
            let message = Person(name: "Luke", id: 5, data: data)
            XCTAssertEqual(try reader.decode(Person.self), message)
        }
    }

    func testDecodeString() throws {
        let data = Foundation.Data(hexEncoded: "666F6F")!
        try test(data: data) { reader in
            XCTAssertEqual(try reader.decode(String.self), "foo")
        }
    }

    func testDecodeNestedEmptyMessage() throws {
        let data = Foundation.Data(hexEncoded: """
            0A     // (Tag 1 | Varint) for `name`
            03     // Length 3
            426F62 // "Bob"
            12     // (Tag 2 | Length Delimited) for `child1`
            00     // Length 0
        """)!
        try test(data: data) { reader in
            let expected = Parent {
                $0.name = "Bob"
                $0.child = .init()
            }
            XCTAssertEqual(try reader.decode(Parent.self), expected)
        }
    }

    // MARK: - Tests - Decoding Proto3 Well-Known Types

    func testDecodeBoolValue() throws {
        let data = Foundation.Data(hexEncoded: """
            0A   // (Tag 1 | Length Delimited)
            02   // Length 2
            08   // (Tag 1 | Varint)
            01   // Value "true"
        """)!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Bool.self, boxed: true) }
            XCTAssertEqual(value, true)
        }
    }

    func testDecodeBytesValue() throws {
        let data = Foundation.Data(hexEncoded: """
            0A           // (Tag 1 | Length Delimited)
            08           // Length 8
            0A           // (tag 1 | Length Delimited)
            06           // Data length 6
            001122334455 // Random data
        """)!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Foundation.Data.self, boxed: true) }
            XCTAssertEqual(value, Foundation.Data(hexEncoded: "001122334455")!)
        }
    }

    func testDecodeDoubleValue() throws {
        let data = Foundation.Data(hexEncoded: """
            0A                // (Tag 1 | Length Delimited)
            09                // Length 9
            09                // (Tag 1 | Fixed64)
            8D976E1283C0F33F  // Value 1.2345
        """)!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Double.self, boxed: true) }
            XCTAssertEqual(value, 1.2345)
        }
    }

    func testDecodeFloatValue() throws {
        let data = Foundation.Data(hexEncoded: """
            0A        // (Tag 1 | Length Delimited)
            05        // Length 9
            0D        // (Tag 1 | Fixed32)
            19049E3F  // Value 1.2345
        """)!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Float.self, boxed: true) }
            XCTAssertEqual(value, 1.2345)
        }
    }

    func testDecodeInt32Value() throws {
        let data = Foundation.Data(hexEncoded: """
            0A         // (Tag 1 | Length Delimited)
            06         // Length 6
            08         // (Tag 1 | Varint)
            FBFFFFFF0F // Value -5
        """)!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Int32.self, boxed: true) }
            XCTAssertEqual(value, -5)
        }
    }

    func testDecodeInt64Value() throws {
        let data = Foundation.Data(hexEncoded: """
            0A                   // (Tag 1 | Length Delimited)
            0B                   // Length 11
            08                   // (Tag 1 | Varint)
            FBFFFFFFFFFFFFFFFF01 // Value -5
        """)!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Int64.self, boxed: true) }
            XCTAssertEqual(value, -5)
        }
    }

    func testDecodeStringValue() throws {
        let data = Foundation.Data(hexEncoded: """
            0A     // (Tag 1 | Length Delimited)
            05     // Length 5
            0A     // (tag 1 | Length Delimited)
            03     // String length 3
            666F6F // "foo"
        """)!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(String.self, boxed: true) }
            XCTAssertEqual(value, "foo")
        }
    }

    func testDecodeUInt32Value() throws {
        let data = Foundation.Data(hexEncoded: """
            0A // (Tag 1 | Length Delimited)
            02 // Length 2
            08 // (Tag 1 | Varint)
            05 // Value 5
        """)!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(UInt32.self, boxed: true) }
            XCTAssertEqual(value, 5)
        }
    }

    func testDecodeUInt64Value() throws {
        let data = Foundation.Data(hexEncoded: """
            0A // (Tag 1 | Length Delimited)
            02 // Length 2
            08 // (Tag 1 | Varint)
            05 // Value 5
        """)!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(UInt64.self, boxed: true) }
            XCTAssertEqual(value, 5)
        }
    }

    // MARK: - Tests - Decoding Enums

    func testDecodeEnum() throws {
        let data = Foundation.Data(hexEncoded: "08_01")!
        try test(data: data) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Person.PhoneType.self) }
            XCTAssertEqual(value, .HOME)
        }
    }

    func testDecodeUnknownEnumNilStrategy() throws {
        let data = Foundation.Data(hexEncoded: "08_05")!
        try test(data: data, enumStrategy: .returnNil) { reader in
            let value = try reader.decode(tag: 1) { try reader.decode(Person.PhoneType.self) }
            XCTAssertNil(value)
        }
    }

    func testDecodeUnknownEnumThrowStrategy() throws {
        let data = Foundation.Data(hexEncoded: "08_05")!
        var didThrow = false

        try test(data: data, enumStrategy: .throwError) { reader in
            do {
                let _ = try reader.decode(tag: 1) { try reader.decode(Person.PhoneType.self) }
            } catch ProtoDecoder.Error.unknownEnumCase(_, let fieldNumber) {
                didThrow = true
                XCTAssertEqual(fieldNumber, 5)
            }
        }

        XCTAssertTrue(didThrow, "Unknown enum should throw")
    }

    func testDecodeEnumInOneOf() throws {
        let data = Foundation.Data(hexEncoded: """
            20 // (Tag 4 | Varint)
            01 // Value 1
            10 // (Tag 2 | Varint)
            01 // Value 1
        """)!
        try test(data: data, enumStrategy: .returnNil) { reader in
            let message = OneOfs {
                $0.standalone_enum = .A
                $0.choice = .similar_enum_option(.A)
            }
            XCTAssertEqual(try reader.decode(OneOfs.self), message)
        }
    }

    func testDecodeUnknownEnumInOneOfNilStrategy() throws {
        let data = Foundation.Data(hexEncoded: """
            20 // (Tag 4 | Varint)
            01 // Value 1
            10 // (Tag 2 | Varint)
            02 // Value 2
        """)!
        try test(data: data, enumStrategy: .returnNil) { reader in
            let message = OneOfs {
                $0.standalone_enum = .A
            }
            XCTAssertEqual(try reader.decode(OneOfs.self), message)
        }
    }

    // MARK: - Tests - Decoding Repeated Fields

    func testDecodeRepeatedBools() throws {
        let data = Foundation.Data(hexEncoded: "08_01_08_00")!
        try test(data: data) { reader in
            var values: [Bool] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, [true, false])
        }
    }

    func testDecodePackedRepeatedBools() throws {
        let data = Foundation.Data(hexEncoded: "0A_02_01_00")!
        try test(data: data) { reader in
            var values: [Bool] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, [true, false])
        }
    }

    func testDecodeRepeatedEnums() throws {
        let data = Foundation.Data(hexEncoded: "08_01_08_00")!
        try test(data: data) { reader in
            var values: [Person.PhoneType] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, [.HOME, .MOBILE])
        }
    }

    func testDecodeRepeatedUnknownEnumsThrowStrategy() throws {
        let data = Foundation.Data(hexEncoded: """
             08       // (tag 1 | Varint)
             01       // .HOME
             08       // (tag 1 | Varint)
             05       // Unknown enum
         """)!

        try test(data: data, enumStrategy: .throwError) { reader in
            var values: [Person.PhoneType] = []

            var didThrow = false

            try _ = reader.forEachTag { tag in
                switch tag {
                case 1:
                    do {
                        try reader.decode(into: &values)
                    } catch ProtoDecoder.Error.unknownEnumCase(_, let fieldNumber) {
                        didThrow = true
                        XCTAssertEqual(fieldNumber, 5)
                    }

                default:
                    XCTFail("Should not encounter unknown fields")
                }
            }

            XCTAssertTrue(didThrow, "Unknown enum should throw")
        }
    }

    func testDecodeRepeatedUnknownEnumsNilStrategySomeFail() throws {
        let data = Foundation.Data(hexEncoded: """
             08       // (tag 1 | Varint)
             01       // .HOME
             08       // (tag 1 | Varint)
             05       // Unknown enum
         """)!
        try test(data: data, enumStrategy: .returnNil) { reader in
            var values: [Person.PhoneType] = []


            let fields = try reader.forEachTag { tag in
                switch tag {
                case 1:
                    try reader.decode(into: &values)
                default:
                    XCTFail("Should not encounter unknown fields")
                }
            }

            XCTAssertEqual(values, [.HOME])
            let expectedData = Foundation.Data(hexEncoded: """
                08       // (tag 1 | Varint)
                05       // Unknown enum
            """)!
            XCTAssertEqual(fields, [1: expectedData])
        }
    }

    func testDecodeRepeatedUnknownEnumsNilStrategyAllFail() throws {
        let data = Foundation.Data(hexEncoded: """
             08       // (tag 1 | Varint)
             04       // Unknown enum
             08       // (tag 1 | Varint)
             05       // Unknown enum
         """)!
        try test(data: data, enumStrategy: .returnNil) { reader in
            var values: [Person.PhoneType] = []


            let fields = try reader.forEachTag { tag in
                switch tag {
                case 1:
                    try reader.decode(into: &values)
                default:
                    XCTFail("Should not encounter unknown fields")
                }
            }

            XCTAssertEqual(values, [])
            XCTAssertEqual(fields, [1: data])
        }
    }


    func testDecodePackedRepeatedEnums() throws {
        let data = Foundation.Data(hexEncoded: "0A_02_01_00")!
        try test(data: data) { reader in
            var values: [Person.PhoneType] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, [.HOME, .MOBILE])
        }
    }

    func testDecodePackedRepeatedUnknownEnumsSomeFail() throws {
        let data = Foundation.Data(hexEncoded: """
            0A       // (tag 1 | Length Delimited)
            02       // Number of enums
            01       // .HOME
            05       // Unknown enum
        """)!

        try test(data: data, enumStrategy: .returnNil) { reader in
            var values: [Person.PhoneType] = []

            let fields = try reader.forEachTag { tag in
                switch tag {
                case 1:
                    try reader.decode(into: &values)
                default:
                    XCTFail("Should not encounter unknown fields")
                }
            }

            let expectedData = Foundation.Data(hexEncoded: """
                       08       // (Tag 1 | Varint)
                       05       // Unknown enum
                   """)!

            XCTAssertEqual(values, [.HOME])
            XCTAssertEqual(fields, [1: expectedData])
        }
    }

    func testDecodePackedRepeatedUnknownEnumsAllFail() throws {
        let data = Foundation.Data(hexEncoded: """
            0A       // (tag 1 | Length Delimited)
            02       // Number of enums
            04       // Unknown enum
            05       // Unknown enum
        """)!
        try test(data: data, enumStrategy: .returnNil) { reader in
            var values: [Person.PhoneType] = []

            let fields = try reader.forEachTag { tag in
                switch tag {
                case 1:
                    try reader.decode(into: &values)
                default:
                    XCTFail("Should not encounter unknown fields")
                }
            }

            XCTAssertEqual(values, [])

            // The original data is packed (length delimited), but we encode it as
            // unpacked in the unknown data, so it shows up as individual varints.

            let expectedData = Foundation.Data(hexEncoded: """
                08       // (Tag 1 | Varint)
                04       // Unknown enum
                08       // (Tag 1 | Varint)
                05       // Unknown enum
            """)!

            XCTAssertEqual(fields, [1: expectedData])
        }
    }

    func testDecodeRepeatedStrings() throws {
        let data = Foundation.Data(hexEncoded: """
            0A     // (Tag 1 | Length Delimited)
            03     // Length 3
            666F6F // Value "foo"
            0A     // (Tag 1 | Length Delimited)
            03     // Length 3
            626172 // Value "bar"
        """)!
        try test(data: data) { reader in
            var values: [String] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, ["foo", "bar"])
        }
    }

    func testDecodeRepeatedDoubles() throws {
        let data = Foundation.Data(hexEncoded: """
            09               // (Tag 1 | Fixed64)
            8D976E1283C0F33F // Value 1.2345
            09               // (Tag 1 | Fixed64)
            0E2DB29DEF271B40 // Value 6.7890
        """)!
        try test(data: data) { reader in
            var values: [Double] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, [1.2345, 6.7890])
        }
    }

    func testDecodePackedRepeatedDoubles() throws {
        let data = Foundation.Data(hexEncoded: """
            0A
            10
            8D976E1283C0F33F
            0E2DB29DEF271B40
        """)!
        try test(data: data) { reader in
            var values: [Double] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, [1.2345, 6.7890])
        }
    }

    func testDecodeRepeatedFloats() throws {
        let data = Foundation.Data(hexEncoded: "0D_19049E3F_0D_7D3FD940")!
        try test(data: data) { reader in
            var values: [Float] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, [1.2345, 6.7890])
        }
    }

    func testDecodePackedRepeatedFloats() throws {
        let data = Foundation.Data(hexEncoded: "0A_08_19049E3F_7D3FD940")!
        try test(data: data) { reader in
            var values: [Float] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, [1.2345, 6.7890])
        }
    }

    func testDecodeRepeatedFixedUInt32() throws {
        let data = Foundation.Data(hexEncoded: """
            0D       // (Tag 1 | Fixed32)
            01000000 // Value 1
            0D       // (Tag 1 | Fixed 32)
            FFFFFFFF // Value UInt32.max
        """)!

        try test(data: data) { reader in
            var values: [UInt32] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .fixed) }

            XCTAssertEqual(values, [1, .max])
        }
    }

    func testDecodePackedRepeatedFixedUInt32() throws {
        let data = Foundation.Data(hexEncoded: """
            0A       // (Tag 1 | Length Delimited)
            08       // Length 8
            01000000 // Value 1
            FFFFFFFF // Value UInt32.max
        """)!

        try test(data: data) { reader in
            var values: [UInt32] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .fixed) }

            XCTAssertEqual(values, [1, .max])
        }
    }

    func testDecodePackedRepeatedFixedUInt32Empty() throws {
        let data = Foundation.Data(hexEncoded: """
            0A       // (Tag 1 | Length Delimited)
            00       // Length 0
        """)!

        try test(data: data) { reader in
            var values: [UInt64] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .fixed) }

            XCTAssertEqual(values, [])
        }
    }

    func testDecodePackedRepeatedFixedUInt64() throws {
        let data = Foundation.Data(hexEncoded: """
            0A               // (Tag 1 | Length Delimited)
            10               // Length 16
            0100000000000000 // Value 1
            FFFFFFFFFFFFFFFF // Value UInt64.max
        """)!

        try test(data: data) { reader in
            var values: [UInt64] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .fixed) }

            XCTAssertEqual(values, [1, .max])
        }
    }

    func testDecodePackedRepeatedFixedUInt64Empty() throws {
        let data = Foundation.Data(hexEncoded: """
            0A       // (Tag 1 | Length Delimited)
            00       // Length 0
        """)!

        try test(data: data) { reader in
            var values: [UInt64] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .fixed) }

            XCTAssertEqual(values, [])
        }
    }

    func testDecodeRepeatedVarintUInt32() throws {
        let data = Foundation.Data(hexEncoded: """
            08         // (Tag 1 | Varint)
            01         // Value 1
            08         // (Tag 1 | Varint)
            FFFFFFFF0F // Value UInt32.max
        """)!

        try test(data: data) { reader in
            var values: [UInt32] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .variable) }

            XCTAssertEqual(values, [1, .max])
        }
    }

    func testDecodePackedRepeatedVarintUInt32() throws {
        let data = Foundation.Data(hexEncoded: """
            0A         // (Tag 1 | Varint)
            06         // Length 6
            01         // Value 1
            FFFFFFFF0F // Value UInt32.max
        """)!

        try test(data: data) { reader in
            var values: [UInt32] = []
            try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .variable) }

            XCTAssertEqual(values, [1, .max])
        }
    }

    // MARK: - Tests - Decoding Maps

    func testDecodeUInt32ToUInt32FixedMap() throws {
        let data = Foundation.Data(hexEncoded: """
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
        """)!

        try test(data: data) { reader in
            var values: [UInt32: UInt32] = [:]
            try reader.decode(tag: 1) { try reader.decode(into: &values, keyEncoding: .fixed, valueEncoding: .fixed) }

            XCTAssertEqual(values, [1: 2, 3: 4])
        }
    }

    func testDecodeUInt32ToUInt64VarintMap() throws {
        let data = Foundation.Data(hexEncoded: """
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
        """)!

        try test(data: data) { reader in
            var values: [UInt32: UInt64] = [:]
            try reader.decode(tag: 1) { try reader.decode(into: &values, keyEncoding: .variable, valueEncoding: .variable) }

            XCTAssertEqual(values, [1: 2, 3: 4])
        }
    }

    func testDecodeUInt32ToStringMap() throws {
        let data = Foundation.Data(hexEncoded: """
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
        """)!

        try test(data: data) { reader in
            var values: [UInt32: String] = [:]
            try reader.decode(tag: 1) { try reader.decode(into: &values, keyEncoding: .variable) }

            XCTAssertEqual(values, [1: "two", 3: "four"])
        }
    }

    func testDecodeStringToEnumMap() throws {
        let data = Foundation.Data(hexEncoded: """
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
        """)!

        try test(data: data) { reader in
            var values: [String: Person.PhoneType] = [:]
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, ["a": .HOME, "b": .MOBILE])
        }
    }

    func testDecodeStringToUnknownEnumMapOneFail() throws {
        let data = Foundation.Data(hexEncoded: """
            // Key/Value 1
            0A // (Tag 1 | Length Delimited)
            05 // Length 5
            0A // (Tag 1 | Length Delimited)
            01 // Length 1
            61 // Value "a"
            10 // (Tag 2 | Varint)
            05 // Value unknown

            // Key/Value 2
            0A // (Tag 1 | Length Delimited)
            05 // Length 5
            0A // (Tag 1 | Length Delimited)
            01 // Length 1
            62 // Value "b"
            10 // (Tag 2 | Varint)
            00 // Value .MOBILE
        """)!

        try test(data: data, enumStrategy: .returnNil) { reader in
            var values: [String: Person.PhoneType] = [:]

            let unknownFields = try reader.forEachTag { tag in
                switch tag {
                case 1:
                    try reader.decode(into: &values)
                default:
                    XCTFail("Should not encounter unknown fields")
                }
            }

            XCTAssertEqual(values, ["b": .MOBILE])
            let expectedData = Foundation.Data(hexEncoded: """
                // Key/Value 1
                0A // (Tag 1 | Length Delimited)
                05 // Length 5
                0A // (Tag 1 | Length Delimited)
                01 // Length 1
                61 // Value "a"
                10 // (Tag 2 | Varint)
                05 // Value unknown
            """)!
            XCTAssertEqual(unknownFields, [1: expectedData])
        }
    }

    func testDecodeStringToUnknownEnumMapOneFailReverseOrder() throws {

        let data = Foundation.Data(hexEncoded: """
            // Key/Value 1 out of order
            0A // (Tag 1 | Length Delimited)
            05 // Length 5
            10 // (Tag 2 | Varint)
            05 // Value unknown
            0A // (Tag 1 | Length Delimited)
            01 // Length 1
            61 // Value "a"


            // Key/Value 2 out of order
            0A // (Tag 1 | Length Delimited)
            05 // Length 5
            10 // (Tag 2 | Varint)
            00 // Value .MOBILE
            0A // (Tag 1 | Length Delimited)
            01 // Length 1
            62 // Value "b"
        """)!

        try test(data: data, enumStrategy: .returnNil) { reader in
            var values: [String: Person.PhoneType] = [:]

            let unknownFields = try reader.forEachTag { tag in
                switch tag {
                case 1:
                    try reader.decode(into: &values)
                default:
                    XCTFail("Should not encounter unknown fields")
                }
            }

            XCTAssertEqual(values, ["b": .MOBILE])
            let expectedData = Foundation.Data(hexEncoded: """
                    // Key/Value 1 in proper order
                    0A // (Tag 1 | Length Delimited)
                    05 // Length 5
                    0A // (Tag 1 | Length Delimited)
                    01 // Length 1
                    61 // Value "a"
                    10 // (Tag 2 | Varint)
                    05 // Value unknown
                """)!
            XCTAssertEqual(unknownFields, [1: expectedData])
        }
    }

    func testDecodeStringToUnknownEnumMapAllFail() throws {
        let data = Foundation.Data(hexEncoded: """
            // Key/Value 1
            0A // (Tag 1 | Length Delimited)
            05 // Length 5
            0A // (Tag 1 | Length Delimited)
            01 // Length 1
            61 // Value "a"
            10 // (Tag 2 | Varint)
            05 // Value unknown

            // Key/Value 2
            0A // (Tag 1 | Length Delimited)
            05 // Length 5
            0A // (Tag 1 | Length Delimited)
            01 // Length 1
            62 // Value "b"
            10 // (Tag 2 | Varint)
            05 // Value unknown
        """)!

        try test(data: data, enumStrategy: .returnNil) { reader in
            var values: [String: Person.PhoneType] = [:]

            let unknownFields = try reader.forEachTag { tag in
                switch tag {
                case 1:
                    try reader.decode(into: &values)
                default:
                    XCTFail("Should not encounter unknown fields")
                }
            }

            XCTAssertEqual(values, [:])
            XCTAssertEqual(unknownFields, [1: data])
        }
    }

    func testDecodeStringToInt32FixedMap() throws {
        let data = Foundation.Data(hexEncoded: """
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
        """)!

        try test(data: data) { reader in
            var values: [String: Int32] = [:]
            try reader.decode(tag: 1) { try reader.decode(into: &values, valueEncoding: .fixed) }

            XCTAssertEqual(values, ["a": -1, "b": -2])
        }
    }

    func testDecodeStringToStringMap() throws {
        let data = Foundation.Data(hexEncoded: """
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
        """)!

        try test(data: data) { reader in
            var values: [String: String] = [:]
            try reader.decode(tag: 1) { try reader.decode(into: &values) }

            XCTAssertEqual(values, ["a": "two", "b": "four"])
        }
    }

    // MARK: - Tests - Groups

    func testSkippingGroup() throws {
        let data = Foundation.Data(hexEncoded: """
            0B       // (Tag 1 | Start Group)
            10       // (Tag 2 | Varint)
            05       // Value 5
            1A       // (Tag 3 | Length Delimited)
            0A       // Length 10
              0D       // (Tag 1 | Fixed32)
              05000000 // Value 5
              15       // (Tag 2 | Fixed32)
              FFFFFFFF // Value UInt32.max
            25       // (Tag 4 | Fixed32)
            FBFFFFFF // Value -5
            0C       // (Tag 1 | End Group
        """)!
        try test(data: data) { reader in
            let unknownFields = try reader.forEachTag { tag in
                XCTFail("The one group tag should have been skipped")
            }

            // The entire data should have been skipped.
            XCTAssertEqual(unknownFields, [1: data])
        }
    }

    func testSkippingRepeatedGroup() throws {
        var data = Foundation.Data(hexEncoded: """
            0B       // (Tag 1 | Start Group)
            10       // (Tag 2 | Varint)
            05       // Value 5
            1A       // (Tag 3 | Length Delimited)
            0A       // Length 10
              0D       // (Tag 1 | Fixed32)
              05000000 // Value 5
              15       // (Tag 2 | Fixed32)
              FFFFFFFF // Value UInt32.max
            25       // (Tag 4 | Fixed32)
            FBFFFFFF // Value -5
            0C       // (Tag 1 | End Group
        """)!
        // Repeat it twice
        data.append(data)

        try test(data: data) { reader in
            let unknownFields = try reader.forEachTag { tag in
                XCTFail("The one group tag should have been skipped")
            }

            XCTAssertEqual(unknownFields, [1: data])
        }
    }

    func testSkippingNestedGroups() throws {
        let data = Foundation.Data(hexEncoded: """
            0B       // (Tag 1 | Start Group)
            10       // (Tag 2 | Varint)
            05       // Value 5
            1B       // (Tag 3 | Start Group)
              0D       // (Tag 1 | Fixed32)
              05000000 // Value 5
              15       // (Tag 2 | Fixed32)
              FFFFFFFF // Value UInt32.max
            1C       // (Tag 3 | End Group)
            25       // (Tag 4 | Fixed32)
            FBFFFFFF // Value -5
            0C       // (Tag 1 | End Group
        """)!

        try test(data: data) { reader in
            let unknownFields = try reader.forEachTag { tag in
                XCTFail("The one group tag should have been skipped")
            }

            XCTAssertEqual(unknownFields, [1: data])
        }
    }

    // MARK: - Tests - Unknown Fields

    func testUnknownFields() throws {
        let data = Foundation.Data(hexEncoded: """
            0D       // (Tag 1 | Fixed32)
            05000000 // Value 5
            15       // (Tag 2 | Fixed32)
            FFFFFFFF // Value UInt32.max
        """)!
        try test(data: data) { reader in
            let unknownFields = try reader.forEachTag { tag in
                switch tag {
                case 1: XCTAssertEqual(try reader.readFixed32(), 5)
                default: try reader.readUnknownField(tag: tag)
                }
            }

            XCTAssertEqual(unknownFields, [2: Foundation.Data(hexEncoded: "15_FFFFFFFF")])
        }
    }

    func testNonContiguousUnknownFields() throws {
        let data = Foundation.Data(hexEncoded: """
            08         // (Tag 1 | Varint)
            05         // Value 5
            10         // (Tag 2 | Varint)
            AC02       // Value 300
            18         // (Tag 3 | Varint)
            FFFFFFFF0F // Value UInt32.max
        """)!
        try test(data: data) { reader in
            let unknownFields = try reader.forEachTag { tag in
                switch tag {
                case 2: XCTAssertEqual(try UInt32(truncatingIfNeeded: reader.readVarint()), 300)
                default: try reader.readUnknownField(tag: tag)
                }
            }

            XCTAssertEqual(unknownFields, [1: Foundation.Data(hexEncoded: "08_05"), 3: Foundation.Data(hexEncoded: "18_FFFFFFFF0F")])
        }
    }

    struct NestedMessage: ProtoDecodable {

        static var protoSyntax: ProtoSyntax? { .proto2 }

        let unknownFields: UnknownFields

        init(from reader: ProtoReader) throws {
            self.unknownFields = try reader.forEachTag { tag in
                try reader.readUnknownField(tag: tag)
            }
        }
    }

    func testNestedMessageUnknownFields() throws {
        let data = Foundation.Data(hexEncoded: """
            12       // (Tag 2 | Length Delimited)
            0A       // Length 10
              0D       // (Tag 1 | Fixed32)
              05000000 // Value 5
              15       // (Tag 2 | Fixed32)
              FFFFFFFF // Value UInt32.max
        """)!
        try test(data: data) { reader in
            let unknownFields = try reader.forEachTag { tag in
                try reader.readUnknownField(tag: tag)
            }

            XCTAssertEqual(unknownFields, [2: Foundation.Data(hexEncoded: "120A0D0500000015FFFFFFFF")])
        }
    }

    func testNestedUnknownFields() throws {
        let data = Foundation.Data(hexEncoded: """
            08       // (Tag 1 | Varint)
            05       // Value 5
            12       // (Tag 2 | Length Delimited)
            0A       // Length 10
              0D       // (Tag 1 | Fixed32)
              05000000 // Value 5
              15       // (Tag 2 | Fixed32)
              FFFFFFFF // Value UInt32.max
            1D       // (Tag 3 | Fixed32)
            FBFFFFFF // Value -5
        """)!
        try test(data: data) { reader in
            let unknownFields = try reader.forEachTag { tag in
                switch tag {
                case 1: XCTAssertEqual(try UInt32(truncatingIfNeeded: reader.readVarint()), 5)
                case 2:
                    let nestedMessage = try reader.decode(NestedMessage.self)
                    XCTAssertEqual(nestedMessage.unknownFields, [1: Foundation.Data(hexEncoded: "0D_05000000"), 2: Foundation.Data(hexEncoded: "15_FFFFFFFF")])
                default: try reader.readUnknownField(tag: tag)
                }
            }

            XCTAssertEqual(unknownFields, [3: Foundation.Data(hexEncoded: "1D_FBFFFFFF")])
        }
    }

    // MARK: - Tests - Reading Primitives

    func testReadFixed32() throws {
        let data = Foundation.Data(hexEncoded: """
            0D       // (Tag 1 | Fixed32)
            05000000 // Value 5
            15       // (Tag 2 | Fixed32)
            FFFFFFFF // Value UInt32.max
        """)!
        try test(data: data) { reader in
            let _ = try reader.forEachTag { tag in
                switch tag {
                case 1: XCTAssertEqual(try reader.readFixed32(), 5)
                case 2: XCTAssertEqual(try reader.readFixed32(), .max)
                default: XCTFail("Unexpected field")
                }
            }
        }
    }

    func testReadFixed64() throws {
        let data = Foundation.Data(hexEncoded: """
            09               // (Tag 1 | Fixed64)
            0500000000000000 // Value 5
            11               // (Tag 2 | Fixed64)
            FFFFFFFFFFFFFFFF // Value UInt64.max
        """)!
        try test(data: data) { reader in
            let _ = try reader.forEachTag { tag in
                switch tag {
                case 1: XCTAssertEqual(try reader.readFixed64(), 5)
                case 2: XCTAssertEqual(try reader.readFixed64(), .max)
                default: XCTFail("Unexpected field")
                }
            }
        }
    }

    func testReadVarint32() throws {
        let data = Foundation.Data(hexEncoded: """
            08         // (Tag 1 | Varint)
            05         // Value 5
            10         // (Tag 2 | Varint)
            AC02       // Value 300
            18         // (Tag 3 | Varint)
            FFFFFFFF0F // Value UInt32.max
        """)!
        try test(data: data) { reader in
            let _ = try reader.forEachTag { tag in
                switch tag {
                case 1: XCTAssertEqual(try UInt32(truncatingIfNeeded: reader.readVarint()), 5)
                case 2: XCTAssertEqual(try UInt32(truncatingIfNeeded: reader.readVarint()), 300)
                case 3: XCTAssertEqual(try UInt32(truncatingIfNeeded: reader.readVarint()), .max)
                default: XCTFail("Unexpected field")
                }
            }
        }
    }

    func testReadVarint64() throws {
        let data = Foundation.Data(hexEncoded: """
            08                   // (Tag 1 | Varint)
            05                   // Value 5
            10                   // (Tag 2 | Varint)
            AC02                 // Value 300
            18                   // (Tag 3 | Varint)
            FFFFFFFFFFFFFFFFFF01 // Value UInt64.max
        """)!
        try test(data: data) { reader in
            let _ = try reader.forEachTag { tag in
                switch tag {
                case 1: XCTAssertEqual(try reader.readVarint(), 5)
                case 2: XCTAssertEqual(try reader.readVarint(), 300)
                case 3: XCTAssertEqual(try reader.readVarint(), .max)
                default: XCTFail("Unexpected field")
                }
            }
        }
    }

    // MARK: - Private Methods

    private func test(data: Foundation.Data, enumStrategy: ProtoDecoder.UnknownEnumValueDecodingStrategy = .throwError, test: (ProtoReader) throws -> Void) rethrows {
        try data.withUnsafeBytes { buffer in
            guard let baseAddress = buffer.baseAddress, buffer.count > 0 else {
                return
            }

            let readBuffer = ReadBuffer(
                storage: baseAddress.bindMemory(to: UInt8.self, capacity: buffer.count),
                count: buffer.count
            )
            let reader = ProtoReader(buffer: readBuffer, enumDecodingStrategy: enumStrategy)
            try test(reader)
        }
    }
}

// MARK: -

extension ProtoReader {

    fileprivate func decode<T>(tag: UInt32, _ decode: () throws -> T) throws -> T {
        var value: T?
        _ = try forEachTag { decodedTag in
            switch decodedTag {
            case tag: value = try decode()
            default: XCTFail("Unexpected tag \(decodedTag)")
            }
        }
        return value!
    }

    func forEachTag(_ decode: (UInt32) throws -> Void) throws -> UnknownFields {
        let token = try beginMessage()
        while let tag = try nextTag(token: token) {
            try decode(tag)
        }
        return try endMessage(token: token)
    }
}
