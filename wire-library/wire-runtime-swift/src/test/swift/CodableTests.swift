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
import Wire
import XCTest

final class CodableTests: XCTestCase {

    // MARK: - Decode Tests

    func testDecodeOptional() throws {
        let json = """
        {
          "opt_int64":"2",
          "repeated_int32":[1,2,3],
          "opt_uint64":"4",
          "opt_double":6,
          "map_int32_string":[
            1,"foo",
            2,"bar"
          ],
          "opt_bytes":"ASNF",
          "opt_uint32":3,
          "opt_enum":0,
          "repeated_string":["foo","bar","baz"],
          "opt_int32":1,
          "opt_float":5,
          "opt_string":"foo"
        }
        """

        let expected = SimpleOptional2(
            opt_int32: 1,
            opt_int64: 2,
            opt_uint32: 3,
            opt_uint64: 4,
            opt_float: 5,
            opt_double: 6,
            opt_bytes: Data(hexEncoded: "0123456"),
            opt_string: "foo",
            opt_enum: .UNKNOWN,
            repeated_int32: [1, 2, 3],
            repeated_string: ["foo", "bar", "baz"],
            map_int32_string: [1: "foo", 2: "bar"]
        )

        try assertDecode(json: json, expected: expected)
    }

    func testDecodeRequired() throws {
        let json = """
        {
          "req_int64":"2",
          "repeated_int32":[1,2,3],
          "req_uint64":"4",
          "req_double":6,
          "map_int32_string":[
            1,"foo",
            2,"bar"
          ],
          "req_bytes":"ASNF",
          "req_uint32":3,
          "req_enum":0,
          "repeated_string":["foo","bar","baz"],
          "req_int32":1,
          "req_float":5,
          "req_string":"foo"
        }
        """

        let expected = SimpleRequired2(
            req_int32: 1,
            req_int64: 2,
            req_uint32: 3,
            req_uint64: 4,
            req_float: 5,
            req_double: 6,
            req_bytes: Data(hexEncoded: "0123456")!,
            req_string: "foo",
            req_enum: .UNKNOWN,
            repeated_int32: [1, 2, 3],
            repeated_string: ["foo", "bar", "baz"],
            map_int32_string: [1: "foo", 2: "bar"]
        )

        try assertDecode(json: json, expected: expected)
    }

    // MARK: - Encode Tests

    func testEncodeOptional() throws {
        // Only include one value in maps until https://bugs.swift.org/browse/SR-13414 is fixed.
        let proto = SimpleOptional2(
            opt_int32: 1,
            opt_int64: 2,
            opt_uint32: 3,
            opt_uint64: 4,
            opt_float: 5,
            opt_double: 6,
            opt_bytes: Data(hexEncoded: "0123456"),
            opt_string: "foo",
            opt_enum: .UNKNOWN,
            repeated_int32: [1, 2, 3],
            repeated_string: ["foo", "bar", "baz"],
            map_int32_string: [1: "foo"]
        )

        let expected = """
        {
          "map_int32_string":[1,"foo"],
          "opt_bytes":"ASNF",
          "opt_double":6,
          "opt_enum":0,
          "opt_float":5,
          "opt_int32":1,
          "opt_int64":"2",
          "opt_string":"foo",
          "opt_uint32":3,
          "opt_uint64":"4",
          "repeated_int32":[1,2,3],
          "repeated_string":["foo","bar","baz"]
        }
        """

        try assertEncode(proto: proto, expected: expected)
    }

    func testEncodeRequired() throws {
        // Only include one value in maps until https://bugs.swift.org/browse/SR-13414 is fixed.
        let proto = SimpleRequired2(
            req_int32: 1,
            req_int64: 2,
            req_uint32: 3,
            req_uint64: 4,
            req_float: 5,
            req_double: 6,
            req_bytes: Data(hexEncoded: "0123456")!,
            req_string: "foo",
            req_enum: .UNKNOWN,
            repeated_int32: [1, 2, 3],
            repeated_string: ["foo", "bar", "baz"],
            map_int32_string: [1: "foo"]
        )

        let expected = """
        {
          "map_int32_string":[1,"foo"],
          "repeated_int32":[1,2,3],
          "repeated_string":["foo","bar","baz"],
          "req_bytes":"ASNF",
          "req_double":6,
          "req_enum":0,
          "req_float":5,
          "req_int32":1,
          "req_int64":"2",
          "req_string":"foo",
          "req_uint32":3,
          "req_uint64":"4"
        }
        """

        try assertEncode(proto: proto, expected: expected)
    }

    // MARK: - Private Methods

    private func assertDecode<P: Decodable & Equatable>(
        json: String,
        expected: P,
        file: StaticString = #file,
        line: UInt = #line
    ) throws {
        let json = json.compacted()
        let proto = try JSONDecoder().decode(P.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(proto, expected, file: file, line: line)
    }

    private func assertEncode<P: Encodable>(
        proto: P,
        expected: String,
        file: StaticString = #file,
        line: UInt = #line
    ) throws {
        let expected = expected.compacted()
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys

        let json = String(data: try encoder.encode(proto), encoding: .utf8)
        XCTAssertEqual(json, expected, file: file, line: line)
    }

}

// MARK: -

private extension String {

    func compacted() -> String {
        let lines = split(separator: "\n")
        return lines.map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }.joined(separator: "")
    }

}
