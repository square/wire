//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation
import XCTest
@testable import WireRuntime

final class ProtoWriterTests: XCTestCase {

    // MARK: - Tests - Encoding Integers

    func testEncodeFixedInt32() {
        let writer = ProtoWriter()
        try! writer.encode(tag: 1, value: Int32(-5), encoding: .fixed)

        // 0D is (tag 1 << 3 | .fixed32)
        XCTAssertEqual(writer.data, Data(hexEncoded: "0D_FBFFFFFF"))
    }

    func testEncodeFixedInt64() {
        let writer = ProtoWriter()
        try! writer.encode(tag: 1, value: Int64(-5), encoding: .fixed)

        // 09 is (tag 1 << 3 | .fixed64)
        XCTAssertEqual(writer.data, Data(hexEncoded: "09_FBFFFFFFFFFFFFFF"))
    }

    func testEncodeFixedUInt32() {
        let writer = ProtoWriter()
        try! writer.encode(tag: 1, value: UInt32(5), encoding: .fixed)

        // 0D is (tag 1 << 3 | .fixed32)
        XCTAssertEqual(writer.data, Data(hexEncoded: "0D_05000000"))
    }

    func testEncodeFixedUInt64() {
        let writer = ProtoWriter()
        try! writer.encode(tag: 1, value: UInt64(5), encoding: .fixed)

        // 09 is (tag 1 << 3 | .fixed64)
        XCTAssertEqual(writer.data, Data(hexEncoded: "09_0500000000000000"))
    }

    func testEncodeSignedInt32() {
        let writer = ProtoWriter()
        try! writer.encode(tag: 1, value: Int32(-5), encoding: .signed)

        // 08 is (tag 1 << 3 | .varint)
        XCTAssertEqual(writer.data, Data(hexEncoded: "08_09"))
    }

    func testEncodeSignedInt64() {
        let writer = ProtoWriter()
        try! writer.encode(tag: 1, value: Int64(-5), encoding: .signed)

        // 08 is (tag 1 << 3 | .varint)
        XCTAssertEqual(writer.data, Data(hexEncoded: "08_09"))
    }

    func testEncodeVarintInt32() {
        let writer = ProtoWriter()
        try! writer.encode(tag: 1, value: Int32(-5), encoding: .variable)

        // 08 is (tag 1 << 3 | .varint)
        XCTAssertEqual(writer.data, Data(hexEncoded: "08_FBFFFFFF0F"))
    }

    func testEncodeVarintInt64() {
        let writer = ProtoWriter()
        try! writer.encode(tag: 1, value: Int64(-5), encoding: .variable)

        // 08 is (tag 1 << 3 | .varint)
        XCTAssertEqual(writer.data, Data(hexEncoded: "08_FBFFFFFFFFFFFFFFFF01"))
    }

    func testEncodeVarintUInt32() {
        let writer = ProtoWriter()
        try! writer.encode(tag: 1, value: UInt32(5), encoding: .variable)

        // 08 is (tag 1 << 3 | .varint)
        XCTAssertEqual(writer.data, Data(hexEncoded: "08_05"))
    }

    func testEncodeVarintUInt64() {
        let writer = ProtoWriter()
        try! writer.encode(tag: 1, value: UInt64(5), encoding: .variable)

        // 08 is (tag 1 << 3 | .varint)
        XCTAssertEqual(writer.data, Data(hexEncoded: "08_05"))
    }

    // MARK: - Tests - Writing Primitives

    func testWriteFixed32() {
        let writer = ProtoWriter()
        writer.writeFixed32(5)
        XCTAssertEqual(writer.data, Data(hexEncoded: "05000000"))

        writer.writeFixed32(UInt32.max)
        XCTAssertEqual(writer.data, Data(hexEncoded: "05000000_FFFFFFFF"))
    }

    func testWriteFixed64() {
        let writer = ProtoWriter()
        writer.writeFixed64(5)
        XCTAssertEqual(writer.data, Data(hexEncoded: "0500000000000000"))

        writer.writeFixed64(UInt64.max)
        XCTAssertEqual(writer.data, Data(hexEncoded: "0500000000000000_FFFFFFFFFFFFFFFF"))
    }

    func testWriteVarint32() {
        let writer = ProtoWriter()
        writer.writeVarint(UInt32(5))
        XCTAssertEqual(writer.data, Data(hexEncoded: "05"))

        writer.writeVarint(UInt32(300))
        XCTAssertEqual(writer.data, Data(hexEncoded: "05_AC02"))

        writer.writeVarint(UInt32.max)
        XCTAssertEqual(writer.data, Data(hexEncoded: "05_AC02_FFFFFFFF0F"))
    }

    func testWriteVarint64() {
        let writer = ProtoWriter()
        writer.writeVarint(UInt64(5))
        XCTAssertEqual(writer.data, Data(hexEncoded: "05"))

        writer.writeVarint(UInt64(300))
        XCTAssertEqual(writer.data, Data(hexEncoded: "05_AC02"))

        writer.writeVarint(UInt64.max)
        XCTAssertEqual(writer.data, Data(hexEncoded: "05_AC02_FFFFFFFFFFFFFFFFFF01"))
    }

}