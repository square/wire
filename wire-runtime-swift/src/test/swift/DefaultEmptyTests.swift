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

final class DefaultEmptyTests: XCTestCase {
    private struct CodableType : Equatable, Codable {
        @DefaultEmpty
        var emptyResults: [Int]
        var nonemptyResults: [Int]
    }
}

extension DefaultEmptyTests {
    func testEncodingEmptyArrays() {
        let expectedStruct = CodableType(emptyResults: [], nonemptyResults: [])
        let expectedJson = """
        {\
        "emptyResults":[],\
        "nonemptyResults":[]\
        }
        """

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        // Encode our struct
        let jsonData = try! encoder.encode(expectedStruct)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        // Verify decoding round trip results
        let actualStruct = try! JSONDecoder().decode(CodableType.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testEncodingValues() {
        let expectedStruct = CodableType(emptyResults: [1, 2], nonemptyResults: [1, 2])
        let expectedJson = """
        {\
        "emptyResults":[1,2],\
        "nonemptyResults":[1,2]\
        }
        """

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        // Encode our struct
        let jsonData = try! encoder.encode(expectedStruct)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        // Verify decoding round trip results
        let actualStruct = try! JSONDecoder().decode(CodableType.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testDecodingMissingArrays() {
        let expectedStruct = CodableType(emptyResults: [], nonemptyResults: [])

        let json = """
        {\
        "nonemptyResults":[]\
        }
        """

        let jsonData = json.data(using: .utf8)!
        let actualStruct = try! JSONDecoder().decode(CodableType.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }

    func testDecodingMissingArrayFails() {
        let json = "{}"

        let jsonData = json.data(using: .utf8)!
        XCTAssertThrowsError(
            try JSONDecoder().decode(CodableType.self, from: jsonData)
        ) { error in
            guard let error = error as? DecodingError else {
                XCTFail("Invalid error type for \(error)")
                return
            }

            guard case let .keyNotFound(codingKey, _) = error else {
                XCTFail("Invalid error case for \(error)")
                return
            }

            XCTAssertEqual(codingKey.stringValue, "nonemptyResults")
        }
    }
}
