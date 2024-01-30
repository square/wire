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

final class StringEncodedTests: XCTestCase {
}

// MARK: - Round Trip happy path

extension StringEncodedTests {
    struct SupportedTypes : Codable, Equatable {
        @StringEncoded
        var a: Int64
        @StringEncoded
        var b: UInt64
        @ProtoArray
        var c: [Int64]
        @ProtoArray
        var d: [UInt64]
    }

    func testSupportedTypes() throws {
        let expectedStruct = SupportedTypes(
            a: -12,
            b: 13,
            c: [-14, -2],
            d: [15, 2]
        )

        let inputJson = """
        {\
        "a":"-12",\
        "b":"13",\
        "c":["-14","-2"],\
        "d":["15","2"]\
        }
        """

        let expectedJson = """
        {\
        "a":"-12",\
        "b":"13",\
        "c":["-14","-2"],\
        "d":["15","2"]\
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

// MARK: - Data

extension StringEncodedTests {
    struct DataStruct : Codable, Equatable {
        @DefaultEmpty
        @StringEncoded
        var data: Foundation.Data
    }

    func testNullInflatesData() throws {
        let json = """
        {"data":null}
        """
        let expectedStruct = DataStruct(data: .init())

        let jsonData = json.data(using: .utf8)!

        let actualStruct = try JSONDecoder().decode(DataStruct.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testRoundtripEncodingData() throws {
        let string = "Hello World"

        let json = """
        {"data":"SGVsbG8gV29ybGQ="}
        """

        let expectedStruct = DataStruct(data: string.data(using: .utf8)!)

        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()

        let actualStruct = try decoder.decode(DataStruct.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)

        let encoder = JSONEncoder()

        let actualJSONData = try encoder.encode(actualStruct)
        let actualJSON = String(data: actualJSONData, encoding: .utf8)!
        XCTAssertEqual(actualJSON, json)

        let decodedString = String(data: actualStruct.data, encoding: .utf8)
        XCTAssertEqual(string, decodedString)
    }

    func testDecodingURLSafeData() throws {
        let data = Foundation.Data(base64Encoded: "ab+e/fg=")!

        let json = """
        {"data":"ab-e_fg"}
        """

        let expectedStruct = DataStruct(data: data)

        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()

        let actualStruct = try decoder.decode(DataStruct.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }
}

// MARK: - Edge Cases and Failures

extension StringEncodedTests {
    struct SimpleStruct : Codable, Equatable {
        @DefaultEmpty
        @StringEncoded
        var number: Int64
        @DefaultEmpty
        @ProtoArray
        var array: [Int64]
    }

    func testEmptyInflates() throws {
        let json = "{}"
        let expectedStruct = SimpleStruct(number: 0, array: [])

        let jsonData = json.data(using: .utf8)!

        let actualStruct = try JSONDecoder().decode(SimpleStruct.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testNullInflates() throws {
        let json = """
        {"number":null}
        """
        let expectedStruct = SimpleStruct(number: 0, array: [])

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
            case DecodingError.valueNotFound(let value, _):
                XCTAssert(value == Int64.self)

            default:
                XCTFail("Invalid error: \(error)")
            }
        }
    }

    func testRawDecodingRoundTrip() throws {
        let json = """
        {\
        "number":2,\
        "array":[1,3]\
        }
        """
        let expectedStruct = SimpleStruct(number: 2, array: [1,3])

        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()

        let actualStruct = try decoder.decode(SimpleStruct.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }
}

// MARK: - Dictionaries

extension StringEncodedTests {
    struct DictionaryStruct : Codable, Equatable {
        @DefaultEmpty
        @ProtoMap
        var keys: [Int64: String]

        @DefaultEmpty
        @ProtoMap
        var values: [String: Int64]

        @DefaultEmpty
        @ProtoMap
        var both: [Int64: Int64]
    }

    func testMapRoundTrip() throws {
        let json = """
        {\
        "both":{"3":"4"},\
        "keys":{"2":"c"},\
        "values":{"a":"1"}\
        }
        """
        let expectedStruct = DictionaryStruct(
            keys: [2 : "c"],
            values: ["a": 1],
            both: [3 : 4]
        )

        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()

        let actualStruct = try decoder.decode(DictionaryStruct.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys

        let actualJSONData = try encoder.encode(actualStruct)
        let actualJSON = String(data: actualJSONData, encoding: .utf8)!
        XCTAssertEqual(actualJSON, json)
    }

    func testMapRawDecoding() throws {
        let json = """
        {\
        "keys":{"2":"c"},\
        "values":{"a":1},\
        "both":{"3":4}\
        }
        """
        let expectedStruct = DictionaryStruct(
            keys: [2 : "c"],
            values: ["a": 1],
            both: [3 : 4]
        )

        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()

        let actualStruct = try decoder.decode(DictionaryStruct.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testBase64URLEncoding() throws {
        let base64 = "CiHSZFeg4VTJRJO5B9vXbTmlOqQSjwe9CAZG6jSj2RxDt/AVAQAAABgWIg1NTFZCQkU0TVo3WVE0"
        let expectedBase64URL = "CiHSZFeg4VTJRJO5B9vXbTmlOqQSjwe9CAZG6jSj2RxDt_AVAQAAABgWIg1NTFZCQkU0TVo3WVE0"

        XCTAssertEqual(expectedBase64URL, base64.base64URL())
    }

    func testBase64URLDecoding() throws {
        let base64URL = "CiHSZFeg4VTJRJO5B9vXbTmlOqQSjwe9CAZG6jSj2RxDt_AVAQAAABgWIg1NTFZCQkU0TVo3WVE0"
        let expectedBase64 = "CiHSZFeg4VTJRJO5B9vXbTmlOqQSjwe9CAZG6jSj2RxDt/AVAQAAABgWIg1NTFZCQkU0TVo3WVE0"

        XCTAssertEqual(expectedBase64, base64URL.base64())
    }
}
