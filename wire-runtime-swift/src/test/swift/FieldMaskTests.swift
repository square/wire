/*
 * Copyright (C) 2026 Square, Inc.
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
import Wire
import XCTest

final class FieldMaskTests: XCTestCase {
    func testEncodeFieldMask() throws {
        let fieldMask = FieldMask(paths: ["user.display_name", "photo"])

        let data = try ProtoEncoder().encode(fieldMask)

        XCTAssertEqual(
            data,
            Foundation.Data(hexEncoded: "0a11757365722e646973706c61795f6e616d650a0570686f746f")!
        )
    }

    func testDecodeFieldMask() throws {
        let data = Foundation.Data(hexEncoded: "0a11757365722e646973706c61795f6e616d650a0570686f746f")!

        let fieldMask = try ProtoDecoder().decode(FieldMask.self, from: data)

        XCTAssertEqual(fieldMask, FieldMask(paths: ["user.display_name", "photo"]))
    }

    func testTypeURL() {
        XCTAssertEqual(
            FieldMask.protoMessageTypeURL(),
            "type.googleapis.com/google.protobuf.FieldMask"
        )
    }

    func testJSONRoundTrip() throws {
        let jsonData = Foundation.Data(#""user.displayName,photo""#.utf8)

        let fieldMask = try JSONDecoder().decode(FieldMask.self, from: jsonData)
        let encoded = try JSONEncoder().encode(fieldMask)

        XCTAssertEqual(fieldMask, FieldMask(paths: ["user.display_name", "photo"]))
        XCTAssertEqual(String(data: encoded, encoding: .utf8), #""user.displayName,photo""#)
    }

    func testJSONSkipsEmptyPaths() throws {
        let fieldMask = FieldMask(paths: ["", "photo", ""])

        let encoded = try JSONEncoder().encode(fieldMask)
        let decoded = try JSONDecoder().decode(
            FieldMask.self,
            from: Foundation.Data("\",photo,\"".utf8)
        )

        XCTAssertEqual(String(data: encoded, encoding: .utf8), #""photo""#)
        XCTAssertEqual(decoded, FieldMask(paths: ["photo"]))
    }

    func testJSONPreservesLeadingUppercaseAsUnderscore() throws {
        let jsonData = Foundation.Data(#""foo.Bar""#.utf8)

        let fieldMask = try JSONDecoder().decode(FieldMask.self, from: jsonData)
        let encoded = try JSONEncoder().encode(fieldMask)

        XCTAssertEqual(fieldMask, FieldMask(paths: ["foo._bar"]))
        XCTAssertEqual(String(data: encoded, encoding: .utf8), #""foo.Bar""#)
    }

    func testGeneratedMessageWithFieldMask() throws {
        let fieldMask = FieldMask(paths: ["user.display_name", "photo"])
        let otherFieldMask = FieldMask(paths: ["updated_at.seconds"])
        let message = MessageContainingFieldMask {
            $0.mask = fieldMask
            $0.masks = [fieldMask, otherFieldMask]
            $0.masks_by_id = [1: fieldMask, 2: otherFieldMask]
        }

        let data = try ProtoEncoder().encode(message)
        let decoded = try ProtoDecoder().decode(MessageContainingFieldMask.self, from: data)

        XCTAssertEqual(decoded.mask, fieldMask)
        XCTAssertEqual(decoded.masks, [fieldMask, otherFieldMask])
        XCTAssertEqual(decoded.masks_by_id, [1: fieldMask, 2: otherFieldMask])
    }

    func testGeneratedMessageMergesDuplicateSingularFieldMask() throws {
        let data = Foundation.Data(hexEncoded: "0a030a01610a030a0162")!

        let decoded = try ProtoDecoder().decode(MessageContainingFieldMask.self, from: data)

        XCTAssertEqual(decoded.mask, FieldMask(paths: ["a", "b"]))
    }
}
