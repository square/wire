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

    struct SupportedTypes : Codable, Equatable {
        @JSONEnum
        var a: EnumType

        @JSONEnum
        var b: EnumType
    }

    func testEncoding() throws {
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
}
