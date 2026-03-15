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

final class StructTests: XCTestCase {

    // MARK: - StructValue Proto Encoding

    func testEncodeDecodeNullValue() throws {
        let value = StructValue.nullValue
        let data = try ProtoEncoder().encode(value)
        let decoded = try ProtoDecoder().decode(StructValue.self, from: data)
        XCTAssertEqual(decoded, value)
    }

    func testEncodeDecodeNumberValue() throws {
        let value = StructValue.numberValue(42.5)
        let data = try ProtoEncoder().encode(value)
        let decoded = try ProtoDecoder().decode(StructValue.self, from: data)
        XCTAssertEqual(decoded, value)
    }

    func testEncodeDecodeStringValue() throws {
        let value = StructValue.stringValue("hello")
        let data = try ProtoEncoder().encode(value)
        let decoded = try ProtoDecoder().decode(StructValue.self, from: data)
        XCTAssertEqual(decoded, value)
    }

    func testEncodeDecodeUtf8StringValue() throws {
        let value = StructValue.stringValue("\u{041F}\u{0440}\u{0438}\u{0432}\u{0435}\u{0442}")
        let data = try ProtoEncoder().encode(value)
        let decoded = try ProtoDecoder().decode(StructValue.self, from: data)
        XCTAssertEqual(decoded, value)
    }

    func testEncodeDecodeSpecialDoubleValues() throws {
        let list = ListValue(values: [
            .numberValue(-.infinity),
            .numberValue(-0.0),
            .numberValue(0.0),
            .numberValue(.infinity),
            .numberValue(.nan),
        ])
        let data = try ProtoEncoder().encode(list)
        let decoded = try ProtoDecoder().decode(ListValue.self, from: data)
        XCTAssertEqual(decoded.values.count, 5)
        XCTAssertEqual(decoded.values[0], .numberValue(-.infinity))
        XCTAssertEqual(decoded.values[1], .numberValue(-0.0))
        XCTAssertEqual(decoded.values[2], .numberValue(0.0))
        XCTAssertEqual(decoded.values[3], .numberValue(.infinity))
        // NaN != NaN, so assert on the payload directly.
        guard case .numberValue(let nan) = decoded.values[4] else {
            return XCTFail("Expected numberValue, got \(decoded.values[4])")
        }
        XCTAssertTrue(nan.isNaN)
    }

    func testEncodeDecodeBoolValue() throws {
        let value = StructValue.boolValue(true)
        let data = try ProtoEncoder().encode(value)
        let decoded = try ProtoDecoder().decode(StructValue.self, from: data)
        XCTAssertEqual(decoded, value)
    }

    func testEncodeDecodeBoolFalseValue() throws {
        let value = StructValue.boolValue(false)
        let data = try ProtoEncoder().encode(value)
        let decoded = try ProtoDecoder().decode(StructValue.self, from: data)
        XCTAssertEqual(decoded, value)
    }

    func testEncodeDecodeNumberZeroValue() throws {
        let value = StructValue.numberValue(0.0)
        let data = try ProtoEncoder().encode(value)
        let decoded = try ProtoDecoder().decode(StructValue.self, from: data)
        XCTAssertEqual(decoded, value)
    }

    func testEncodeDecodeEmptyStringValue() throws {
        let value = StructValue.stringValue("")
        let data = try ProtoEncoder().encode(value)
        let decoded = try ProtoDecoder().decode(StructValue.self, from: data)
        XCTAssertEqual(decoded, value)
    }

    func testDecodeEmptyValueMessageAsNull() throws {
        // An empty Value message (no oneof field set) should decode as nullValue.
        let decoded = try ProtoDecoder().decode(StructValue.self, from: Foundation.Data())
        XCTAssertEqual(decoded, .nullValue)
    }

    // MARK: - StructMessage Proto Encoding

