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

/// A proto2 required field is always written to JSON, even when it holds the zero value.
/// Decoding fails when a required key is missing, so Swift must be able to read its own JSON.
final class RequiredZeroValuesJsonTests: XCTestCase {
    private func zeroValued() -> AllTypes {
        AllTypes(
            req_int32: 0,
            req_uint32: 0,
            req_sint32: 0,
            req_fixed32: 0,
            req_sfixed32: 0,
            req_int64: 0,
            req_uint64: 0,
            req_sint64: 0,
            req_fixed64: 0,
            req_sfixed64: 0,
            req_bool: false,
            req_float: 0,
            req_double: 0,
            req_string: "",
            req_bytes: Data(),
            req_nested_enum: .A,
            req_nested_message: .init()
        )
    }

    func testZeroValuedRequiredFieldsRoundTripThroughJson() throws {
        let message = zeroValued()

        let json = try JSONEncoder().encode(message)
        let decoded = try JSONDecoder().decode(AllTypes.self, from: json)

        XCTAssertEqual(decoded, message)
    }

    func testZeroValuedRequiredFieldsAreWrittenInEveryEncodeMode() throws {
        let message = zeroValued()
        // JSON keys are camel case by default.
        let requiredKeys = [
            "reqInt32", "reqUint32", "reqSint32", "reqFixed32", "reqSfixed32",
            "reqInt64", "reqUint64", "reqSint64", "reqFixed64", "reqSfixed64",
            "reqBool", "reqFloat", "reqDouble", "reqString", "reqBytes",
            "reqNestedEnum", "reqNestedMessage",
        ]

        for includeDefaults in [false, true] {
            let encoder = JSONEncoder()
            encoder.protoDefaultValuesEncodingStrategy = includeDefaults ? .include : .skip
            let json = try encoder.encode(message)
            let object = try XCTUnwrap(JSONSerialization.jsonObject(with: json) as? [String: Any])

            for key in requiredKeys {
                XCTAssertNotNil(object[key], "\(key) is missing, includeDefaults: \(includeDefaults)")
            }
            XCTAssertNil(object["optInt32"], "includeDefaults: \(includeDefaults)")
            XCTAssertEqual(try JSONDecoder().decode(AllTypes.self, from: json), message)
        }
    }

    func testRequiredFieldsWithDefaultsSetToZeroValuesRoundTripThroughJson() throws {
        let message = SwiftRequiredDefaults(
            count: 0,
            label: "",
            payload: Data(),
            color: .RED,
            flag: false,
            ratio: 0,
            offset: 0,
            big: 0,
            unsigned_count: 0,
            secret: ""
        )

        let json = try JSONEncoder().encode(message)

        XCTAssertEqual(try JSONDecoder().decode(SwiftRequiredDefaults.self, from: json), message)
    }
}
