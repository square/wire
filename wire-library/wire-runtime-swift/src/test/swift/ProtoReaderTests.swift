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

    // MARK: - Tests - Reading Primitives

    func testReadFixed32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "05000000_FFFFFFFF")!)
        XCTAssertEqual(try reader.readFixed32(), 5)
        XCTAssertEqual(try reader.readFixed32(), .max)
    }

    func testReadFixed64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "0500000000000000_FFFFFFFFFFFFFFFF")!)
        XCTAssertEqual(try reader.readFixed64(), 5)
        XCTAssertEqual(try reader.readFixed64(), .max)
    }

    func testReadVarint32() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "05_AC02_FFFFFFFF0F")!)
        XCTAssertEqual(try reader.readVarint32(), 5)
        XCTAssertEqual(try reader.readVarint32(), 300)
        XCTAssertEqual(try reader.readVarint32(), .max)
    }

    func testReadVarint64() throws {
        let reader = ProtoReader(data: Data(hexEncoded: "05_AC02_FFFFFFFFFFFFFFFFFF01")!)
        XCTAssertEqual(try reader.readVarint64(), 5)
        XCTAssertEqual(try reader.readVarint64(), 300)
        XCTAssertEqual(try reader.readVarint64(), .max)
    }

}