    func testEncodeDecodeStructWithFields() throws {
        let message = StructMessage(fields: [
            "name": .stringValue("test"),
            "count": .numberValue(42),
            "active": .boolValue(true),
            "nothing": .nullValue,
        ])
        let encoder = ProtoEncoder()
        encoder.outputFormatting = .sortedKeys
        let data = try encoder.encode(message)
        let decoded = try ProtoDecoder().decode(StructMessage.self, from: data)
        XCTAssertEqual(decoded, message)
    }

    // MARK: - ListValue Proto Encoding

    func testEncodeDecodeListWithValues() throws {
        let list = ListValue(values: [
            .numberValue(1),
            .stringValue("two"),
            .boolValue(true),
            .nullValue,
        ])
        let data = try ProtoEncoder().encode(list)
        let decoded = try ProtoDecoder().decode(ListValue.self, from: data)
        XCTAssertEqual(decoded, list)
    }

    // MARK: - Default & Empty Value Proto Encoding

    func testEncodeDecodeStructWithDefaultValues() throws {
        let message = StructMessage(fields: [
            "empty": .stringValue(""),
            "falsy": .boolValue(false),
            "zero": .numberValue(0),
        ])
        let encoder = ProtoEncoder()
        encoder.outputFormatting = .sortedKeys
        let data = try encoder.encode(message)
        let decoded = try ProtoDecoder().decode(StructMessage.self, from: data)
        XCTAssertEqual(decoded, message)
    }

    func testEncodeDecodeStructValueWithEmptyStruct() throws {
        let value = StructValue.structValue(StructMessage())
        let data = try ProtoEncoder().encode(value)
        let decoded = try ProtoDecoder().decode(StructValue.self, from: data)
        XCTAssertEqual(decoded, value)
    }

    func testEncodeDecodeStructValueWithEmptyList() throws {
        let value = StructValue.listValue(ListValue())
        let data = try ProtoEncoder().encode(value)
        let decoded = try ProtoDecoder().decode(StructValue.self, from: data)
        XCTAssertEqual(decoded, value)
    }

    // MARK: - Nested Struct/Value/List Proto Encoding

    func testEncodeDecodeNestedStruct() throws {
        let inner = StructMessage(fields: [
            "key": .stringValue("value"),
        ])
        let outer = StructMessage(fields: [
            "nested": .structValue(inner),
            "list": .listValue(ListValue(values: [.numberValue(1), .numberValue(2)])),
        ])
        let encoder = ProtoEncoder()
        encoder.outputFormatting = .sortedKeys
        let data = try encoder.encode(outer)
        let decoded = try ProtoDecoder().decode(StructMessage.self, from: data)
        XCTAssertEqual(decoded, outer)
    }

    // MARK: - JSON (Codable) Encoding

    func testStructMessageJSONRoundTrip() throws {
        let message = StructMessage(fields: [
            "name": .stringValue("test"),
            "count": .numberValue(42),
            "active": .boolValue(true),
            "nothing": .nullValue,
        ])

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        let jsonData = try encoder.encode(message)
        let decoded = try JSONDecoder().decode(StructMessage.self, from: jsonData)
        XCTAssertEqual(decoded, message)
    }

