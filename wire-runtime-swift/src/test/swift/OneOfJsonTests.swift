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

/// JSON decoding accepts at most one non-null member of a oneof, like the protobuf runtimes from Google.
final class OneOfJsonTests: XCTestCase {
    private func decode<T : Decodable>(_ type: T.Type, _ json: String) throws -> T {
        return try JSONDecoder().decode(type, from: json.data(using: .utf8)!)
    }

    private func assertRejected<T : Decodable>(
        _ type: T.Type,
        _ json: String,
        keys: [String],
        codingPath: [String] = [],
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        do {
            let decoded = try decode(type, json)
            XCTFail("Expected an error but decoded \(decoded)", file: file, line: line)
        } catch DecodingError.dataCorrupted(let context) {
            XCTAssertEqual(context.codingPath.map { $0.stringValue }, codingPath, file: file, line: line)
            XCTAssertTrue(context.debugDescription.contains("'choice'"), context.debugDescription, file: file, line: line)
            for key in keys {
                XCTAssertTrue(context.debugDescription.contains("'\(key)'"), context.debugDescription, file: file, line: line)
            }
        } catch {
            XCTFail("Unexpected error \(error)", file: file, line: line)
        }
    }

    // MARK: Two non-null members

    func testRejectsTwoScalarMembersInDeclarationOrder() {
        assertRejected(
            OneOfs.self,
            #"{"stringOption":"a","enumOption":"A"}"#,
            keys: ["stringOption", "enumOption"],
            codingPath: ["stringOption"]
        )
    }

    func testRejectsTwoScalarMembersInReverseOrder() {
        assertRejected(
            OneOfs.self,
            #"{"enumOption":"A","stringOption":"a"}"#,
            keys: ["stringOption", "enumOption"],
            codingPath: ["stringOption"]
        )
    }

    func testRejectsMessageAndScalarMembersInEitherOrder() {
        assertRejected(
            OneOfs.self,
            #"{"stringOption":"a","messageOption":{"id":1}}"#,
            keys: ["stringOption", "messageOption"],
            codingPath: ["messageOption"]
        )
        assertRejected(
            OneOfs.self,
            #"{"messageOption":{"id":1},"stringOption":"a"}"#,
            keys: ["stringOption", "messageOption"],
            codingPath: ["messageOption"]
        )
    }

    func testRejectsMembersNamedWithProtoNames() {
        assertRejected(
            OneOfs.self,
            #"{"string_option":"a","message_option":{"id":1}}"#,
            keys: ["string_option", "message_option"],
            codingPath: ["message_option"]
        )
    }

    func testRejectsMembersNamedWithMixedKeys() {
        assertRejected(
            OneOfs.self,
            #"{"string_option":"a","messageOption":{"id":1}}"#,
            keys: ["string_option", "messageOption"],
            codingPath: ["messageOption"]
        )
    }

    func testRejectsTwoMembersInNestedMessage() {
        assertRejected(
            OneOfsHolder.self,
            #"{"oneOfs":{"stringOption":"a","enumOption":"A"}}"#,
            keys: ["stringOption", "enumOption"],
            codingPath: ["oneOfs", "stringOption"]
        )
    }

    func testRejectsTwoMembersOfLargeOneOf() throws {
        let json = #"{"value1":"a","value2":"b"}"#
        do {
            _ = try decode(RedactedLargeMessage.RedactedLargeOneOf.self, json)
            XCTFail("Expected an error")
        } catch DecodingError.dataCorrupted(let context) {
            XCTAssertTrue(context.debugDescription.contains("'action'"), context.debugDescription)
        }
    }

    // MARK: Null members

