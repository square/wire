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

final class Int_AdditionsTests: XCTestCase {

	func testVarintSize() {
		XCTAssertEqual(UInt32(1).varintSize, 1)
		XCTAssertEqual(UInt32(127).varintSize, 1)
		XCTAssertEqual(UInt32(128).varintSize, 2)
		XCTAssertEqual(UInt32(16383).varintSize, 2)
		XCTAssertEqual(UInt32(16384).varintSize, 3)
		XCTAssertEqual(UInt32(2097151).varintSize, 3)
		XCTAssertEqual(UInt32(2097152).varintSize, 4)
		XCTAssertEqual(UInt32(268435455).varintSize, 4)
	}

    func testZigZagDecodedUInt32() {
        XCTAssertEqual(UInt32(0).zigZagDecoded(), 0)
        XCTAssertEqual(UInt32(1).zigZagDecoded(), -1)
        XCTAssertEqual(UInt32(2).zigZagDecoded(), 1)
        XCTAssertEqual(UInt32(3).zigZagDecoded(), -2)
        XCTAssertEqual(UInt32(4294967294).zigZagDecoded(), 2147483647)
        XCTAssertEqual(UInt32(4294967295).zigZagDecoded(), -2147483648)
    }

    func testZigZagDecodedUInt64() {
        XCTAssertEqual(UInt64(0).zigZagDecoded(), 0)
        XCTAssertEqual(UInt64(1).zigZagDecoded(), -1)
        XCTAssertEqual(UInt64(2).zigZagDecoded(), 1)
        XCTAssertEqual(UInt64(3).zigZagDecoded(), -2)
        XCTAssertEqual(UInt64(4294967294).zigZagDecoded(), 2147483647)
        XCTAssertEqual(UInt64(4294967295).zigZagDecoded(), -2147483648)
    }

    func testZigZagEncodedInt32() {
        XCTAssertEqual(Int32(0).zigZagEncoded(), 0)
        XCTAssertEqual(Int32(-1).zigZagEncoded(), 1)
        XCTAssertEqual(Int32(1).zigZagEncoded(), 2)
        XCTAssertEqual(Int32(-2).zigZagEncoded(), 3)
        XCTAssertEqual(Int32(2147483647).zigZagEncoded(), 4294967294)
        XCTAssertEqual(Int32(-2147483648).zigZagEncoded(), 4294967295)
    }

    func testZigZagEncodedInt64() {
        XCTAssertEqual(Int64(0).zigZagEncoded(), 0)
        XCTAssertEqual(Int64(-1).zigZagEncoded(), 1)
        XCTAssertEqual(Int64(1).zigZagEncoded(), 2)
        XCTAssertEqual(Int64(-2).zigZagEncoded(), 3)
        XCTAssertEqual(Int64(2147483647).zigZagEncoded(), 4294967294)
        XCTAssertEqual(Int64(-2147483648).zigZagEncoded(), 4294967295)
    }

}
