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

/// Covers `json_name` values that hold `$` and `%`.
final class JsonNamesTests : XCTestCase {
    private func message() -> SwiftJsonNames {
        SwiftJsonNames(required_name: "r") {
            $0.dollar = "d"
            $0.template = "t"
            $0.percent = "p"
            $0.directive = "x"
            $0.choice = .oneof_dollar("o")
        }
    }

    func testJsonKeysAreTheDeclaredJsonNames() throws {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys

        let json = try encoder.encode(message())

        let expected = """
        {\
        "$one":"o",\
        "$required":"r",\
        "${x}":"t",\
        "%s %S %1S":"x",\
        "100%":"p",\
        "a$b":"d"\
        }
        """
        XCTAssertEqual(String(data: json, encoding: .utf8), expected)
        XCTAssertEqual(try JSONDecoder().decode(SwiftJsonNames.self, from: json), message())
    }

    func testOneofWithPercentInJsonName() throws {
        var original = message()
        original.choice = .oneof_percent(7)
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys

        let json = try encoder.encode(original)

        XCTAssertTrue(String(data: json, encoding: .utf8)!.contains("\"%one\":7"))
        XCTAssertEqual(try JSONDecoder().decode(SwiftJsonNames.self, from: json), original)
    }

    func testFieldNamesAreUsedWhenKeysAreNotCamelCased() throws {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        encoder.protoKeyNameEncodingStrategy = .fieldName

        let json = try encoder.encode(message())

        let expected = """
        {\
        "directive":"x",\
        "dollar":"d",\
        "oneof_dollar":"o",\
        "percent":"p",\
        "required_name":"r",\
        "template":"t"\
        }
        """
        XCTAssertEqual(String(data: json, encoding: .utf8), expected)
        XCTAssertEqual(try JSONDecoder().decode(SwiftJsonNames.self, from: json), message())
    }

    func testBinaryRoundTrip() throws {
        let data = try ProtoEncoder().encode(message())

        XCTAssertEqual(try ProtoDecoder().decode(SwiftJsonNames.self, from: data), message())
    }
}
