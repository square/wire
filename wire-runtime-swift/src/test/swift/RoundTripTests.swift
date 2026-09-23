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

    // a recognized value followed by an unknown occurrence of the same singular enum tag keeps the
    // recognized value; the unknown occurrence is only retained in unknown fields
    func testUnknownEnumOccurrenceDoesNotOverwriteRecognizedSingularValue() throws {
        let data = Foundation.Data(hexEncoded: """
            20 // (Tag 4 | Varint)
            01 // Value 1
            20 // (Tag 4 | Varint)
            05 // Unknown enum value 5
        """)!

        let decoder = ProtoDecoder(enumDecodingStrategy: .returnNil)
        let decoded = try decoder.decode(OneOfs.self, from: data)

        XCTAssertEqual(decoded.standalone_enum, .A)
        XCTAssertEqual(decoded.unknownFields, [4: Foundation.Data(hexEncoded: "20_05")!])

        let encoder = ProtoEncoder()
        XCTAssertEqual(try encoder.encode(decoded), data)
    }

    // an unknown value in a oneof enum field does not select that case, so a previously decoded
    // sibling case survives
    func testUnknownEnumOccurrenceDoesNotClearRecognizedOneOfCase() throws {
        let data = Foundation.Data(hexEncoded: """
            08 // (Tag 1 | Varint)
            01 // Value 1
            10 // (Tag 2 | Varint)
            05 // Unknown enum value 5
        """)!

        let decoder = ProtoDecoder(enumDecodingStrategy: .returnNil)
        let decoded = try decoder.decode(OneOfs.self, from: data)

        XCTAssertEqual(decoded.choice, .enum_option(.A))
        XCTAssertEqual(decoded.unknownFields, [2: Foundation.Data(hexEncoded: "10_05")!])

        let encoder = ProtoEncoder()
        XCTAssertEqual(try encoder.encode(decoded), data)
    }

    // when the oneof also has a message case, an unknown enum occurrence must not claim the oneof's
    // tag, or the already decoded message case is dropped after the field loop
    func testUnknownEnumOccurrenceDoesNotClearRecognizedOneOfMessageCase() throws {
        let data = Foundation.Data(hexEncoded: """
            2A // (Tag 5 | Length Delimited)
            02 // Length 2
            08 // (Tag 1 | Varint)
            07 // Value 7
            08 // (Tag 1 | Varint)
            05 // Unknown enum value 5
        """)!

        let decoder = ProtoDecoder(enumDecodingStrategy: .returnNil)
        let decoded = try decoder.decode(OneOfs.self, from: data)

        XCTAssertEqual(decoded.choice, .message_option(OneOfs.NestedMessage { $0.id = 7 }))
        XCTAssertEqual(decoded.unknownFields, [1: Foundation.Data(hexEncoded: "08_05")!])

        let encoder = ProtoEncoder()
        XCTAssertEqual(try encoder.encode(decoded), data)
    }

    // in proto3 an unknown occurrence must not reset a recognized value to the zero-value default
    func testUnknownEnumOccurrenceDoesNotOverwriteRecognizedProto3Value() throws {
        let data = Foundation.Data(hexEncoded: """
            0A     // (Tag 1 | Length Delimited)
            03     // Length 3
            616263 // "abc"
            10     // (Tag 2 | Varint)
            01     // Value 1
            10     // (Tag 2 | Varint)
            05     // Unknown enum value 5
        """)!

        let decoder = ProtoDecoder(enumDecodingStrategy: .returnNil)
        let decoded = try decoder.decode(Person3.PhoneNumber.self, from: data)

        XCTAssertEqual(decoded.number, "abc")
        XCTAssertEqual(decoded.type, .HOME)
        XCTAssertEqual(decoded.unknownFields, [2: Foundation.Data(hexEncoded: "10_05")!])

        let encoder = ProtoEncoder()
        XCTAssertEqual(try encoder.encode(decoded), data)
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
