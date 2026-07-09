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
}
