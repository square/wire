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
import Wire
import XCTest

final class CodableTests: XCTestCase {
}

// MARK: - Decode Tests

extension CodableTests {
    func testDecodeOptional() throws {
        let json = """
        {
          "opt_int64":"2",
          "repeated_int32":[1,2,3],
          "opt_uint64":"4",
          "opt_double":6,
          "map_int32_string":{
            "1":"foo",
            "2":"bar"
          },
          "opt_bytes":"ASNF",
          "opt_uint32":3,
          "opt_enum":0,
          "repeated_string":["foo","bar","baz"],
          "opt_int32":1,
          "opt_float":5,
          "opt_string":"foo"
        }
        """

        let expected = SimpleOptional2 {
            $0.opt_int32 = 1
            $0.opt_int64 = 2
            $0.opt_uint32 = 3
            $0.opt_uint64 = 4
            $0.opt_float = 5
            $0.opt_double = 6
            $0.opt_bytes = Foundation.Data(hexEncoded: "0123456")
            $0.opt_string = "foo"
            $0.opt_enum = .UNKNOWN
            $0.repeated_int32 = [1, 2, 3]
            $0.repeated_string = ["foo", "bar", "baz"]
            $0.map_int32_string = [1: "foo", 2: "bar"]
        }

        try assertDecode(json: json, expected: expected)
    }

    func testDecodeRequired() throws {
        let json = """
        {
          "req_int64":"2",
          "repeated_int32":[1,2,3],
          "req_uint64":"4",
          "req_double":6,
          "map_int32_string":{
            "1":"foo",
            "2":"bar"
          },
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
            req_bytes: Foundation.Data(hexEncoded: "0123456")!,
            req_string: "foo",
            req_enum: .UNKNOWN
        ) {
            $0.repeated_int32 = [1, 2, 3]
            $0.repeated_string = ["foo", "bar", "baz"]
            $0.map_int32_string = [1: "foo", 2: "bar"]
        }

        try assertDecode(json: json, expected: expected)
    }
}

// MARK: - Encode Tests

extension CodableTests {
    func testEncodeOptional() throws {
        // Only include one value in maps until https://bugs.swift.org/browse/SR-13414 is fixed.
        let proto = SimpleOptional2 {
            $0.opt_int32 = 1
            $0.opt_int64 = 2
            $0.opt_uint32 = 3
            $0.opt_uint64 = 4
            $0.opt_float = 5
            $0.opt_double = 6
            $0.opt_bytes = Foundation.Data(hexEncoded: "0123456")
            $0.opt_string = "foo"
            $0.opt_enum = .UNKNOWN
            $0.repeated_int32 = [1, 2, 3]
            $0.repeated_string = ["foo", "bar", "baz"]
            $0.map_int32_string = [1: "foo"]
        }

        let expected = """
        {
          "map_int32_string":{"1":"foo"},
          "opt_bytes":"ASNF",
          "opt_double":6,
          "opt_enum":"UNKNOWN",
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

        try assertEncode(proto: proto, expected: expected) { encoder in
            encoder.protoKeyNameEncodingStrategy = .fieldName
        }
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
            req_bytes: Foundation.Data(hexEncoded: "0123456")!,
            req_string: "foo",
            req_enum: .UNKNOWN
        ) {
            $0.repeated_int32 = [1, 2, 3]
            $0.repeated_string = ["foo", "bar", "baz"]
            $0.map_int32_string = [1: "foo"]
        }

        let expected = """
        {
          "map_int32_string":{"1":"foo"},
          "repeated_int32":[1,2,3],
          "repeated_string":["foo","bar","baz"],
          "req_bytes":"ASNF",
          "req_double":6,
          "req_float":5,
          "req_int32":1,
          "req_int64":"2",
          "req_string":"foo",
          "req_uint32":3,
          "req_uint64":"4"
        }
        """

        try assertEncode(proto: proto, expected: expected) { encoder in
            encoder.protoKeyNameEncodingStrategy = .fieldName
        }
    }
}

// MARK: - Empty Payloads

extension CodableTests {
    func testDecodesEmptyProto() throws {
        try assertDecode(json: "{}", expected: SimpleOptional2())
    }

    func testEncodesEmptyProto() throws {
        try assertEncode(proto: SimpleOptional2(), expected: "{}")
    }

    func testEncodesEmptyProtoWithDefaults() throws {
        let json = """
        {
          "map_int32_string":{},
          "repeated_int32":[],
          "repeated_string":[]
        }
        """

        try assertEncode(proto: SimpleOptional2(), expected: json) { encoder in
            encoder.protoKeyNameEncodingStrategy = .fieldName
            encoder.protoDefaultValuesEncodingStrategy = .include
        }
    }

    func testDecodesDefaultValues() throws {
        let json = """
        {
          "map_int32_string":{},
          "opt_bytes":null,
          "opt_double":null,
          "opt_enum":null,
          "opt_float":null,
          "opt_int32":null,
          "opt_int64":null,
          "opt_string":null,
          "opt_uint32":null,
          "opt_uint64":null,
          "repeated_int32":[],
          "repeated_string":[]
        }
        """

        try assertDecode(json: json, expected: SimpleOptional2())
    }
}

// MARK: - Key Names

extension CodableTests {
    func testEncodedKeyNamesDefaultToCamelCase() throws {
        let json = """
        {
          "mapInt32String":{"1":"foo"},
          "optDouble":6,
          "optEnum":"A",
          "repeatedString":["B"]
        }
        """

        let proto = SimpleOptional2 {
            $0.opt_double = 6
            $0.opt_enum = .A
            $0.repeated_string = ["B"]
            $0.map_int32_string = [1 : "foo"]
        }

        try assertEncode(proto: proto, expected: json)
    }