    func testStructMessageJSONFormat() throws {
        let message = StructMessage(fields: [
            "key": .stringValue("value"),
        ])
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        let jsonData = try encoder.encode(message)
        let jsonString = String(data: jsonData, encoding: .utf8)
        XCTAssertEqual(jsonString, #"{"key":"value"}"#)
    }

    func testListValueJSONFormat() throws {
        let list = ListValue(values: [
            .numberValue(1),
            .stringValue("two"),
            .boolValue(true),
            .nullValue,
        ])
        let encoder = JSONEncoder()
        let jsonData = try encoder.encode(list)
        let jsonString = String(data: jsonData, encoding: .utf8)
        XCTAssertEqual(jsonString, #"[1,"two",true,null]"#)
    }

    func testStructValueJSONNull() throws {
        let value = StructValue.nullValue
        let jsonData = try JSONEncoder().encode(value)
        let jsonString = String(data: jsonData, encoding: .utf8)
        XCTAssertEqual(jsonString, "null")
    }

    func testStructValueJSONBool() throws {
        let value = StructValue.boolValue(false)
        let jsonData = try JSONEncoder().encode(value)
        let jsonString = String(data: jsonData, encoding: .utf8)
        XCTAssertEqual(jsonString, "false")
        let decoded = try JSONDecoder().decode(StructValue.self, from: jsonData)
        XCTAssertEqual(decoded, value)
    }

    func testStructValueJSONIntegerDecodesAsNumber() throws {
        // JSON integer 1 must decode as numberValue, not boolValue.
        let jsonData = "1".data(using: .utf8)!
        let decoded = try JSONDecoder().decode(StructValue.self, from: jsonData)
        XCTAssertEqual(decoded, .numberValue(1))
    }

    func testStructValueJSONObjectDecodesAsStruct() throws {
        let jsonData = #"{"a":1}"#.data(using: .utf8)!
        let decoded = try JSONDecoder().decode(StructValue.self, from: jsonData)
        XCTAssertEqual(decoded, .structValue(StructMessage(fields: ["a": .numberValue(1)])))
    }

    func testStructValueJSONArrayDecodesAsList() throws {
        let jsonData = "[1,2]".data(using: .utf8)!
        let decoded = try JSONDecoder().decode(StructValue.self, from: jsonData)
        XCTAssertEqual(decoded, .listValue(ListValue(values: [.numberValue(1), .numberValue(2)])))
    }

    func testStructNullJSONRoundTrip() throws {
        let value = StructNull.NULL_VALUE
        let jsonData = try JSONEncoder().encode(value)
        let jsonString = String(data: jsonData, encoding: .utf8)
        XCTAssertEqual(jsonString, "null")
        let decoded = try JSONDecoder().decode(StructNull.self, from: jsonData)
        XCTAssertEqual(decoded, .NULL_VALUE)
    }

    func testNestedStructJSONRoundTrip() throws {
        let message = StructMessage(fields: [
            "nested": .structValue(StructMessage(fields: [
                "inner": .numberValue(99),
            ])),
            "list": .listValue(ListValue(values: [
                .stringValue("a"),
                .stringValue("b"),
            ])),
        ])
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        let jsonData = try encoder.encode(message)
        let decoded = try JSONDecoder().decode(StructMessage.self, from: jsonData)
        XCTAssertEqual(decoded, message)
    }

    func testStructNullJSONRejectsNonNull() throws {
        let jsonData = "42".data(using: .utf8)!
        XCTAssertThrowsError(try JSONDecoder().decode(StructNull.self, from: jsonData))
    }

    // MARK: - Generated Code (MessageContainingStruct)

    func testGeneratedMessageContainingStructRoundTrip() throws {
        var message = MessageContainingStruct(some_null: .NULL_VALUE)
        message.some_struct = StructMessage(fields: ["key": .stringValue("value")])
        message.some_value = .numberValue(42)
        message.some_list = ListValue(values: [.boolValue(true), .nullValue])

        let data = try ProtoEncoder().encode(message)
        let decoded = try ProtoDecoder().decode(MessageContainingStruct.self, from: data)
        XCTAssertEqual(decoded, message)
    }

    func testGeneratedMessageContainingStructNilFieldsRoundTrip() throws {
        let message = MessageContainingStruct(some_null: .NULL_VALUE)
        let data = try ProtoEncoder().encode(message)
        let decoded = try ProtoDecoder().decode(MessageContainingStruct.self, from: data)
        XCTAssertEqual(decoded, message)
        XCTAssertNil(decoded.some_struct)
        XCTAssertNil(decoded.some_value)
        XCTAssertNil(decoded.some_list)
    }

}
