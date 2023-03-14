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
import XCTest
@testable import Wire

final class WriteBufferTests: XCTestCase {

    func testInsertAtBeginning() {
        let buffer = WriteBuffer()
        buffer.append(Foundation.Data(hexEncoded: "2233")!)

        buffer.insert(count: 2, at: 0)

        // The contents of the new bytes start off undefined,
        // so set them to something known for testing comparison.
        buffer.set(0x00, at: 0)
        buffer.set(0x11, at: 1)

        XCTAssertEqual(Foundation.Data(buffer, copyBytes: true), Foundation.Data(hexEncoded: "00112233"))
    }

    func testRemoveAtBeginning() {
        let buffer = WriteBuffer()
        buffer.append(Foundation.Data(hexEncoded: "0011")!)

        buffer.remove(count: 2, at: 0)

        XCTAssertEqual(Foundation.Data(buffer, copyBytes: true), Foundation.Data())
    }

    func testRemoveAtMiddle() {
        let buffer = WriteBuffer()
        buffer.append(Foundation.Data(hexEncoded: "001122")!)

        buffer.remove(count: 1, at: 1)

        XCTAssertEqual(Foundation.Data(buffer, copyBytes: true), Foundation.Data(hexEncoded: "0022"))
    }

    func testRemoveAtEnd() {
        let buffer = WriteBuffer()
        buffer.append(Foundation.Data(hexEncoded: "001122")!)

        buffer.remove(count: 1, at: 2)

        XCTAssertEqual(Foundation.Data(buffer, copyBytes: true), Foundation.Data(hexEncoded: "0011"))
    }

    func testAppendEmptyFirst() {
        let buffer = WriteBuffer()
        buffer.append(Foundation.Data())

        XCTAssertEqual(Foundation.Data(buffer, copyBytes: true), Foundation.Data())
    }

}