    func testDecodePrefersCamelCase() throws {
        let json = """
        {
          "mapInt32String":{"1":"foo"},
          "map_int32_string":{"2":"bar"},
          "optDouble":6,
          "opt_double":8,
          "optEnum":"A",
          "opt_enum":"UNKNOWN",
          "repeatedString":["B"],
          "repeated_string":["C"],
          "opt_int64":"5"
        }
        """

        let proto = SimpleOptional2 {
            $0.opt_int64 = 5
            $0.opt_double = 6
            $0.opt_enum = .A
            $0.repeated_string = ["B"]
            $0.map_int32_string = [1 : "foo"]
        }

        try assertDecode(json: json, expected: proto)
    }
}

// MARK: - Heap Types

extension CodableTests {
    func testHeapRoundtrip() throws {
        let proto = SwiftStackOverflow {
            $0.value3 = "hello"
        }

        let json = """
        {
          "value3":"hello"
        }
        """

        try assertEncode(proto: proto, expected: json)
        try assertDecode(json: json, expected: proto)
    }
}

// MARK: - Duration and Timestamp

extension CodableTests {
    struct DurationAndTimestamp: Equatable, Codable {
        var duration: Wire.Duration?
        var timestamp: Wire.Timestamp?
    }

    func testDurationRoundtrip() throws {
        let json = """
        {
          "duration":"10s",
          "timestamp":"2023-01-25T00:02:33Z"
        }
        """

        let proto = DurationAndTimestamp(
            duration: Duration(seconds: 10, nanos: 0),
            timestamp: Timestamp(seconds: 1674604953, nanos: 0)
        )

        try assertDecode(json: json, expected: proto)
        try assertEncode(proto: proto, expected: json)
    }

    func testNegativeSmallDurationRoundtrip() throws {
        // Durations less than one second are represented with a 0 `seconds` field and a positive or negative `nanos` field

        let json = """
        {"duration":"-0.900s"}
        """

        let proto = DurationAndTimestamp(
            duration: Wire.Duration(seconds: 0, nanos: -900_000_000)
        )

        try assertDecode(json: json, expected: proto)
        try assertEncode(proto: proto, expected: json)
    }

    func testNegativeDurationRoundtrip() throws {
        // For Durations of one second or more, a non-zero value for the `nanos` field must be of the same sign as the `seconds` field

        let json = """
        {"duration":"-1.900s"}
        """

        let proto = DurationAndTimestamp(
            duration: Wire.Duration(seconds: -1, nanos: -900_000_000)
        )

        try assertDecode(json: json, expected: proto)
        try assertEncode(proto: proto, expected: json)
    }

    func testRedactedLargeMessageRoundTrip() throws {
        let json = """
        {"description":"foo"}
        """

        let proto = RedactedLargeMessage {
            $0.description_ = "foo"
        }

        try assertDecode(json: json, expected: proto)
        try assertEncode(proto: proto, expected: json)
    }

    func testDurationConversion() {
        guard #available(macOS 13, iOS 16, watchOS 9, tvOS 16, *) else {
            return
        }

        XCTAssertEqual(Wire.Duration(seconds: 0, nanos: -900).toSwiftDuration(), .nanoseconds(-900))
        XCTAssertEqual(Wire.Duration(seconds: -1, nanos: -900).toSwiftDuration(), .seconds(-1) + .nanoseconds(-900))
    }

    func testLargeDurationRoundtrip() throws {
        let json = """
        {
          "duration":"18014398509481984s",
          "timestamp":"4001-01-01T00:00:00Z"
        }
        """
        let proto = DurationAndTimestamp(
            duration: Duration(seconds: 1 << 54, nanos: 0),
            timestamp: Timestamp(date: .distantFuture)
        )

        // Swift's Date has less precision than Timestamp, so ideally we'd support something further in the future than Date.distantFuture
        // but our current implementation makes use of Date, and so we eventually would start encountering rounding errors.
        try assertDecode(json: json, expected: proto)
        try assertEncode(proto: proto, expected: json)
    }

    func testSmallDurationRoundtrip() throws {
        let json = """
        {
          "duration":"0.100s",
          "timestamp":"0001-01-01T00:00:00Z"
        }
        """
        let proto = DurationAndTimestamp(
            duration: Duration(seconds: 0, nanos: 100_000_000),
            timestamp: Timestamp(date: .distantPast)
        )

        try assertDecode(json: json, expected: proto)
        try assertEncode(proto: proto, expected: json)
    }

    func testVerySmallDurationRoundtrip() throws {
        let json = """
        {"duration":"1.000000010s"}
        """
        let proto = DurationAndTimestamp(
            duration: Duration(seconds: 1, nanos: 10)
        )

        try assertDecode(json: json, expected: proto)
        try assertEncode(proto: proto, expected: json)
    }
}

// MARK: - Private Methods

extension CodableTests {
    private func assertDecode<P: Decodable & Equatable>(
        json: String,
        expected: P,
        file: StaticString = #file,
        line: UInt = #line,
        configuration: (JSONDecoder) -> Void = { _ in }
    ) throws {
        let json = json.compacted()

        let decoder = JSONDecoder()
        configuration(decoder)

        let proto = try decoder.decode(P.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(proto, expected, file: file, line: line)
    }

    private func assertEncode<P: Encodable>(
        proto: P,
        expected: String,
        file: StaticString = #file,
        line: UInt = #line,
        configuration: (JSONEncoder) -> Void = { _ in }
    ) throws {
        let expected = expected.compacted()

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        configuration(encoder)

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
