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

final class JsonEnumTests: XCTestCase {
    public enum EnumType : UInt32, CaseIterable, Codable {
        case ONE = 1
        case TWO = 2
    }
}

// MARK: - Non-optional

extension JsonEnumTests {
    struct SupportedTypes : Codable, Equatable {
        @JSONEnum
        var a: EnumType

        @JSONEnum
        var b: EnumType
    }

    func testEncodingString() throws {
        let expectedStruct = SupportedTypes(
            a: .ONE,
            b: .TWO
        )
        let expectedJson = """
        {\
        "a":"ONE",\
        "b":"TWO"\
        }
        """

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        let jsonData = try! encoder.encode(expectedStruct)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        let actualStruct = try! JSONDecoder().decode(SupportedTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testEncodingInteger() throws {
        let expectedStruct = SupportedTypes(
            a: .ONE,
            b: .TWO
        )
        let expectedJson = """
        {\
        "a":1,\
        "b":2\
        }
        """

        let encoder = JSONEncoder()
        encoder.userInfo[.wireEnumEncodingStrategy] = EnumEncodingStrategy.integer
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        let jsonData = try! encoder.encode(expectedStruct)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        let actualStruct = try! JSONDecoder().decode(SupportedTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testDecoding() throws {
        let expectedStruct = SupportedTypes(
            a: .ONE,
            b: .TWO
        )
        let json = """
        {\
        "a":"ONE",\
        "b":2\
        }
        """

        let jsonData = json.data(using: .utf8)!
        let actualStruct = try! JSONDecoder().decode(SupportedTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testDecodingUnknownString() throws {
        let json = """
        {\
        "a":"ZZZ",\
        "b":2\
        }
        """
        let jsonData = json.data(using: .utf8)!

        XCTAssertThrowsError(
            try JSONDecoder().decode(SupportedTypes.self, from: jsonData)
        ) { error in
            guard let error = error as? ProtoDecoder.Error else {
                XCTFail("Invalid error type for \(error)")
                return
            }

            guard case let .unknownEnumString(_, string) = error else {
                XCTFail("Invalid error case for \(error)")
                return
            }

            XCTAssertEqual(string, "ZZZ")
        }
    }

    func testDecodingUnknownFieldNumber() throws {
        let json = """
        {\
        "a":"ONE",\
        "b":7\
        }
        """
        let jsonData = json.data(using: .utf8)!

        XCTAssertThrowsError(
            try JSONDecoder().decode(SupportedTypes.self, from: jsonData)
        ) { error in
            guard let error = error as? ProtoDecoder.Error else {
                XCTFail("Invalid error type for \(error)")
                return
            }

            guard case let .unknownEnumCase(_, fieldNumber) = error else {
                XCTFail("Invalid error case for \(error)")
                return
            }

            XCTAssertEqual(fieldNumber, 7)
        }
    }
}


// MARK: - Optional

extension JsonEnumTests {
    struct OptionalTypes : Codable, Equatable {
        @JSONOptionalEnum
        var a: EnumType?

        @JSONOptionalEnum
        var b: EnumType?
    }

    func testEncodingNil() throws {
        let expectedStruct = OptionalTypes(
            a: nil,
            b: .TWO
        )
        let expectedJson = """
        {\
        "a":null,\
        "b":"TWO"\
        }
        """

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        let jsonData = try! encoder.encode(expectedStruct)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        let actualStruct = try! JSONDecoder().decode(OptionalTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testDecodingNil() throws {
        let expectedStruct = OptionalTypes(
            a: nil,
            b: .TWO
        )
        let json = """
        {\
        "b":2\
        }
        """

        let jsonData = json.data(using: .utf8)!
        let actualStruct = try! JSONDecoder().decode(OptionalTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testDecodingUnknownOptionalValue() throws {
        let json = """
        {\
        "a":"ONE",\
        "b":7\
        }
        """
        let jsonData = json.data(using: .utf8)!

        XCTAssertThrowsError(
            try JSONDecoder().decode(OptionalTypes.self, from: jsonData)
        ) { error in
            guard let error = error as? ProtoDecoder.Error else {
                XCTFail("Invalid error type for \(error)")
                return
            }

            guard case let .unknownEnumCase(_, fieldNumber) = error else {
                XCTFail("Invalid error case for \(error)")
                return
            }

            XCTAssertEqual(fieldNumber, 7)
        }
    }

    func testLossyDecodingUnknownOptionalValue() throws {
        let expectedStruct = OptionalTypes(
            a: .ONE,
            b: nil
        )
        
        let json = """
        {\
        "a":"ONE",\
        "b":7\
        }
        """
        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()
        decoder.userInfo[.wireEnumDecodingStrategy] = EnumDecodingStrategy.shouldSkip

        let decoded = try decoder.decode(OptionalTypes.self, from: jsonData)
        XCTAssertEqual(decoded, expectedStruct)
    }
}

// MARK: - Array

extension JsonEnumTests {

    struct ArrayTypes : Codable, Equatable {
        @JSONEnumArray
        var results: [EnumType]
    }

    func testEncodingArray() throws {
        let expectedStruct = ArrayTypes(
            results: [.ONE, .TWO]
        )
        let expectedJson = """
        {\
        "results":["ONE","TWO"]\
        }
        """

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        let jsonData = try! encoder.encode(expectedStruct)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        let actualStruct = try! JSONDecoder().decode(ArrayTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testDecodingArray() throws {
        let expectedStruct = ArrayTypes(
            results: [.ONE, .TWO]
        )
        let json = """
        {\
        "results":["ONE",2]\
        }
        """

        let jsonData = json.data(using: .utf8)!
        let actualStruct = try! JSONDecoder().decode(ArrayTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testDecodingNilArray() throws {
        let expectedStruct = ArrayTypes(
            results: []
        )
        let json = "{}"

        let jsonData = json.data(using: .utf8)!
        let actualStruct = try! JSONDecoder().decode(ArrayTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testDecodingUnknownArrayValue() throws {
        let json = """
        {\
        "results":["ONE","ZZZ"]\
        }
        """
        let jsonData = json.data(using: .utf8)!

        XCTAssertThrowsError(
            try JSONDecoder().decode(ArrayTypes.self, from: jsonData)
        ) { error in
            guard let error = error as? ProtoDecoder.Error else {
                XCTFail("Invalid error type for \(error)")
                return
            }

            guard case let .unknownEnumString(_, string) = error else {
                XCTFail("Invalid error case for \(error)")
                return
            }

            XCTAssertEqual(string, "ZZZ")
        }
    }

    func testLossyDecodingUnknownArrayValue() throws {
        let expectedStruct = ArrayTypes(
            results: [.ONE]
        )
        let json = """
        {\
        "results":["ONE","ZZZ"]\
        }
        """
        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()
        decoder.userInfo[.wireEnumDecodingStrategy] = EnumDecodingStrategy.shouldSkip

        let decoded = try decoder.decode(ArrayTypes.self, from: jsonData)
        XCTAssertEqual(decoded, expectedStruct)
    }
}
