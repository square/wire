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

/// Map entries decode like protobuf-java.
final class MapEntryDecodingTests: XCTestCase {

    // MARK: - Unknown tags

    func testMapEntryWithUnknownTagIsSkipped() throws {
        // Entry with key "k", value "n", and an unknown varint field 3.
        let varint = Foundation.Data(hexEncoded: "0a0a0a016b12030a016e1800")!
        let decoded = try ProtoDecoder().decode(MappyThings.self, from: varint)
        XCTAssertEqual(decoded.things, ["k": MappyThing { $0.name = "n" }])
        XCTAssertEqual(decoded.unknownFields, [:])

        // Same entry with unknown length-delimited, fixed32, and fixed64 fields.
        let others = Foundation.Data(hexEncoded: "0a1a0a016b12030a016e220200aa2d00000000310000000000000000")!
        XCTAssertEqual(try ProtoDecoder().decode(MappyThings.self, from: others).things, ["k": MappyThing { $0.name = "n" }])
    }

    func testMapEntryWithUnknownGroupIsSkipped() throws {
        // Entry with key "k", value "n", and an unknown group with tag 3 that holds a varint field.
        let data = Foundation.Data(hexEncoded: "0a0c0a016b12030a016e1b08011c")!
        let decoded = try ProtoDecoder().decode(MappyThings.self, from: data)
        XCTAssertEqual(decoded.things, ["k": MappyThing { $0.name = "n" }])
        XCTAssertEqual(decoded.unknownFields, [:])
    }

    func testMapEntryWithUnknownTagBeforeAndAfterKnownTags() throws {
        // Unknown varint field 3, key "k", unknown fixed32 field 5, then value "n".
        let data = Foundation.Data(hexEncoded: "0a0f" + "1800" + "0a016b" + "2d00000000" + "12030a016e")!
        let decoded = try ProtoDecoder().decode(MappyThings.self, from: data)
        XCTAssertEqual(decoded.things, ["k": MappyThing { $0.name = "n" }])
        XCTAssertEqual(decoded.unknownFields, [:])
    }

    // MARK: - Scalar keys and values with the wrong wire type