    func testNullMemberBeforeValueKeepsValue() throws {
        let decoded = try decode(OneOfs.self, #"{"enumOption":null,"stringOption":"a"}"#)
        XCTAssertEqual(decoded.choice, .string_option("a"))
    }

    func testNullMemberAfterValueKeepsValue() throws {
        let decoded = try decode(OneOfs.self, #"{"stringOption":"a","enumOption":null}"#)
        XCTAssertEqual(decoded.choice, .string_option("a"))
    }

    func testNullMessageMemberNextToScalarKeepsScalar() throws {
        let first = try decode(OneOfs.self, #"{"messageOption":null,"stringOption":"a"}"#)
        XCTAssertEqual(first.choice, .string_option("a"))
        let second = try decode(OneOfs.self, #"{"stringOption":"a","messageOption":null}"#)
        XCTAssertEqual(second.choice, .string_option("a"))
    }

    func testNullMemberNextToMessageKeepsMessage() throws {
        let decoded = try decode(OneOfs.self, #"{"stringOption":null,"messageOption":{"id":7}}"#)
        XCTAssertEqual(decoded.choice, .message_option(OneOfs.NestedMessage { $0.id = 7 }))
    }

    func testAllNullMembersDecodeToNoMember() throws {
        let decoded = try decode(OneOfs.self, #"{"stringOption":null,"enumOption":null,"messageOption":null}"#)
        XCTAssertNil(decoded.choice)
    }

    func testNullMemberNextToValueInNestedMessage() throws {
        let decoded = try decode(OneOfsHolder.self, #"{"oneOfs":{"stringOption":null,"enumOption":"A"}}"#)
        XCTAssertEqual(decoded.one_ofs?.choice, .enum_option(.A))
    }

    // MARK: One member, no member, and the two keys of one member

    func testOneMemberDecodes() throws {
        XCTAssertEqual(try decode(OneOfs.self, #"{"stringOption":"a"}"#).choice, .string_option("a"))
        XCTAssertEqual(try decode(OneOfs.self, #"{"string_option":"a"}"#).choice, .string_option("a"))
        XCTAssertEqual(try decode(OneOfs.self, #"{"enumOption":"A"}"#).choice, .enum_option(.A))
        XCTAssertEqual(
            try decode(OneOfs.self, #"{"messageOption":{"id":3}}"#).choice,
            .message_option(OneOfs.NestedMessage { $0.id = 3 })
        )
    }

    func testNoMemberDecodes() throws {
        XCTAssertNil(try decode(OneOfs.self, "{}").choice)
        XCTAssertNil(try decode(OneOfs.self, #"{"standaloneEnum":"A"}"#).choice)
    }

    func testMemberWithBothKeysKeepsTheCamelCaseKey() throws {
        let decoded = try decode(OneOfs.self, #"{"string_option":"snake","stringOption":"camel"}"#)
        XCTAssertEqual(decoded.choice, .string_option("camel"))
    }

    func testMemberWithNullCamelCaseKeyFallsBackToProtoKey() throws {
        let decoded = try decode(OneOfs.self, #"{"stringOption":null,"string_option":"snake"}"#)
        XCTAssertEqual(decoded.choice, .string_option("snake"))
    }

    func testMemberWithBothKeysAndAnotherMemberIsRejected() {
        assertRejected(
            OneOfs.self,
            #"{"string_option":"snake","stringOption":"camel","enumOption":"A"}"#,
            keys: ["stringOption", "enumOption"],
            codingPath: ["stringOption"]
        )
    }

    // MARK: Round trip

    func testRoundTripStillWorks() throws {
        let messages = [
            OneOfs { $0.choice = .string_option("a") },
            OneOfs { $0.choice = .enum_option(.A) },
            OneOfs { $0.choice = .message_option(OneOfs.NestedMessage { $0.id = 5 }) },
            OneOfs { $0.standalone_enum = .A },
        ]
        for message in messages {
            let data = try JSONEncoder().encode(message)
            XCTAssertEqual(try JSONDecoder().decode(OneOfs.self, from: data), message)
        }
        let holder = OneOfsHolder { $0.one_ofs = messages[2] }
        let data = try JSONEncoder().encode(holder)
        XCTAssertEqual(try JSONDecoder().decode(OneOfsHolder.self, from: data), holder)
    }

    func testRoundTripWithProtoKeyNames() throws {
        let message = OneOfs { $0.choice = .string_option("a") }
        let encoder = JSONEncoder()
        encoder.protoKeyNameEncodingStrategy = .fieldName
        let data = try encoder.encode(message)
        XCTAssertEqual(try JSONDecoder().decode(OneOfs.self, from: data), message)
    }

    // MARK: Members with names that Swift reads as something else after a type name

    func testMembersWithSpecialNames() throws {
        XCTAssertEqual(try decode(OneOfsSpecialNames.self, #"{"self":"a"}"#).choice, .`self`("a"))
        XCTAssertEqual(try decode(OneOfsSpecialNames.self, #"{"init":"a"}"#).choice, .`init`("a"))
        XCTAssertEqual(try decode(OneOfsSpecialNames.self, #"{"Type":"a"}"#).choice, .`Type`("a"))
        XCTAssertEqual(try decode(OneOfsSpecialNames.self, #"{"Protocol":"a","Type":null}"#).choice, .`Protocol`("a"))
        assertRejected(OneOfsSpecialNames.self, #"{"Type":"a","Protocol":"b"}"#, keys: ["Type", "Protocol"], codingPath: ["Protocol"])
    }
}
