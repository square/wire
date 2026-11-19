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
@testable import WireTests

/// Covers proto2 `required` fields that declare an explicit `default`.
final class RequiredDefaultsTests : XCTestCase {
    private let protoDefaultLabel = "a$b \"c\" \\d\n"

    /// A message whose required fields all hold their declared defaults.
    private func defaultValued() -> SwiftRequiredDefaults {
        SwiftRequiredDefaults(
            count: 5,
            label: protoDefaultLabel,
            payload: Data("abc".utf8),
            color: .GREEN,
            flag: true,
            ratio: 1.5,
            offset: -2.25,
            big: -9_000_000_000,
            unsigned_count: UInt32.max,
            secret: "hunter2"
        )
    }

    /// A message whose required fields all differ from their declared defaults.
    private func otherValued() -> SwiftRequiredDefaults {
        SwiftRequiredDefaults(
            count: 0,
            label: "",
            payload: Data(),
            color: .RED,
            flag: false,
            ratio: 0,
            offset: 0,
            big: 0,
            unsigned_count: 0,
            secret: ""
        ) {
            $0.optional_count = 9
        }
    }

    func testBinaryRoundTripOfDefaultValues() throws {
        let message = defaultValued()

        let data = try ProtoEncoder().encode(message)
        let decoded = try ProtoDecoder().decode(SwiftRequiredDefaults.self, from: data)

        XCTAssertEqual(decoded, message)
        XCTAssertEqual(decoded.count, 5)
        XCTAssertEqual(decoded.label, protoDefaultLabel)
        XCTAssertEqual(decoded.payload, Data("abc".utf8))
        XCTAssertEqual(decoded.color, .GREEN)
        XCTAssertEqual(decoded.flag, true)
        XCTAssertEqual(decoded.ratio, 1.5)
        XCTAssertEqual(decoded.offset, -2.25)
        XCTAssertEqual(decoded.big, -9_000_000_000)
        XCTAssertEqual(decoded.unsigned_count, UInt32.max)
        XCTAssertEqual(decoded.secret, "hunter2")
    }

    func testBinaryRoundTripOfOtherValues() throws {
        let message = otherValued()

        let data = try ProtoEncoder().encode(message)
        let decoded = try ProtoDecoder().decode(SwiftRequiredDefaults.self, from: data)

        XCTAssertEqual(decoded, message)
        XCTAssertEqual(decoded.count, 0)
        XCTAssertEqual(decoded.color, .RED)
        XCTAssertEqual(decoded.flag, false)
        XCTAssertEqual(decoded.optional_count, 9)
    }

    func testMissingRequiredFieldIsStillAnError() throws {
        XCTAssertThrowsError(try ProtoDecoder().decode(SwiftRequiredDefaults.self, from: Data())) { error in
            guard case let ProtoDecoder.Error.missingRequiredField(_, fieldName) = error else {
                return XCTFail("Unexpected error: \(error)")
            }
            XCTAssertEqual(fieldName, "count")
        }
    }

    func testOptionalFieldWithDefaultStillFallsBack() throws {
        var message = defaultValued()

        XCTAssertNil(message.optional_count)
        XCTAssertEqual(message.$optional_count, 7)

        message.optional_count = 9
        XCTAssertEqual(message.$optional_count, 9)
    }

    func testJsonRoundTripOfDefaultValues() throws {
        let message = defaultValued()
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys

        let json = try encoder.encode(message)
        let decoded = try JSONDecoder().decode(SwiftRequiredDefaults.self, from: json)

        // Required fields are written even though they hold their defaults and the default
        // strategy skips default values.
        let expectedJson = """
        {\
        "big":"-9000000000",\
        "color":"GREEN",\
        "count":5,\
        "flag":true,\
        "label":"a$b \\"c\\" \\\\d\\n",\
        "offset":-2.25,\
        "payload":"YWJj",\
        "ratio":1.5,\
        "secret":"hunter2",\
        "unsignedCount":4294967295\
        }
        """
        XCTAssertEqual(String(data: json, encoding: .utf8), expectedJson)
        XCTAssertEqual(decoded, message)
    }

    func testJsonRoundTripOfOtherValues() throws {
        let message = otherValued()

        let json = try JSONEncoder().encode(message)
        let decoded = try JSONDecoder().decode(SwiftRequiredDefaults.self, from: json)

        XCTAssertEqual(decoded, message)
    }

    func testJsonWithoutRequiredFieldIsStillAnError() throws {
        let json = Data("{}".utf8)

        XCTAssertThrowsError(try JSONDecoder().decode(SwiftRequiredDefaults.self, from: json))
    }

    func testEquality() throws {
        XCTAssertEqual(defaultValued(), defaultValued())
        XCTAssertEqual(Set([defaultValued(), defaultValued()]).count, 1)

        var changed = defaultValued()
        changed.count = 6
        XCTAssertNotEqual(changed, defaultValued())

        var changedOptional = defaultValued()
        changedOptional.optional_count = 7
        XCTAssertNotEqual(changedOptional, defaultValued())

        XCTAssertNotEqual(defaultValued(), otherValued())
    }

    func testDescriptionRedactsSecretAndShowsValues() throws {
        let description = defaultValued().description

        XCTAssertTrue(description.contains("count: 5"), description)
        XCTAssertTrue(description.contains("color: GREEN"), description)
        XCTAssertTrue(description.contains("secret: <redacted>"), description)
        XCTAssertFalse(description.contains("hunter2"), description)
    }

    // MARK: - Heap allocated message

    private func largeDefaultValued() -> SwiftRequiredDefaultsLarge {
        SwiftRequiredDefaultsLarge(
            f1: 1,
            f2: 2,
            f3: 3,
            f4: 4,
            f5: -5,
            f6: 6,
            f7: 7.5,
            f8: 8.5,
            f9: true,
            f10: "ten",
            f11: Data([1, 2]),
            f12: .BLUE,
            f13: -13,
            f14: "$",
            f15: "secret"
        )
    }

    func testLargeMessageBinaryAndJsonRoundTrips() throws {
        let message = largeDefaultValued()

        let data = try ProtoEncoder().encode(message)
        XCTAssertEqual(try ProtoDecoder().decode(SwiftRequiredDefaultsLarge.self, from: data), message)

        let json = try JSONEncoder().encode(message)
        XCTAssertEqual(try JSONDecoder().decode(SwiftRequiredDefaultsLarge.self, from: json), message)
    }

    func testLargeMessageEqualityAndCopyOnWrite() throws {
        let original = largeDefaultValued()
        var copy = original
        copy.f1 = 100

        XCTAssertEqual(original.f1, 1)
        XCTAssertEqual(copy.f1, 100)
        XCTAssertNotEqual(original, copy)
        XCTAssertEqual(original, largeDefaultValued())
        XCTAssertNil(original.f16)
        XCTAssertEqual(original.$f16, 16)
    }

    func testLargeMessageDescriptionRedactsSecret() throws {
        let description = largeDefaultValued().description

        XCTAssertTrue(description.contains("f1: 1"), description)
        XCTAssertTrue(description.contains("f15: <redacted>"), description)
        XCTAssertFalse(description.contains("secret"), description)
    }

    func testLargeMessageMissingRequiredFieldIsStillAnError() throws {
        XCTAssertThrowsError(try ProtoDecoder().decode(SwiftRequiredDefaultsLarge.self, from: Data())) { error in
            guard case let ProtoDecoder.Error.missingRequiredField(_, fieldName) = error else {
                return XCTFail("Unexpected error: \(error)")
            }
            XCTAssertEqual(fieldName, "f1")
        }
    }
}