    func testMapEntryWithLengthDelimitedScalarValueIsSkipped() throws {
        let decoder = ProtoDecoder()

        // Each entry has the key "k" and a length-delimited value, which is skipped.
        let ints = Foundation.Data(hexEncoded: "0a07" + "0a016b" + "1202" + "0102")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: ints).string_ints, ["k": 0])

        let bools = Foundation.Data(hexEncoded: "1207" + "0a016b" + "1202" + "0102")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: bools).string_bools, ["k": false])

        let doubles = Foundation.Data(hexEncoded: "1a07" + "0a016b" + "1202" + "0102")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: doubles).string_doubles, ["k": 0.0])

        // A valid value before or after the skipped value is kept.
        let validThenSkipped = Foundation.Data(hexEncoded: "0a09" + "0a016b" + "1006" + "1202" + "0102")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: validThenSkipped).string_ints, ["k": 3])
        let skippedThenValid = Foundation.Data(hexEncoded: "0a09" + "0a016b" + "1202" + "0102" + "1006")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: skippedThenValid).string_ints, ["k": 3])
    }

    func testMapEntryWithLengthDelimitedScalarKeyIsSkipped() throws {
        let decoder = ProtoDecoder()

        // The sint32 key is length-delimited, so it is skipped and the key is the default.
        let scalars = Foundation.Data(hexEncoded: "2a07" + "0a020102" + "12016e")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: scalars).int_strings, [0: "n"])

        let things = Foundation.Data(hexEncoded: "1209" + "0a020102" + "12030a016e")!
        XCTAssertEqual(try decoder.decode(MappyThings.self, from: things).int_things, [0: MappyThing { $0.name = "n" }])
    }

    func testMapEntryWithLengthDelimitedEnumValueIsSkipped() throws {
        // Entry "k" with a length-delimited value, which is skipped, so the value is the default.
        let data = Foundation.Data(hexEncoded: "0a07" + "0a016b" + "1202" + "0102")!
        for decoder in [ProtoDecoder(), ProtoDecoder(enumDecodingStrategy: .returnNil)] {
            let decoded = try decoder.decode(MappyEnums.self, from: data)
            XCTAssertEqual(decoded.string_enums, ["k": .DEFAULT])
            XCTAssertEqual(decoded.unknownFields, [:])
        }
    }

    func testMapEntryWithLengthDelimitedScalarPastEntryThrows() throws {
        // The skipped value claims 9 bytes, but the entry ends after 5.
        let data = Foundation.Data(hexEncoded: "0a05" + "0a016b" + "1209")!
        XCTAssertThrowsError(try ProtoDecoder().decode(MappyScalars.self, from: data)) { error in
            XCTAssertTrue(error is ProtoDecoder.Error, "unexpected error \(error)")
        }
    }

    // MARK: - Missing keys and values

    func testMapEntryWithoutValueHasDefaultValue() throws {
        let decoder = ProtoDecoder()

        // Entry with key "k" and no value.
        let noValue = Foundation.Data(hexEncoded: "0a030a016b")!
        XCTAssertEqual(try decoder.decode(MappyThings.self, from: noValue).things, ["k": MappyThing()])

        // Entry with neither key nor value.
        let empty = Foundation.Data(hexEncoded: "0a00")!
        XCTAssertEqual(try decoder.decode(MappyThings.self, from: empty).things, ["": MappyThing()])

        // Entry with only an unknown field.
        let onlyUnknown = Foundation.Data(hexEncoded: "0a021800")!
        XCTAssertEqual(try decoder.decode(MappyThings.self, from: onlyUnknown).things, ["": MappyThing()])
    }

    func testMapEntryWithoutKeyHasDefaultKey() throws {
        let decoder = ProtoDecoder()

        // String key: entry with value "n" and no key.
        let valueOnly = Foundation.Data(hexEncoded: "0a05" + "12030a016e")!
        XCTAssertEqual(try decoder.decode(MappyThings.self, from: valueOnly).things, ["": MappyThing { $0.name = "n" }])

        // Integer key: field 2, entry with value "n" and no key.
        let intKey = Foundation.Data(hexEncoded: "1205" + "12030a016e")!
        XCTAssertEqual(try decoder.decode(MappyThings.self, from: intKey).int_things, [0: MappyThing { $0.name = "n" }])

        // Integer key and message value are both missing.
        let neither = Foundation.Data(hexEncoded: "1200")!
        XCTAssertEqual(try decoder.decode(MappyThings.self, from: neither).int_things, [0: MappyThing()])
    }

    func testMapEntryWithoutScalarValueHasZeroValue() throws {
        let decoder = ProtoDecoder()

        // Each entry has the key "k" and no value.
        let ints = Foundation.Data(hexEncoded: "0a030a016b")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: ints).string_ints, ["k": 0])

        let bools = Foundation.Data(hexEncoded: "12030a016b")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: bools).string_bools, ["k": false])

        let doubles = Foundation.Data(hexEncoded: "1a030a016b")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: doubles).string_doubles, ["k": 0.0])

        let strings = Foundation.Data(hexEncoded: "22030a016b")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: strings).string_strings, ["k": ""])

        // Integer key and string value are both missing.
        let neither = Foundation.Data(hexEncoded: "2a00")!
        XCTAssertEqual(try decoder.decode(MappyScalars.self, from: neither).int_strings, [0: ""])
    }

    // MARK: - Unknown enum values

    func testMapEntryWithUnknownEnumValueIsKeptAsUnknownField() throws {
        // Entries "a" to FOO, "k" to 77 (unknown), "b" to BAR.
        let bytes = Foundation.Data(hexEncoded: "0a050a01611001" + "0a050a016b104d" + "0a050a01621002")!
        let decoded = try ProtoDecoder(enumDecodingStrategy: .returnNil).decode(MappyEnums.self, from: bytes)

        XCTAssertEqual(decoded.string_enums, ["a": .FOO, "b": .BAR])
        XCTAssertEqual(decoded.unknownFields, [1: Foundation.Data(hexEncoded: "0a050a016b104d")!])

        // The unknown entry survives a round trip.
        let reencoded = try ProtoEncoder().encode(decoded)
        let roundTripped = try ProtoDecoder(enumDecodingStrategy: .returnNil).decode(MappyEnums.self, from: reencoded)
        XCTAssertEqual(roundTripped.string_enums, ["a": .FOO, "b": .BAR])
        XCTAssertEqual(roundTripped.unknownFields, decoded.unknownFields)
    }

    func testMapEntryWithUnknownEnumValueAndUnknownTagIsKeptInCanonicalForm() throws {
        // Entry "k" to 77 (unknown) with an unknown varint field 3.
        let bytes = Foundation.Data(hexEncoded: "0a070a016b104d1800")!
        let decoded = try ProtoDecoder(enumDecodingStrategy: .returnNil).decode(MappyEnums.self, from: bytes)

        XCTAssertEqual(decoded.string_enums, [:])
        XCTAssertEqual(decoded.unknownFields, [1: Foundation.Data(hexEncoded: "0a050a016b104d")!])
    }

    func testMapEntryWithKnownEnumValueAfterUnknownEnumValueUsesLastValue() throws {
        // Entry "k" with value 77 (unknown) followed by value FOO.
        let bytes = Foundation.Data(hexEncoded: "0a070a016b104d1001")!
        let decoded = try ProtoDecoder(enumDecodingStrategy: .returnNil).decode(MappyEnums.self, from: bytes)

        XCTAssertEqual(decoded.string_enums, ["k": .FOO])
        XCTAssertEqual(decoded.unknownFields, [:])
    }

    func testMapEntryWithoutEnumValueHasZeroValue() throws {
        let bytes = Foundation.Data(hexEncoded: "0a030a016b")!
        XCTAssertEqual(
            try ProtoDecoder().decode(MappyEnums.self, from: bytes).string_enums,
            ["k": .DEFAULT]
        )
        XCTAssertEqual(
            try ProtoDecoder(enumDecodingStrategy: .returnNil).decode(MappyEnums.self, from: bytes).string_enums,
            ["k": .DEFAULT]
        )
    }

    func testMapEntryWithUnknownEnumValueThrowsWhenStrategyIsThrowError() throws {
        let bytes = Foundation.Data(hexEncoded: "0a050a016b104d")!
        XCTAssertThrowsError(try ProtoDecoder().decode(MappyEnums.self, from: bytes)) { error in
            guard case ProtoDecoder.Error.unknownEnumCase(_, let fieldNumber) = error else {
                return XCTFail("Unexpected error: \(error)")
            }
            XCTAssertEqual(fieldNumber, 77)
        }
    }

    // MARK: - Malformed entries

    func testMalformedMapEntriesThrowDecoderErrors() throws {
        let payloads: [String: String] = [
            // The unknown length-delimited field is longer than the entry.
            "length-delimited past entry": "0a05" + "0a016b" + "2205" + "0102030405",
            // The unknown fixed64 field is truncated by the end of the data.
            "truncated fixed64": "0a05" + "0a016b" + "3100",
            // The unknown fixed32 field is truncated by the end of the data.
            "truncated fixed32": "0a05" + "0a016b" + "2d00",
            // The unknown varint never ends.
            "unterminated varint": "0a0e" + "0a016b" + "18ffffffffffffffffffff",
            // The unknown group never ends.
            "unterminated group": "0a06" + "0a016b" + "1b0801",
            // The unknown group ends with the wrong tag.
            "mismatched group end": "0a07" + "0a016b" + "1b0801" + "24",
            // The unknown field has the reserved wire type 6.
            "invalid wire type": "0a05" + "0a016b" + "1e00",
            // The entry has a field with tag 0.
            "tag zero": "0a05" + "0a016b" + "0000",
            // The entry is longer than the data.
            "entry past end": "0a20" + "0a016b",
            // The string key is a varint, which protobuf-java does not skip.
            "string key with varint wire type": "0a02" + "0802",
            // The key is not valid UTF-8.
            "invalid utf8 key": "0a04" + "0a02ff" + "fe",
        ]
        for (name, hex) in payloads {
            let data = Foundation.Data(hexEncoded: hex)!
            XCTAssertThrowsError(try ProtoDecoder().decode(MappyThings.self, from: data), name) { error in
                XCTAssertTrue(error is ProtoDecoder.Error, "\(name): unexpected error \(error)")
            }
            XCTAssertThrowsError(try ProtoDecoder(enumDecodingStrategy: .returnNil).decode(MappyEnums.self, from: data), name) { error in
                XCTAssertTrue(error is ProtoDecoder.Error, "\(name): unexpected error \(error)")
            }
        }
    }

    func testMapEntryWithoutValueOfMessageWithRequiredFieldThrows() throws {
        // A missing message value decodes as the default message.
        // A message with a required field has no valid default, so decoding fails with a decoder error.
        let data = Foundation.Data(hexEncoded: "0a030a016b")!
        XCTAssertThrowsError(try ProtoDecoder().decode(MappyRequiredThings.self, from: data)) { error in
            XCTAssertTrue(error is ProtoDecoder.Error, "unexpected error \(error)")
        }
    }

    func testEnumMapEntryOutsideOfMessageThrowsDecoderError() throws {
        // The type reads a map entry without a parent message that has a tag, so there is nowhere to keep it.
        struct RootMap: ProtoDecodable {
            static var protoSyntax: ProtoSyntax? { nil }
            var values: [String: MappyEnums.ValueEnum] = [:]

            init(from reader: ProtoReader) throws {
                try reader.decode(into: &values)
            }
        }

        let entry = Foundation.Data(hexEncoded: "0a016b104d")!
        XCTAssertThrowsError(try ProtoDecoder(enumDecodingStrategy: .returnNil).decode(RootMap.self, from: entry)) { error in
            XCTAssertTrue(error is ProtoDecoder.Error, "unexpected error \(error)")
        }
    }
}
