/*
 * Copyright (C) 2023 Square, Inc.
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

final class ProtoEnumCodableTests: XCTestCase {
    enum EnumType : Int32, CaseIterable, ProtoEnum, Proto2Enum {
        case DO_NOT_USE = 0
        case ONE = 1
        case TWO = 2

        var description: String {
            switch self {
            case .DO_NOT_USE:
                return "DO_NOT_USE"

            case .ONE:
                return "ONE"

            case .TWO:
                return "TWO"
            }
        }
    }
}

// MARK: - Non-optional

extension ProtoEnumCodableTests {
    struct SupportedTypes : Codable, Equatable {
        var a: EnumType
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
        encoder.protoEnumEncodingStrategy = .integer
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        let jsonData = try! encoder.encode(expectedStruct)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        let actualStruct = try! JSONDecoder().decode(SupportedTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testEncodingNegativeValueEnum() throws {
        let expectedStruct = NegativeValueMessage {
            $0.value = NegativeValueEnum.NEGATIVE_VALUE
        }

        let expectedJson = """
        {\
        "value":-1\
        }
        """

        let encoder = JSONEncoder()
        encoder.protoEnumEncodingStrategy = .integer
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        let jsonData = try! encoder.encode(expectedStruct)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        let actualStruct = try! JSONDecoder().decode(NegativeValueMessage.self, from: jsonData)
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

    func testDecodingNegativeValueEnum() throws {
        let expectedStruct = NegativeValueMessage {
            $0.value = NegativeValueEnum.NEGATIVE_VALUE
        }

        let json = """
        {\
        "value":-1\
        }
        """
        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()
        decoder.protoEnumDecodingStrategy = .returnNil

        let decoded = try decoder.decode(NegativeValueMessage.self, from: jsonData)
        XCTAssertEqual(decoded, expectedStruct)
    }
}

// MARK: - Optional

extension ProtoEnumCodableTests {
    struct OptionalTypes : Codable, Equatable {
        var a: EnumType?
        var b: EnumType?
    }

    func testEncodingNil() throws {
        let expectedStruct = OptionalTypes(
            a: nil,
            b: .TWO
        )
        let expectedJson = """
        {\
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

    func testEncodingIntegerNil() throws {
        let expectedStruct = OptionalTypes(
            a: nil,
            b: .TWO
        )
        let expectedJson = """
        {\
        "b":2\
        }
        """

        let encoder = JSONEncoder()
        encoder.protoEnumEncodingStrategy = .integer
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
        decoder.protoEnumDecodingStrategy = .returnNil

        let decoded = try decoder.decode(OptionalTypes.self, from: jsonData)
        XCTAssertEqual(decoded, expectedStruct)
    }
}

// MARK: - Array

extension ProtoEnumCodableTests {

    struct ArrayTypes : Codable, Equatable {
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

    func testEncodingNumericArray() throws {
        let expectedStruct = ArrayTypes(
            results: [.ONE, .TWO]
        )
        let expectedJson = """
        {\
        "results":[1,2]\
        }
        """

        let encoder = JSONEncoder()
        encoder.protoEnumEncodingStrategy = .integer
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        let jsonData = try! encoder.encode(expectedStruct)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        let raw = try! JSONDecoder().decode([String: [Int]].self, from: jsonData)
        print(raw)

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
            results: [.ONE, .TWO]
        )
        let json = """
        {\
        "results":["ONE","ZZZ","TWO"]\
        }
        """
        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()
        decoder.protoEnumDecodingStrategy = .returnNil

        let decoded = try decoder.decode(ArrayTypes.self, from: jsonData)
        XCTAssertEqual(decoded, expectedStruct)
    }
}

// MARK: - Maps

extension ProtoEnumCodableTests {
    struct DictionaryTypes : Codable, Equatable {
        @DefaultEmpty
        var standard: [String: EnumType]

        @DefaultEmpty
        @ProtoMap
        var protoMap: [String: EnumType]
    }

    func testDecodingUnknownDictionaryValue() throws {
        let json = """
        {\
        "standard":{"a":"ZZZ","b":"ONE"}\
        }
        """
        let jsonData = json.data(using: .utf8)!

        XCTAssertThrowsError(
            try JSONDecoder().decode(DictionaryTypes.self, from: jsonData)
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

    func testDecodingUnknownProtoMapValue() throws {
        let json = """
        {\
        "protoMap":{"a":"ZZZ","b":"ONE"}\
        }
        """
        let jsonData = json.data(using: .utf8)!

        XCTAssertThrowsError(
            try JSONDecoder().decode(DictionaryTypes.self, from: jsonData)
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

    func testLossyDecodingUnknownDictionaryValue() throws {
        let json = """
        {\
        "standard":{"a":"ZZZ","b":"ONE"}\
        }
        """
        let jsonData = json.data(using: .utf8)!

        // There's basically no good solution here
        let decoder = JSONDecoder()
        decoder.protoEnumDecodingStrategy = .returnNil

        XCTAssertThrowsError(
            try decoder.decode(DictionaryTypes.self, from: jsonData)
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

    func testLossyDecodingUnknownProtoMapValue() throws {
        let expectedStruct = DictionaryTypes(
            standard: [:],
            protoMap: ["b": .ONE]
        )

        let json = """
        {\
        "protoMap":{"a":"ZZZ","b":"ONE"}\
        }
        """
        let jsonData = json.data(using: .utf8)!

        let decoder = JSONDecoder()
        decoder.protoEnumDecodingStrategy = .returnNil

        let decoded = try decoder.decode(DictionaryTypes.self, from: jsonData)
        XCTAssertEqual(decoded, expectedStruct)
    }
}
