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
