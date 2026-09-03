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

final class RoundTripTests: XCTestCase {

    func testPersonEncodeDecode() throws {
        let personData = Data(json_data: "")
        let person = Person(name: "Luke Skywalker", id: 42, data: personData) {
            $0.email = "luke@skywalker.net"
            $0.phone = [
                Person.PhoneNumber(number: "800-555-1234") { $0.type = .WORK },
            ]
            $0.aliases = ["Nerfherder"]
        }

        let encoder = ProtoEncoder()
        let data = try encoder.encode(person)

        let decoder = ProtoDecoder()
        let decodedPerson = try decoder.decode(Person.self, from: data)

        XCTAssertEqual(decodedPerson, person)
    }

    // ensure that fields set to their identity value survive a roundtrip when omitted over the wire
    func testProto3IdentityValues() throws {
        let empty = EmptyOmitted(
            numeric_value: 0,
            string_value: "",
            bytes_value: Foundation.Data(),
            bool_value: false,
            enum_value: .UNKNOWN
        ) {
            $0.message_value = nil
            $0.repeated_value = []
            $0.map_value = [:]
        }

        let encoder = ProtoEncoder()
        let data = try encoder.encode(empty)

        let decoder = ProtoDecoder()
        let decodedEmpty = try decoder.decode(EmptyOmitted.self, from: data)

        XCTAssertEqual(decodedEmpty, empty)
    }

    func testSizeDelimited() throws {
        let values = [
            Person3(name: "John Doe", id: 123),
            Person3(name: "Jane Doe", id: 456) {
                $0.email = "jdoe@example.com"
            }
        ]

        let encoder = ProtoEncoder()
        let data = try encoder.encodeSizeDelimited(values)

        let decoder = ProtoDecoder()
        let decodedValues = try decoder.decodeSizeDelimited(Person3.self, from: data)

        XCTAssertEqual(decodedValues, values)
    }

    // ensure that an unknown value in a singular enum field decoded with the .returnNil strategy
    // is preserved in unknown fields and reemitted when the message is reencoded
    func testUnknownEnumValueInSingularFieldRoundTrip() throws {
        let data = Foundation.Data(hexEncoded: """
            20 // (Tag 4 | Varint)
            05 // Unknown enum value 5
        """)!

        let decoder = ProtoDecoder(enumDecodingStrategy: .returnNil)
        let decoded = try decoder.decode(OneOfs.self, from: data)

        XCTAssertNil(decoded.standalone_enum)
        XCTAssertEqual(decoded.unknownFields, [4: data])

        let encoder = ProtoEncoder()
        XCTAssertEqual(try encoder.encode(decoded), data)
    }

    // a preserved unknown enum value reemits after known fields, so a stale client's edit of the
    // same field is shadowed for last-wins readers — matching generated Kotlin/Java and GPB proto2
    func testEditedSingularEnumFieldReemitsPreservedUnknownValue() throws {
        let data = Foundation.Data(hexEncoded: """
            20 // (Tag 4 | Varint)
            05 // Unknown enum value 5
        """)!

        let decoder = ProtoDecoder(enumDecodingStrategy: .returnNil)
        var decoded = try decoder.decode(OneOfs.self, from: data)
        decoded.standalone_enum = .A

        let expected = Foundation.Data(hexEncoded: """
            20 // (Tag 4 | Varint)
            01 // Value 1
            20 // (Tag 4 | Varint)
            05 // Unknown enum value 5
        """)!
        let encoder = ProtoEncoder()
        XCTAssertEqual(try encoder.encode(decoded), expected)
    }

    // in proto3 the field itself backfills to the zero-value default while the raw unknown
    // value is preserved in unknown fields, keeping the reencoded bytes identical
    func testUnknownEnumValueInProto3SingularFieldRoundTrip() throws {
        let data = Foundation.Data(hexEncoded: """
            0A     // (Tag 1 | Length Delimited)
            03     // Length 3
            616263 // "abc"
            10     // (Tag 2 | Varint)
            05     // Unknown enum value 5
        """)!

        let decoder = ProtoDecoder(enumDecodingStrategy: .returnNil)
        let decoded = try decoder.decode(Person3.PhoneNumber.self, from: data)

        XCTAssertEqual(decoded.number, "abc")
        XCTAssertEqual(decoded.type, .MOBILE)
        XCTAssertEqual(decoded.unknownFields, [2: Foundation.Data(hexEncoded: "10_05")!])

        let encoder = ProtoEncoder()
        XCTAssertEqual(try encoder.encode(decoded), data)
    }
}
