//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation
import XCTest
@testable import Wire

final class ProtoReaderTests: XCTestCase {

    // MARK: - Tests - Decoding Integers

    func testDecodeFixedInt32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "0D_FBFFFFFF")!)
        let value = try reader.decode(tag: 1) { try reader.decode(Int32.self, encoding: .fixed) }
        XCTAssertEqual(value, -5)
    }

    func testDecodeFixedInt64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "09_FBFFFFFFFFFFFFFF")!)
        let value = try reader.decode(tag: 1) { try reader.decode(Int64.self, encoding: .fixed) }
        XCTAssertEqual(value, -5)
    }

    func testDecodeFixedUInt32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "0D_05000000")!)
        let value = try reader.decode(tag: 1) { try reader.decode(UInt32.self, encoding: .fixed) }
        XCTAssertEqual(value, 5)
    }

    func testDecodeFixedUInt64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "09_0500000000000000")!)
        let value = try reader.decode(tag: 1) { try reader.decode(UInt64.self, encoding: .fixed) }
        XCTAssertEqual(value, 5)
    }

    func testDecodeSignedInt32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_09")!)
        let value = try reader.decode(tag: 1) { try reader.decode(Int32.self, encoding: .signed) }
        XCTAssertEqual(value, -5)
    }

    func testDecodeSignedInt64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_09")!)
        let value = try reader.decode(tag: 1) { try reader.decode(Int64.self, encoding: .signed) }
        XCTAssertEqual(value, -5)
    }

    func testDecodeVarintInt32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_FBFFFFFF0F")!)
        let value = try reader.decode(tag: 1) { try reader.decode(Int32.self, encoding: .variable) }
        XCTAssertEqual(value, -5)
    }

    func testDecodeVarintInt64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_FBFFFFFFFFFFFFFFFF01")!)
        let value = try reader.decode(tag: 1) { try reader.decode(Int64.self, encoding: .variable) }
        XCTAssertEqual(value, -5)
    }

    func testDecodeVarintUInt32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_05")!)
        let value = try reader.decode(tag: 1) { try reader.decode(UInt32.self, encoding: .variable) }
        XCTAssertEqual(value, 5)
    }

    func testDecodeVarintUInt64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_05")!)
        let value = try reader.decode(tag: 1) { try reader.decode(UInt64.self, encoding: .variable) }
        XCTAssertEqual(value, 5)
    }

    // MARK: - Tests - Decoding Messages And More

    func testDecodeBool() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_01")!)
        let value = try reader.decode(tag: 1) { try reader.decode(Bool.self) }
        XCTAssertEqual(value, true)
    }

    func testDecodeData() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            0A           // (tag 1 | Length Delimited)
            06           // Data length
            001122334455 // Random data
        """)!)
        let value = try reader.decode(tag: 1) { try reader.decode(Data.self) }
        XCTAssertEqual(value, Data(hexEncoded: "001122334455")!)
    }

    func testDecodeMessage() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            0A       // (tag 1 | Length Delimited)
            04       // Length of name
            4C756B65 // Name value "Luke"
            10       // (tag 2 | Varint)
            05       // ID value 5
        """)!)
        let message = Person(name: "Luke", id: 5)
        XCTAssertEqual(try reader.decode(Person.self), message)
    }

    func testDecodeString() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "666F6F")!)
        XCTAssertEqual(try reader.decode(String.self), "foo")
    }

    // MARK: - Tests - Decoding Enums

    func testDecodeEnum() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_01")!)
        let value = try reader.decode(tag: 1) { try reader.decode(Person.PhoneType.self) }
        XCTAssertEqual(value, .HOME)
    }

    // MARK: - Tests - Decoding Repeated Fields

    func testDecodeRepeatedBools() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_01_08_00")!)
        var values: [Bool] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, [true, false])
    }

    func testDecodePackedRepeatedBools() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "0A_02_01_00")!)
        var values: [Bool] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, [true, false])
    }

    func testDecodeRepeatedEnums() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_01_08_00")!)
        var values: [Person.PhoneType] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, [.HOME, .MOBILE])
    }

    func testDecodePackedRepeatedEnums() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "0A_02_01_00")!)
        var values: [Person.PhoneType] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, [.HOME, .MOBILE])
    }

    func testDecodeRepeatedStrings() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            0A     // (Tag 1 | Length Delimited)
            03     // Length 3
            666F6F // Value "foo"
            0A     // (Tag 1 | Length Delimited)
            03     // Length 3
            626172 // Value "bar"
        """)!)
        var values: [String] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, ["foo", "bar"])
    }

    func testDecodeRepeatedDoubles() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            09               // (Tag 1 | Fixed64)
            8D976E1283C0F33F // Value 1.2345
            09               // (Tag 1 | Fixed64)
            0E2DB29DEF271B40 // Value 6.7890
        """)!)
        var values: [Double] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, [1.2345, 6.7890])
    }

    func testDecodePackedRepeatedDoubles() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            0A
            10
            8D976E1283C0F33F
            0E2DB29DEF271B40
        """)!)
        var values: [Double] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, [1.2345, 6.7890])
    }

    func testDecodeRepeatedFloats() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "0D_19049E3F_0D_7D3FD940")!)
        var values: [Float] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, [1.2345, 6.7890])
    }

    func testDecodePackedRepeatedFloats() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "0A_08_19049E3F_7D3FD940")!)
        var values: [Float] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, [1.2345, 6.7890])
    }

    func testDecodeRepeatedFixedUInt32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            0D       // (Tag 1 | Fixed32)
            01000000 // Value 1
            0D       // (Tag 1 | Fixed 32)
            FFFFFFFF // Value UInt32.max
        """)!)
        var values: [UInt32] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .fixed) }

        XCTAssertEqual(values, [1, .max])
    }

    func testDecodePackedRepeatedFixedUInt32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            0A       // (Tag 1 | Length Delimited)
            08       // Length 8
            01000000 // Value 1
            FFFFFFFF // Value UInt32.max
        """)!)
        var values: [UInt32] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .fixed) }

        XCTAssertEqual(values, [1, .max])
    }

    func testDecodeRepeatedVarintUInt32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            08         // (Tag 1 | Varint)
            01         // Value 1
            08         // (Tag 1 | Varint)
            FFFFFFFF0F // Value UInt32.max
        """)!)
        var values: [UInt32] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .variable) }

        XCTAssertEqual(values, [1, .max])
    }

    func testDecodePackedRepeatedVarintUInt32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            0A         // (Tag 1 | Varint)
            06         // Length 6
            01         // Value 1
            FFFFFFFF0F // Value UInt32.max
        """)!)
        var values: [UInt32] = []
        try reader.decode(tag: 1) { try reader.decode(into: &values, encoding: .variable) }

        XCTAssertEqual(values, [1, .max])
    }

    // MARK: - Tests - Decoding Maps

    func testDecodeUInt32ToUInt32FixedMap() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
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
        """)!)

        var values: [UInt32: UInt32] = [:]
        try reader.decode(tag: 1) { try reader.decode(into: &values, keyEncoding: .fixed, valueEncoding: .fixed) }

        XCTAssertEqual(values, [1: 2, 3: 4])
    }

    func testDecodeUInt32ToUInt64VarintMap() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
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
        """)!)

        var values: [UInt32: UInt64] = [:]
        try reader.decode(tag: 1) { try reader.decode(into: &values, keyEncoding: .variable, valueEncoding: .variable) }

        XCTAssertEqual(values, [1: 2, 3: 4])
    }

    func testDecodeUInt32ToStringMap() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
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
        """)!)

        var values: [UInt32: String] = [:]
        try reader.decode(tag: 1) { try reader.decode(into: &values, keyEncoding: .variable) }

        XCTAssertEqual(values, [1: "two", 3: "four"])
    }

    func testDecodeStringToEnumMap() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
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
        """)!)

        var values: [String: Person.PhoneType] = [:]
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, ["a": .HOME, "b": .MOBILE])
    }

    func testDecodeStringToInt32FixedMap() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
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
        """)!)

        var values: [String: Int32] = [:]
        try reader.decode(tag: 1) { try reader.decode(into: &values, valueEncoding: .fixed) }

        XCTAssertEqual(values, ["a": -1, "b": -2])
    }

    func testDecodeStringToStringMap() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
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
        """)!)

        var values: [String: String] = [:]
        try reader.decode(tag: 1) { try reader.decode(into: &values) }

        XCTAssertEqual(values, ["a": "two", "b": "four"])
    }

    // MARK: - Tests - Groups

    func testSkippingGroup() throws {
        let data = Data(hexEncoded: """
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
        let reader = ProtoReader(data: data)
        let unknownFields = try reader.forEachTag { tag in
            XCTFail("The one group tag should have been skipped")
        }

        // The entire data should have been skipped.
        XCTAssertEqual(unknownFields, data)
    }

    func testSkippingRepeatedGroup() throws {
        let key = ProtoWriter.makeFieldKey(tag: 3, wireType: .endGroup)
        print("key: \(key)")

        var data = Data(hexEncoded: """
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

        let reader = ProtoReader(data: data)
        let unknownFields = try reader.forEachTag { tag in
            XCTFail("The one group tag should have been skipped")
        }

        XCTAssertEqual(unknownFields, data)
    }

    func testSkippingNestedGroups() throws {
        let key = ProtoWriter.makeFieldKey(tag: 3, wireType: .endGroup)
        print("key: \(key)")

        let data = Data(hexEncoded: """
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

        let reader = ProtoReader(data: data)
        let unknownFields = try reader.forEachTag { tag in
            XCTFail("The one group tag should have been skipped")
        }

        XCTAssertEqual(unknownFields, data)
    }

    // MARK: - Tests - Unknown Fields

    func testUnknownFields() throws {
        let data = Data(hexEncoded: """
            0D       // (Tag 1 | Fixed32)
            05000000 // Value 5
            15       // (Tag 2 | Fixed32)
            FFFFFFFF // Value UInt32.max
        """)!
        let reader = ProtoReader(data: data)

        let unknownFields = try reader.forEachTag { tag in
            switch tag {
            case 1: XCTAssertEqual(try reader.readFixed32(), 5)
            default: try reader.readUnknownField(tag: tag)
            }
        }

        XCTAssertEqual(unknownFields, Data(hexEncoded: "15_FFFFFFFF"))
    }

    func testNonContiguousUnknownFields() throws {
        let data = Data(hexEncoded: """
            08         // (Tag 1 | Varint)
            05         // Value 5
            10         // (Tag 2 | Varint)
            AC02       // Value 300
            18         // (Tag 3 | Varint)
            FFFFFFFF0F // Value UInt32.max
        """)!
        let reader = ProtoReader(data: data)
        let unknownFields = try reader.forEachTag { tag in
            switch tag {
            case 2: XCTAssertEqual(try reader.readVarint32(), 300)
            default: try reader.readUnknownField(tag: tag)
            }
        }

        XCTAssertEqual(unknownFields, Data(hexEncoded: "08_05_18_FFFFFFFF0F"))
    }

    struct NestedMessage: ProtoDecodable {
        let unknownFields: Data

        init(from reader: ProtoReader) throws {
            self.unknownFields = try reader.forEachTag { tag in
                try reader.readUnknownField(tag: tag)
            }
        }
    }

    func testNestedUnknownFields() throws {
        let data = Data(hexEncoded: """
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
        let reader = ProtoReader(data: data)
        let unknownFields = try reader.forEachTag { tag in
            switch tag {
            case 1: XCTAssertEqual(try reader.readVarint32(), 5)
            case 2:
                let nestedMessage = try reader.decode(NestedMessage.self)
                XCTAssertEqual(nestedMessage.unknownFields, Data(hexEncoded: "0D_05000000_15_FFFFFFFF"))
            default: try reader.readUnknownField(tag: tag)
            }
        }

        XCTAssertEqual(unknownFields, Data(hexEncoded: "1D_FBFFFFFF"))
    }

    // MARK: - Tests - Reading Primitives

    func testReadFixed32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            0D       // (Tag 1 | Fixed32)
            05000000 // Value 5
            15       // (Tag 2 | Fixed32)
            FFFFFFFF // Value UInt32.max
        """)!)
        let _ = try reader.forEachTag { tag in
            switch tag {
            case 1: XCTAssertEqual(try reader.readFixed32(), 5)
            case 2: XCTAssertEqual(try reader.readFixed32(), .max)
            default: XCTFail("Unexpected field")
            }
        }
    }

    func testReadFixed64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            09               // (Tag 1 | Fixed64)
            0500000000000000 // Value 5
            11               // (Tag 2 | Fixed64)
            FFFFFFFFFFFFFFFF // Value UInt64.max
        """)!)
        let _ = try reader.forEachTag { tag in
            switch tag {
            case 1: XCTAssertEqual(try reader.readFixed64(), 5)
            case 2: XCTAssertEqual(try reader.readFixed64(), .max)
            default: XCTFail("Unexpected field")
            }
        }
    }

    func testReadVarint32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            08         // (Tag 1 | Varint)
            05         // Value 5
            10         // (Tag 2 | Varint)
            AC02       // Value 300
            18         // (Tag 3 | Varint)
            FFFFFFFF0F // Value UInt32.max
        """)!)
        let _ = try reader.forEachTag { tag in
            switch tag {
            case 1: XCTAssertEqual(try reader.readVarint32(), 5)
            case 2: XCTAssertEqual(try reader.readVarint32(), 300)
            case 3: XCTAssertEqual(try reader.readVarint32(), .max)
            default: XCTFail("Unexpected field")
            }
        }
    }

    func testReadVarint64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: """
            08                   // (Tag 1 | Varint)
            05                   // Value 5
            10                   // (Tag 2 | Varint)
            AC02                 // Value 300
            18                   // (Tag 3 | Varint)
            FFFFFFFFFFFFFFFFFF01 // Value UInt64.max
        """)!)
        let _ = try reader.forEachTag { tag in
            switch tag {
            case 1: XCTAssertEqual(try reader.readVarint64(), 5)
            case 2: XCTAssertEqual(try reader.readVarint64(), 300)
            case 3: XCTAssertEqual(try reader.readVarint64(), .max)
            default: XCTFail("Unexpected field")
            }
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

}
