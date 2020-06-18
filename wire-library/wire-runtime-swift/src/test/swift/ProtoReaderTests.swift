//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation
import XCTest
@testable import WireRuntime

final class ProtoReaderTests: XCTestCase {

    // MARK: - Tests - Decoding Integers

    func testDecodeFixedInt32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "FBFFFFFF")!)
        XCTAssertEqual(try reader.decode(Int32.self, encoding: .fixed), -5)
    }

    func testDecodeFixedInt64() {
        let reader = ProtoReader(data: Data(hexEncoded: "FBFFFFFFFFFFFFFF")!)
        XCTAssertEqual(try reader.decode(Int64.self, encoding: .fixed), -5)
    }

    func testDecodeFixedUInt32() {
        let reader = ProtoReader(data: Data(hexEncoded: "05000000")!)
        XCTAssertEqual(try reader.decode(UInt32.self, encoding: .fixed), 5)
    }

    func testDecodeFixedUInt64() {
        let reader = ProtoReader(data: Data(hexEncoded: "0500000000000000")!)
        XCTAssertEqual(try reader.decode(UInt64.self, encoding: .fixed), 5)
    }

    func testDecodeSignedInt32() {
        let reader = ProtoReader(data: Data(hexEncoded: "09")!)
        XCTAssertEqual(try reader.decode(Int32.self, encoding: .signed), -5)
    }

    func testDecodeSignedInt64() {
        let reader = ProtoReader(data: Data(hexEncoded: "09")!)
        XCTAssertEqual(try reader.decode(Int64.self, encoding: .signed), -5)
    }

    func testDecodeVarintInt32() {
        let reader = ProtoReader(data: Data(hexEncoded: "FBFFFFFF0F")!)
        XCTAssertEqual(try reader.decode(Int32.self, encoding: .variable), -5)
    }

    func testDecodeVarintInt64() {
        let reader = ProtoReader(data: Data(hexEncoded: "FBFFFFFFFFFFFFFFFF01")!)
        XCTAssertEqual(try reader.decode(Int64.self, encoding: .variable), -5)
    }

    func testDecodeVarintUInt32() {
        let reader = ProtoReader(data: Data(hexEncoded: "05")!)
        XCTAssertEqual(try reader.decode(UInt32.self, encoding: .variable), 5)
    }

    func testDecodeVarintUInt64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "05")!)
        XCTAssertEqual(try reader.decode(UInt64.self, encoding: .variable), 5)
    }

    // MARK: - Tests - Decoding Messages And More

    func testDecodeBool() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "01")!)
        XCTAssertEqual(try reader.decode(Bool.self), true)
    }

    func testDecodeData() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "001122334455")!)
        XCTAssertEqual(try reader.decode(Data.self), Data(hexEncoded: "001122334455")!)
    }

    func testDecodeMessage() throws {
        // 0A is (tag 1 << 3 | .lengthDelimited) for the name
        // 04 is the name length ("Luke")
        // 4C756B65 is the text "Luke"
        // 10 is (tag 2 << 3 | .varint) for the ID
        // 05 is the ID value
        let reader = ProtoReader(data: Data(hexEncoded: "0A_04_4C756B65_10_05")!)
        let message = Person(name: "Luke", id: 5)
        XCTAssertEqual(try reader.decode(Person.self), message)
    }

    func testDecodeString() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "666F6F")!)
        XCTAssertEqual(try reader.decode(String.self), "foo")
    }

    // MARK: - Tests - Decoding Enums

    func testDecodeEnum() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "01")!)
        XCTAssertEqual(try reader.decode(Person.PhoneType.self), .HOME)
    }

    // MARK: - Tests - Decoding Repeated Fields

    func testDecodeRepeatedStrings() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "0A_03_666F6F_0A_03_626172")!)
        var strings: [String] = []
        _ = try reader.forEachTag { tag in
            switch tag {
            case 1: try reader.decode(into: &strings)
            default: XCTFail("Unexpected tag")
            }
        }

        XCTAssertEqual(strings, ["foo", "bar"])
    }

    // MARK: - Tests - Unknown Fields

    func testUnknownFields() throws {
        let data = Data(hexEncoded: "0D_05000000_15_FFFFFFFF")!
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
        let data = Data(hexEncoded: "08_05_10_AC02_18_FFFFFFFF0F")!
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
        // 08_05 - Field 1, varint value 5
        // 12_0A - Field 2 (nested message) and length
        // 0D_05000000_15_FFFFFFFF - nested message
        // 1D_FBFFFFFF - Field 3
        let data = Data(hexEncoded: "08_05_12_0A_0D_05000000_15_FFFFFFFF_1D_FBFFFFFF")!
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
        let reader = ProtoReader(data: Data(hexEncoded: "0D_05000000_15_FFFFFFFF")!)
        let _ = try reader.forEachTag { tag in
            switch tag {
            case 1: XCTAssertEqual(try reader.readFixed32(), 5)
            case 2: XCTAssertEqual(try reader.readFixed32(), .max)
            default: XCTFail("Unexpected field")
            }
        }
    }

    func testReadFixed64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "09_0500000000000000_11_FFFFFFFFFFFFFFFF")!)
        let _ = try reader.forEachTag { tag in
            switch tag {
            case 1: XCTAssertEqual(try reader.readFixed64(), 5)
            case 2: XCTAssertEqual(try reader.readFixed64(), .max)
            default: XCTFail("Unexpected field")
            }
        }
    }

    func testReadVarint32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "08_05_10_AC02_18_FFFFFFFF0F")!)
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
        let reader = ProtoReader(data: Data(hexEncoded: "08_05_10_AC02_18_FFFFFFFFFFFFFFFFFF01")!)
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
