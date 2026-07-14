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
import Wire
import XCTest

final class ProtoDecoderTests: XCTestCase {

    func testDecodeEmptyData() throws {
        let decoder = ProtoDecoder()
        let object = try decoder.decode(SimpleOptional2.self, from: Foundation.Data())

        XCTAssertEqual(object, SimpleOptional2())
    }

    func testDuplicatedSingularMessageFieldsAreMerged() throws {
        // Encoding two messages back to back is equivalent to a single message in which the
        // singular message field `nested` appears twice. Per the protobuf specification the
        // occurrences merge: `name` takes the value of the last occurrence while
        // `partially_redacted`, absent from the last occurrence, is kept from the first.
        let first = Redacted(name: "message") {
            $0.nested = Redacted2(name: "first") {
                $0.partially_redacted = Redacted3(name: "kept")
            }
        }
        let second = Redacted(name: "message") {
            $0.nested = Redacted2(name: "last")
        }

        var data = try ProtoEncoder().encode(first)
        data.append(try ProtoEncoder().encode(second))
        let decoded = try ProtoDecoder().decode(Redacted.self, from: data)

        XCTAssertEqual(decoded.nested?.name, "last")
        XCTAssertEqual(decoded.nested?.partially_redacted?.name, "kept")
    }

    func testRequiredFieldsMayBeSplitAcrossDuplicatedMessageOccurrences() throws {
        // The first RequiredPair occurrence supplies `a`; the second supplies `b`.
        let data = Foundation.Data(hexEncoded: "0a030a01610a03120162")!

        let decoded = try ProtoDecoder().decode(ContainsRequiredPair.self, from: data)

        XCTAssertEqual(decoded.pair?.a, "a")
        XCTAssertEqual(decoded.pair?.b, "b")
    }

    func testDuplicatedMessagesCannotResetRecursionLimit() throws {
        let data = duplicatedNestedMessage(depth: 100)

        XCTAssertThrowsError(try ProtoDecoder().decode(RecursiveMessage.self, from: data)) { error in
            guard case ProtoDecoder.Error.recursionLimitExceeded = error else {
                return XCTFail("Unexpected error: \(error)")
            }
        }
    }

    func testDecodeEmptySizeDelimitedData() throws {
        let decoder = ProtoDecoder()
        let object = try decoder.decodeSizeDelimited(SimpleOptional2.self, from: Foundation.Data())

        XCTAssertEqual(object, [])
    }

    func testDecodeSizeDelimitedRejectsUnrepresentableSize() throws {
        let decoder = ProtoDecoder()
        let data = Foundation.Data(hexEncoded: """
            FFFFFFFFFFFFFFFFFF01 // UInt64.max
        """)!

        XCTAssertThrowsError(
            try decoder.decodeSizeDelimited(SimpleOptional2.self, from: data)
        ) { error in
            guard case ProtoDecoder.Error.unexpectedEndOfData = error else {
                XCTFail("Unexpected error: \(error)")
                return
            }
        }
    }

    func testDecodeSizeDelimitedRejectsTruncatedSize() throws {
        let decoder = ProtoDecoder()
        let data = Foundation.Data(hexEncoded: """
            80 // Truncated size varint
        """)!

        XCTAssertThrowsError(
            try decoder.decodeSizeDelimited(SimpleOptional2.self, from: data)
        ) { error in
            guard case ProtoDecoder.Error.unexpectedEndOfData = error else {
                XCTFail("Unexpected error: \(error)")
                return
            }
        }
    }

    func testDecodeEmptyDataTwice() throws {
        let decoder = ProtoDecoder()
        // The empty message case is optimized to reuse objects, so make sure
        // that no unexpected state changes persist from one decode to another.
        let object1 = try decoder.decode(SimpleOptional2.self, from: Foundation.Data())
        let object2 = try decoder.decode(SimpleOptional2.self, from: Foundation.Data())

        XCTAssertEqual(object1, SimpleOptional2())
        XCTAssertEqual(object2, SimpleOptional2())
    }

    private func duplicatedNestedMessage(depth: Int) -> Foundation.Data {
        guard depth > 0 else { return Foundation.Data() }

        let nested = duplicatedNestedMessage(depth: depth - 1)
        var result = Foundation.Data([0x0a, 0x00]) // First child occurrence is empty.
        result.append(0x0a) // Second child occurrence contains the next level.
        appendVarint(UInt64(nested.count), to: &result)
        result.append(nested)
        return result
    }

    private func appendVarint(_ value: UInt64, to data: inout Foundation.Data) {
        var value = value
        while value >= 0x80 {
            data.append(UInt8(truncatingIfNeeded: value) | 0x80)
            value >>= 7
        }
        data.append(UInt8(value))
    }
}
