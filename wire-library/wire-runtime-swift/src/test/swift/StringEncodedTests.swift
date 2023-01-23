/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

final class StringEncodedTests: XCTestCase {
}

// MARK: - Round Trip happy path

extension StringEncodedTests {
    struct SupportedTypes : Codable, Equatable {
        @StringEncoded
        var a: Int64
        @StringEncoded
        var b: UInt64
        @StringEncodedValues
        var c: [Int64]
        @StringEncodedValues
        var d: [UInt64]
        @StringEncoded
        var e: Int64?
        @StringEncoded
        var f: Int64?
        @StringEncoded
        var g: UInt64?
        @StringEncoded
        var h: UInt64?
        @DefaultEmpty
        @StringEncodedValues
        var i: [Int64]
        @DefaultEmpty
        @StringEncodedValues
        var j: Set<UInt64>
        @StringEncodedValues
        var k: Set<Int64>
        @StringEncodedValues
        var l: [Int64?]
    }

    func testSupportedTypes() throws {
        let expectedStruct = SupportedTypes(
            a: -12,
            b: 13,
            c: [-14],
            d: [15],
            e: -16,
            f: nil,
            g: 17,
            h: nil,
            i: [],
            j: [],
            k: [1],
            l: [1, nil, 2]
        )

        let inputJson = """
        {\
        "a":"-12",\
        "b":"13",\
        "c":["-14"],\
        "d":["15"],\
        "e":"-16",\
        "g":"17",\
        "k":["1"],\
        "l":["1",null,"2"]\
        }
        """

        let expectedJson = """
        {\
        "a":"-12",\
        "b":"13",\
        "c":["-14"],\
        "d":["15"],\
        "e":"-16",\
        "f":null,\
        "g":"17",\
        "h":null,\
        "i":[],\
        "j":[],\
        "k":["1"],\
        "l":["1",null,"2"]\
        }
        """

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        let jsonData = inputJson.data(using: .utf8)!

        // Ensure we can decode our "dirty" JSON
        let actualStruct = try JSONDecoder().decode(SupportedTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)

        // Ensure we can encode our "clean" JSON
        let encodedStruct = try encoder.encode(expectedStruct)
        let actualJson = String(data: encodedStruct, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)
    }
}

// MARK: - Edge Cases and Failures

extension StringEncodedTests {
    struct SimpleStruct : Codable, Equatable {
        @StringEncoded
        var number: Int64?
        @DefaultEmpty
        @StringEncodedValues
        var array: [Int64]
    }

    func testEmptyInflates() throws {
        let json = "{}"
        let expectedStruct = SimpleStruct(number: nil, array: [])

        let jsonData = json.data(using: .utf8)!

        let actualStruct = try JSONDecoder().decode(SimpleStruct.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testNullInflates() throws {
        let json = """
        {"number":null}
        """
        let expectedStruct = SimpleStruct(number: nil, array: [])

        let jsonData = json.data(using: .utf8)!

        let actualStruct = try JSONDecoder().decode(SimpleStruct.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testInvalidSingleDataThrows() throws {
        let json = """
        {"number":"abc"}
        """
        let jsonData = json.data(using: .utf8)!

        XCTAssertThrowsError(
            try JSONDecoder().decode(SimpleStruct.self, from: jsonData)
        ) { error in
            switch error {
            case ProtoDecoder.Error.unparsableString(_, let value):
                XCTAssertEqual(value, "abc")

            default:
                XCTFail("Invalid error: \(error)")
            }
        }
    }

    func testInvalidSingleDataContentThrows() throws {
        // This will fail because the default is .disallowRawDecoding
        let json = """
        {"number":2}
        """
        let jsonData = json.data(using: .utf8)!

        XCTAssertThrowsError(
            try JSONDecoder().decode(SimpleStruct.self, from: jsonData)
        ) { error in
            switch error {
            case DecodingError.typeMismatch:
                break

            default:
                XCTFail("Invalid error: \(error)")
            }
        }
    }

    func testInvalidArrayDataThrows() throws {
        let json = """
        {\
        "number":"2",\
        "array":["abc"]}
        """
        let jsonData = json.data(using: .utf8)!

        XCTAssertThrowsError(
            try JSONDecoder().decode(SimpleStruct.self, from: jsonData)
        ) { error in
            switch error {
            case ProtoDecoder.Error.unparsableString(_, let value):
                XCTAssertEqual(value, "abc")

            default:
                XCTFail("Invalid error: \(error)")
            }
        }
    }

    func testNullArrayDataThrows() throws {
        let json = """
        {"array":[null]}
        """
        let jsonData = json.data(using: .utf8)!

        XCTAssertThrowsError(
            try JSONDecoder().decode(SimpleStruct.self, from: jsonData)
        ) { error in
            switch error {
            case ProtoDecoder.Error.unparsableString(_, let value):
                XCTAssertNil(value)

            default:
                XCTFail("Invalid error: \(error)")
            }
        }
    }

    func testRawEncodingRoundTrip() throws {
        let json = """
        {\
        "number":2,\
        "array":[1,3]\
        }
        """
        let expectedStruct = SimpleStruct(number: 2, array: [1,3])

        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()
        decoder.stringEncodedDecodingStrategy = .allowRawDecoding

        let actualStruct = try decoder.decode(SimpleStruct.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)

        let encoder = JSONEncoder()
        encoder.stringEncodedEncodingStrategy = .raw

        let actualJSONData = try encoder.encode(actualStruct)
        let actualJSON = String(data: actualJSONData, encoding: .utf8)!
        XCTAssertEqual(actualJSON, json)
    }
}
