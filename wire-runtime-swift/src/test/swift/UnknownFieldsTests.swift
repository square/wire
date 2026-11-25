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
@testable import Wire
import XCTest

final class UnknownFieldsTests: XCTestCase {

    // MARK: - Tests - Decoding and Encoding

    func testUnknownFieldsOfEveryWireTypeAreEncodedByteForByte() throws {
        let unknown = varintField(100, 150)
            + fixed32Field(101, 0x0403_0201)
            + fixed64Field(102, 0x0807_0605_0403_0201)
            + lengthField(103, Array("unknown".utf8))
            + groupField(104, inner: varintField(1, 1) + groupField(2, inner: lengthField(3, [9, 9])))
        let input = Foundation.Data(varintField(1, 7) + unknown)

        let message = try ProtoDecoder().decode(SimpleOptional2.self, from: input)

        XCTAssertEqual(message.opt_int32, 7)
        XCTAssertEqual(message.unknownFields.rawData, Foundation.Data(unknown))
        XCTAssertEqual(try ProtoEncoder().encode(message), input)
    }

    func testUnknownFieldsReadOutOfOrderAreEncodedInAscendingTagOrder() throws {
        let input = Foundation.Data(
            varintField(300, 3)
                + lengthField(100, [1, 2, 3])
                + fixed32Field(200, 2)
        )
        let expected = Foundation.Data(
            lengthField(100, [1, 2, 3])
                + fixed32Field(200, 2)
                + varintField(300, 3)
        )

        let message = try ProtoDecoder().decode(SimpleOptional2.self, from: input)

        XCTAssertEqual(message.unknownFields.rawData, expected)
        XCTAssertEqual(try ProtoEncoder().encode(message), expected)
    }

    func testRepeatedUnknownTagsKeepTheirOrder() throws {
        let input = Foundation.Data(
            varintField(200, 1)
                + varintField(100, 10)
                + varintField(200, 2)
                + lengthField(100, [11])
                + varintField(200, 3)
        )
        let expected = Foundation.Data(
            varintField(100, 10)
                + lengthField(100, [11])
                + varintField(200, 1)
                + varintField(200, 2)
                + varintField(200, 3)
        )

        let message = try ProtoDecoder().decode(SimpleOptional2.self, from: input)

        XCTAssertEqual(message.unknownFields.rawData, expected)
        XCTAssertEqual(message.unknownFields.fieldsByTag, [
            100: Foundation.Data(varintField(100, 10) + lengthField(100, [11])),
            200: Foundation.Data(varintField(200, 1) + varintField(200, 2) + varintField(200, 3)),
        ])
        XCTAssertEqual(try ProtoEncoder().encode(message), expected)
    }

    func testRepeatedUnknownTagsInAscendingOrderAreKeptAsRead() throws {
        let input = Foundation.Data(varintField(500, 1) + varintField(500, 2) + varintField(500, 1) + varintField(501, 0))
        let message = try ProtoDecoder().decode(SimpleOptional2.self, from: input)
        XCTAssertEqual(try ProtoEncoder().encode(message), input)
    }

    func testManyDistinctUnknownTagsAreStoredInOneBuffer() throws {
        var input = Foundation.Data()
        for tag in 1000..<(1000 + 100_000) {
            input.append(contentsOf: varintField(UInt32(tag), 1))
        }

        let plain = try ProtoDecoder().decode(SimpleOptional2.self, from: input)
        XCTAssertEqual(plain.unknownFields.rawData, input)
        XCTAssertEqual(try ProtoEncoder().encode(plain), input)

        let extensible = try ProtoDecoder().decode(Extensible.self, from: input)
        XCTAssertEqual(extensible.unknownFields.rawData, input)
        XCTAssertEqual(try ProtoEncoder().encode(extensible), input)
    }

    #if canImport(Darwin)
    func testManyDistinctUnknownTagsUseMemoryCloseToTheInputSize() throws {
        var input = Foundation.Data()
        for tag in 1000..<(1000 + 100_000) {
            input.append(contentsOf: varintField(UInt32(tag), 1))
        }
        let decoder = ProtoDecoder()
        _ = try decoder.decode(Extensible.self, from: input)

        let before = mallocBytesInUse()
        let message = try decoder.decode(Extensible.self, from: input)
        let used = mallocBytesInUse() - before

        // Each tag used about 350 bytes when it had its own buffer, data value and cache entry.
        XCTAssertLessThan(used, 3 * input.count)
        XCTAssertEqual(message.unknownFields.rawData.count, input.count)
    }

    private func mallocBytesInUse() -> Int {
        var statistics = malloc_statistics_t()
        malloc_zone_statistics(nil, &statistics)
        return Int(statistics.size_in_use)
    }
    #endif

    func testOutOfOrderFieldsAreOrderedByTagForEveryShape() throws {
        var seed: UInt64 = 0x2545F4914F6CDD1D
        func random(_ bound: Int) -> Int {
            seed ^= seed << 13
            seed ^= seed >> 7
            seed ^= seed << 17
            return Int(seed % UInt64(bound))
        }

        let shapes: [String: [(tag: UInt32, bytes: [UInt8])]] = [
            "one tag after a larger one": [(1015, varintField(1015, 1))]
                + (0..<40_000).map { _ in (1014, varintField(1014, 1)) },
            "two alternating tags": (0..<30_000).map { index in
                let tag: UInt32 = index % 2 == 0 ? 1015 : 1014
                return (tag, varintField(tag, UInt64(index)))
            },
            "descending distinct tags": (0..<20_000).reversed().map { (UInt32(1000 + $0), varintField(UInt32(1000 + $0), 1)) },
            "random tags and sizes": (0..<20_000).map { _ in
                let tag = UInt32(1000 + random(5_000))
                return (tag, lengthField(tag, [UInt8](repeating: 7, count: random(20))))
            },
        ]
        for (name, fields) in shapes {
            var input = Foundation.Data()
            for field in fields { input.append(contentsOf: field.bytes) }
            var expected = Foundation.Data()
            for field in fields.enumerated().sorted(by: { ($0.element.tag, $0.offset) < ($1.element.tag, $1.offset) }) {
                expected.append(contentsOf: field.element.bytes)
            }

            XCTAssertEqual(UnknownFields(rawData: input).rawData, expected, name)
            let message = try ProtoDecoder().decode(Extensible.self, from: input)
            XCTAssertEqual(message.unknownFields.rawData, expected, name)
            XCTAssertEqual(try ProtoEncoder().encode(message), expected, name)
        }
    }

    func testManyDescendingUnknownTagsAreSorted() throws {
        var input = Foundation.Data()
        var expected = Foundation.Data()
        for tag in (1000..<(1000 + 20_000)).reversed() {
            input.append(contentsOf: varintField(UInt32(tag), 1))
        }
        for tag in 1000..<(1000 + 20_000) {
            expected.append(contentsOf: varintField(UInt32(tag), 1))
        }

        let message = try ProtoDecoder().decode(SimpleOptional2.self, from: input)

        XCTAssertEqual(try ProtoEncoder().encode(message), expected)
    }

    func testUnknownEnumValueIsKeptInUnknownFields() throws {
        let input = Foundation.Data(varintField(9, 99) + varintField(1, 5))
        let message = try ProtoDecoder(enumDecodingStrategy: .returnNil).decode(SimpleOptional2.self, from: input)
        XCTAssertEqual(message.opt_int32, 5)
        XCTAssertEqual(message.unknownFields.fieldsByTag, [9: Foundation.Data(varintField(9, 99))])
    }

    func testUnknownLengthDelimitedFieldIsKeptWithItsContent() throws {
        let nested = varintField(1, 1) + varintField(900, 9)
        let input = Foundation.Data(lengthField(2, nested) + varintField(901, 1))
        // Tag 2 of Extensible is not a field, so the whole nested message is unknown here.
        let message = try ProtoDecoder().decode(Extensible.self, from: input)
        XCTAssertEqual(message.unknownFields.rawData, input)
        XCTAssertEqual(try ProtoEncoder().encode(message), input)
    }

    // MARK: - Tests - Extensible Messages

    func testExtensibleMessageParsesExtensionsFromDecodedUnknownFields() throws {
        let input = Foundation.Data(
            lengthField(1, Array("name".utf8))
                + varintField(1001, 42)
                + lengthField(1014, Array("text".utf8))
                + varintField(5000, 1)
        )

        let message = try ProtoDecoder().decode(Extensible.self, from: input)

        XCTAssertEqual(message.name, "name")
        XCTAssertEqual(message.ext_int32, 42)
        XCTAssertEqual(message.ext_string, "text")
        XCTAssertNil(message.ext_bool)
        XCTAssertEqual(message.unknownFields.fieldsByTag.keys.sorted(), [1001, 1014, 5000])
        XCTAssertEqual(try ProtoEncoder().encode(message), input)
    }

    func testSettingExtensionKeepsOtherUnknownFields() throws {
        let input = Foundation.Data(varintField(1001, 42) + varintField(5000, 1) + varintField(1500, 2))
        var message = try ProtoDecoder().decode(Extensible.self, from: input)

        message.ext_bool = true
        message.ext_int32 = 43
        message.ext_string = nil

        XCTAssertEqual(message.ext_int32, 43)
        XCTAssertEqual(message.ext_bool, true)
        XCTAssertEqual(
            message.unknownFields.rawData,
            Foundation.Data(varintField(1001, 43) + varintField(1011, 1) + varintField(1500, 2) + varintField(5000, 1))
        )

        message.ext_int32 = nil
        message.ext_bool = nil
        XCTAssertEqual(message.unknownFields.rawData, Foundation.Data(varintField(1500, 2) + varintField(5000, 1)))
    }

    func testExtensibleUnknownFieldsCopyOnWrite() throws {
        let input = Foundation.Data(varintField(1001, 42) + lengthField(1014, Array("text".utf8)))
        let original = try ProtoDecoder().decode(Extensible.self, from: input)
        XCTAssertEqual(original.ext_string, "text")

        var copy = original
        copy.ext_string = "changed"
        copy.ext_int32 = 7

        XCTAssertEqual(original.ext_string, "text")
        XCTAssertEqual(original.ext_int32, 42)
        XCTAssertEqual(copy.ext_string, "changed")
        XCTAssertEqual(copy.ext_int32, 7)
        XCTAssertNotEqual(original, copy)
        XCTAssertEqual(try ProtoEncoder().encode(original), input)
    }

    func testRawSubscriptWriteClearsMemoizedExtension() throws {
        let input = Foundation.Data(varintField(1001, 42))
        var message = try ProtoDecoder().decode(Extensible.self, from: input)
        XCTAssertEqual(message.ext_int32, 42)

        message.unknownFields[1001] = Foundation.Data(varintField(1001, 8))

        XCTAssertEqual(message.ext_int32, 8)

        message.unknownFields[1001] = nil

        XCTAssertNil(message.ext_int32)
        XCTAssertTrue(message.unknownFields.isEmpty)
    }

    func testAbsentExtensionIsMemoizedUntilItIsSet() throws {
        let input = Foundation.Data(varintField(1001, 42))
        let original = try ProtoDecoder().decode(Extensible.self, from: input)
        XCTAssertNil(original.ext_bool)
        XCTAssertNil(original.ext_bool)

        var copy = original
        copy.ext_bool = true
        XCTAssertEqual(copy.ext_bool, true)
        XCTAssertNil(original.ext_bool)

        var raw = original
        raw.unknownFields[1011] = Foundation.Data(varintField(1011, 1))
        XCTAssertEqual(raw.ext_bool, true)
        XCTAssertNil(original.ext_bool)

        copy.ext_bool = nil
        XCTAssertNil(copy.ext_bool)
        XCTAssertEqual(copy, original)
    }

    func testClearingAnExtensionOnACopyBeforeAnyReadKeepsTheOriginal() throws {
        let input = Foundation.Data(varintField(1001, 42) + varintField(500, 1))
        let original = try ProtoDecoder().decode(Extensible.self, from: input)
        var copy = original

        copy.ext_int32 = nil

        XCTAssertNil(copy.ext_int32)
        XCTAssertEqual(original.ext_int32, 42)
    }

    func testClearingATagThroughTheRawSubscriptOnACopyBeforeAnyReadKeepsTheOriginal() throws {
        let input = Foundation.Data(varintField(1001, 42))
        let original = try ProtoDecoder().decode(Extensible.self, from: input)
        var copy = original

        copy.unknownFields[1001] = nil

        XCTAssertNil(copy.ext_int32)
        XCTAssertEqual(original.ext_int32, 42)
    }

    func testSettingAnAbsentTagOnACopyBeforeAnyReadDoesNotChangeTheOriginal() throws {
        let original = try ProtoDecoder().decode(Extensible.self, from: Foundation.Data(varintField(500, 1)))
        var copy = original

        copy.unknownFields[1001] = Foundation.Data(varintField(1001, 7))

        XCTAssertEqual(copy.ext_int32, 7)
        XCTAssertNil(original.ext_int32)
    }

    func testReadingAnAbsentTagOnTheOriginalDoesNotHideAValueSetOnACopy() throws {
        let original = try ProtoDecoder().decode(Extensible.self, from: Foundation.Data(varintField(500, 1)))
        var copy = original

        copy.unknownFields[1001] = Foundation.Data(varintField(1001, 7))

        XCTAssertNil(original.ext_int32)
        XCTAssertEqual(copy.ext_int32, 7)
    }

    func testClearingAnAbsentTagChangesNothing() throws {
        let input = Foundation.Data(varintField(1001, 42) + varintField(5000, 1))
        var message = try ProtoDecoder().decode(Extensible.self, from: input)
        XCTAssertEqual(message.ext_int32, 42)

        message.ext_bool = nil
        message.unknownFields[1500] = nil

        XCTAssertEqual(message.unknownFields.rawData, input)
        XCTAssertEqual(message.ext_int32, 42)
        var plain = UnknownFields(rawData: input)
        plain[7] = nil
        XCTAssertEqual(plain.rawData, input)
    }

    func testExtensibleMessageWithoutUnknownFieldsHasEmptyFields() throws {
        let message = try ProtoDecoder().decode(Extensible.self, from: Foundation.Data(lengthField(1, [0x61])))
        XCTAssertTrue(message.unknownFields.isEmpty)
        XCTAssertNil(message.ext_int32)
        XCTAssertEqual(message.unknownFields, [:])
    }

    // MARK: - Tests - Equality and Hashing

    func testEqualityAndHashing() throws {
        let a = try ProtoDecoder().decode(SimpleOptional2.self, from: Foundation.Data(varintField(100, 1) + varintField(200, 2)))
        let b = try ProtoDecoder().decode(SimpleOptional2.self, from: Foundation.Data(varintField(100, 1) + varintField(200, 2)))
        let c = try ProtoDecoder().decode(SimpleOptional2.self, from: Foundation.Data(varintField(100, 1) + varintField(200, 3)))

        XCTAssertEqual(a.unknownFields, b.unknownFields)
        XCTAssertEqual(a.unknownFields.hashValue, b.unknownFields.hashValue)
        XCTAssertNotEqual(a.unknownFields, c.unknownFields)
        XCTAssertEqual(Set([a.unknownFields, b.unknownFields, c.unknownFields]).count, 2)
    }

    func testFieldsReadInADifferentTagOrderAreEqual() throws {
        let a = try ProtoDecoder().decode(Extensible.self, from: Foundation.Data(varintField(1100, 1) + varintField(1200, 2)))
        let b = try ProtoDecoder().decode(Extensible.self, from: Foundation.Data(varintField(1200, 2) + varintField(1100, 1)))

        XCTAssertEqual(a, b)
        XCTAssertEqual(a.unknownFields.hashValue, b.unknownFields.hashValue)
        XCTAssertEqual(Set([a.unknownFields, b.unknownFields]).count, 1)
    }

    func testFieldsWithTheSameTagInADifferentOrderAreNotEqual() throws {
        let a = try ProtoDecoder().decode(Extensible.self, from: Foundation.Data(varintField(1100, 1) + varintField(1100, 2)))
        let b = try ProtoDecoder().decode(Extensible.self, from: Foundation.Data(varintField(1100, 2) + varintField(1100, 1)))

        XCTAssertNotEqual(a, b)
    }

    func testEqualityIgnoresMemoizedExtensions() throws {
        let input = Foundation.Data(varintField(1001, 42))
        let parsed = try ProtoDecoder().decode(Extensible.self, from: input)
        let untouched = try ProtoDecoder().decode(Extensible.self, from: input)
        XCTAssertEqual(parsed.ext_int32, 42)

        XCTAssertEqual(parsed, untouched)
        XCTAssertEqual(parsed.unknownFields.hashValue, untouched.unknownFields.hashValue)

        var built = Extensible()
        built.ext_int32 = 42
        XCTAssertEqual(parsed, built)
    }

    // MARK: - Tests - Migration Accessors

    func testFieldsByTagReturnsADictionaryOfTagToBytes() throws {
        let input = Foundation.Data(varintField(7, 1) + groupField(9, inner: varintField(1, 2)) + varintField(7, 3))
        let unknownFields = UnknownFields(rawData: input)

        XCTAssertEqual(unknownFields.fieldsByTag, [
            7: Foundation.Data(varintField(7, 1) + varintField(7, 3)),
            9: Foundation.Data(groupField(9, inner: varintField(1, 2))),
        ])
        XCTAssertEqual(unknownFields.count, 2)
        XCTAssertFalse(unknownFields.isEmpty)
        XCTAssertEqual(unknownFields.map { $0.key }, [7, 9])
    }

    func testFieldsByTagCanBeSet() throws {
        var unknownFields = UnknownFields()
        unknownFields.fieldsByTag = [
            30: Foundation.Data(varintField(30, 3)),
            10: Foundation.Data(varintField(10, 1)),
        ]

        XCTAssertEqual(unknownFields.rawData, Foundation.Data(varintField(10, 1) + varintField(30, 3)))
        XCTAssertEqual(UnknownFields(fieldsByTag: unknownFields.fieldsByTag), unknownFields)
    }

    func testSubscriptReadsInsertsReplacesAndRemovesFields() {
        var unknownFields: UnknownFields = [
            10: Foundation.Data(varintField(10, 1)),
            30: Foundation.Data(varintField(30, 3)),
        ]
        XCTAssertEqual(unknownFields[10], Foundation.Data(varintField(10, 1)))
        XCTAssertNil(unknownFields[20])

        unknownFields[20] = Foundation.Data(varintField(20, 2))
        unknownFields[40] = Foundation.Data(varintField(40, 4))
        unknownFields[5] = Foundation.Data(varintField(5, 0))
        XCTAssertEqual(
            unknownFields.rawData,
            Foundation.Data(varintField(5, 0) + varintField(10, 1) + varintField(20, 2) + varintField(30, 3) + varintField(40, 4))
        )

        unknownFields[20] = Foundation.Data(varintField(20, 22) + varintField(20, 23))
        XCTAssertEqual(unknownFields[20], Foundation.Data(varintField(20, 22) + varintField(20, 23)))
        XCTAssertEqual(unknownFields.count, 5)

        unknownFields[5] = nil
        unknownFields[20] = nil
        unknownFields[40] = nil
        unknownFields[99] = nil
        XCTAssertEqual(unknownFields.rawData, Foundation.Data(varintField(10, 1) + varintField(30, 3)))
    }

    func testRawDataIsOrderedByTag() {
        let unknownFields = UnknownFields(rawData: Foundation.Data(varintField(9, 1) + varintField(3, 2) + varintField(9, 3)))
        XCTAssertEqual(unknownFields.rawData, Foundation.Data(varintField(3, 2) + varintField(9, 1) + varintField(9, 3)))
    }

    func testExtensibleUnknownFieldsHasTheSameAccessors() throws {
        let input = Foundation.Data(varintField(7, 1) + varintField(9, 2) + varintField(7, 3))
        var unknownFields = try ProtoDecoder().decode(Extensible.self, from: input).unknownFields

        XCTAssertEqual(unknownFields.fieldsByTag, [
            7: Foundation.Data(varintField(7, 1) + varintField(7, 3)),
            9: Foundation.Data(varintField(9, 2)),
        ])
        XCTAssertEqual(unknownFields[9], Foundation.Data(varintField(9, 2)))
        XCTAssertEqual(unknownFields.count, 2)
        XCTAssertEqual(unknownFields.map { $0.key }, [7, 9])

        unknownFields[7] = nil
        XCTAssertEqual(unknownFields, [9: Foundation.Data(varintField(9, 2))])
        XCTAssertEqual(ExtensibleUnknownFields(fieldsByTag: unknownFields.fieldsByTag), unknownFields)
    }

    func testWriteUnknownFieldsFromDictionaryLiteral() throws {
        let writer = ProtoWriter()
        try writer.writeUnknownFields([
            2: Foundation.Data(varintField(2, 1)),
            1: Foundation.Data(varintField(1, 1)),
        ] as [UInt32: Foundation.Data])
        XCTAssertEqual(Foundation.Data(writer.buffer, copyBytes: true), Foundation.Data(varintField(1, 1) + varintField(2, 1)))
    }

    // MARK: - Tests - Codable

    func testCodableUsesADictionaryOfTagToBytes() throws {
        let unknownFields: UnknownFields = [
            1: Foundation.Data(varintField(1, 5)),
            2: Foundation.Data(fixed32Field(2, 1)),
        ]
        let encoded = try JSONEncoder().encode(unknownFields)
        let expected = try JSONEncoder().encode([UInt32: Foundation.Data](uniqueKeysWithValues: unknownFields.fieldsByTag.map { ($0.key, $0.value) }))

        // Dictionary key order is not stable, so compare the decoded forms.
        XCTAssertEqual(
            try JSONDecoder().decode([UInt32: Foundation.Data].self, from: encoded),
            try JSONDecoder().decode([UInt32: Foundation.Data].self, from: expected)
        )
        XCTAssertEqual(try JSONDecoder().decode(UnknownFields.self, from: encoded), unknownFields)
    }

    func testExtensibleUnknownFieldsCodableRoundTrip() throws {
        let input = Foundation.Data(varintField(1001, 42) + lengthField(1014, Array("text".utf8)))
        let original = try ProtoDecoder().decode(Extensible.self, from: input).unknownFields

        let decoded = try JSONDecoder().decode(ExtensibleUnknownFields.self, from: try JSONEncoder().encode(original))

        XCTAssertEqual(decoded, original)
        var message = Extensible()
        message.unknownFields = decoded
        XCTAssertEqual(message.ext_int32, 42)
        XCTAssertEqual(message.ext_string, "text")
    }

    func testCodableDropsBytesThatAreNotCompleteFields() throws {
        let good = varintField(10, 1)
        let invalid = UnknownFields(rawData: Foundation.Data(good + [0x08, 0x80]))

        let decoded = try JSONDecoder().decode(UnknownFields.self, from: try JSONEncoder().encode(invalid))

        XCTAssertEqual(decoded.rawData, Foundation.Data(good))
        XCTAssertEqual(invalid.rawData, Foundation.Data(good + [0x08, 0x80]))
    }

    // MARK: - Tests - Malformed Data

    func testBytesThatAreNotFieldsAreKeptAtTheEnd() {
        let good = varintField(10, 1)
        let junk: [UInt8] = [0x08, 0x80]  // A varint that never ends.
        let unknownFields = UnknownFields(rawData: Foundation.Data(varintField(20, 2) + good + junk))

        XCTAssertEqual(unknownFields.rawData, Foundation.Data(good + varintField(20, 2) + junk))
        XCTAssertEqual(unknownFields.fieldsByTag.count, 2)
        XCTAssertEqual(unknownFields[10], Foundation.Data(good))
    }

    func testMalformedFieldsDoNotCrash() {
        let malformed: [[UInt8]] = [
            [0x80],  // Key without end.
            [0x08],  // Varint without value.
            [0x09, 1, 2, 3],  // Short fixed64.
            [0x0D, 1, 2],  // Short fixed32.
            [0x0A, 0x05, 1],  // Short length delimited.
            [0x0A, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01],  // Length too big.
            [0x0C],  // End group without start.
            [0x0B, 0x08, 0x01],  // Unterminated group.
            [0x0E],  // Wire type 6.
            [0x0F],  // Wire type 7.
        ]
        for bytes in malformed {
            let unknownFields = UnknownFields(rawData: Foundation.Data(bytes))
            XCTAssertEqual(unknownFields.rawData, Foundation.Data(bytes))
            XCTAssertTrue(unknownFields.fieldsByTag.isEmpty)
            XCTAssertNil(unknownFields[1])
            var mutable = unknownFields
            mutable[1] = Foundation.Data(varintField(1, 1))
            XCTAssertEqual(mutable.rawData, Foundation.Data(varintField(1, 1) + bytes))
        }
    }

    func testDeeplyNestedGroupsAreScannedWithoutRecursion() {
        let depth = 200_000
        var bytes: [UInt8] = []
        for _ in 0..<depth { bytes.append(0x0B) }  // Start group, tag 1.
        for _ in 0..<depth { bytes.append(0x0C) }  // End group, tag 1.
        let unknownFields = UnknownFields(rawData: Foundation.Data(bytes))
        XCTAssertEqual(unknownFields.fieldsByTag.count, 1)
        XCTAssertEqual(unknownFields[1]?.count, bytes.count)
    }

    func testRawDataThatIsASliceIsAccepted() {
        let all = Foundation.Data([0xFF, 0xFF] + varintField(9, 1) + varintField(3, 2))
        let slice = all.dropFirst(2)
        let unknownFields = UnknownFields(rawData: slice)
        XCTAssertEqual(unknownFields.rawData, Foundation.Data(varintField(3, 2) + varintField(9, 1)))
        XCTAssertEqual(unknownFields[9], Foundation.Data(varintField(9, 1)))

        let ordered = UnknownFields(rawData: Foundation.Data(all.dropFirst(2).prefix(2)))
        XCTAssertEqual(ordered.rawData.startIndex, 0)
        XCTAssertEqual(ordered.rawData[0], varintField(9, 1)[0])
        let orderedSlice = UnknownFields(rawData: all.dropFirst(4))
        XCTAssertEqual(orderedSlice.rawData.startIndex, 0)
        XCTAssertEqual(orderedSlice.rawData[0], varintField(3, 2)[0])
    }

    func testDictionaryStyleAccessors() {
        var unknownFields = UnknownFields(minimumCapacity: 10)
        XCTAssertTrue(unknownFields.isEmpty)

        XCTAssertNil(unknownFields.updateValue(Foundation.Data(varintField(20, 2)), forKey: 20))
        XCTAssertNil(unknownFields.updateValue(Foundation.Data(varintField(10, 1)), forKey: 10))
        XCTAssertEqual(
            unknownFields.updateValue(Foundation.Data(varintField(20, 3)), forKey: 20),
            Foundation.Data(varintField(20, 2))
        )
        XCTAssertEqual(unknownFields.keys, [10, 20])
        XCTAssertEqual(unknownFields.values, [Foundation.Data(varintField(10, 1)), Foundation.Data(varintField(20, 3))])

        XCTAssertEqual(unknownFields[10, default: Foundation.Data()], Foundation.Data(varintField(10, 1)))
        XCTAssertEqual(unknownFields[99, default: Foundation.Data([1])], Foundation.Data([1]))
        unknownFields[30, default: Foundation.Data()] = Foundation.Data(varintField(30, 3))
        XCTAssertEqual(unknownFields.keys, [10, 20, 30])

        XCTAssertEqual(unknownFields.removeValue(forKey: 10), Foundation.Data(varintField(10, 1)))
        XCTAssertNil(unknownFields.removeValue(forKey: 10))
        XCTAssertEqual(unknownFields.keys, [20, 30])
    }

    func testExtensibleDictionaryStyleAccessors() throws {
        var unknownFields = ExtensibleUnknownFields(minimumCapacity: 10)
        XCTAssertNil(unknownFields.updateValue(Foundation.Data(varintField(1001, 42)), forKey: 1001))
        var message = Extensible()
        message.unknownFields = unknownFields
        XCTAssertEqual(message.ext_int32, 42)

        XCTAssertEqual(message.unknownFields.keys, [1001])
        XCTAssertEqual(message.unknownFields.values, [Foundation.Data(varintField(1001, 42))])
        XCTAssertEqual(message.unknownFields[1001, default: Foundation.Data()], Foundation.Data(varintField(1001, 42)))
        XCTAssertEqual(message.unknownFields.removeValue(forKey: 1001), Foundation.Data(varintField(1001, 42)))
        XCTAssertNil(message.ext_int32)
        XCTAssertTrue(message.unknownFields.isEmpty)
    }

    // MARK: - Tests - Size and Copy on Write

    func testUnknownFieldsIsOnePointerInSize() {
        XCTAssertEqual(MemoryLayout<UnknownFields>.size, 8)
        XCTAssertEqual(MemoryLayout<UnknownFields>.size, MemoryLayout<AnyObject>.size)
    }

    func testExtensibleUnknownFieldsIsAtMostTwoPointersInSize() {
        XCTAssertLessThanOrEqual(MemoryLayout<ExtensibleUnknownFields>.size, 16)
    }

    func testEmptyUnknownFieldsAreEmptyEverywhere() throws {
        var fields = UnknownFields()
        XCTAssertTrue(fields.isEmpty)
        XCTAssertEqual(fields.rawData, Foundation.Data())
        XCTAssertEqual(fields, UnknownFields(rawData: Foundation.Data()))
        XCTAssertEqual(fields, UnknownFields(fieldsByTag: [:]))
        XCTAssertEqual(fields.hashValue, UnknownFields(rawData: Foundation.Data()).hashValue)

        // Removing the last field makes the value empty again.
        fields[1] = Foundation.Data(varintField(1, 1))
        XCTAssertFalse(fields.isEmpty)
        fields[1] = nil
        XCTAssertTrue(fields.isEmpty)
        XCTAssertEqual(fields, UnknownFields())
    }

    func testMutatingACopyOfUnknownFieldsDoesNotChangeTheOriginal() {
        var original = UnknownFields()
        original[10] = Foundation.Data(varintField(10, 1))
        original[30] = Foundation.Data(varintField(30, 3))
        let originalBytes = original.rawData

        var copy = original
        copy[20] = Foundation.Data(varintField(20, 2))
        XCTAssertEqual(original.rawData, originalBytes)
        XCTAssertEqual(original.keys, [10, 30])
        XCTAssertEqual(copy.keys, [10, 20, 30])

        var replaced = original
        replaced[10] = Foundation.Data(varintField(10, 99))
        XCTAssertEqual(original[10], Foundation.Data(varintField(10, 1)))
        XCTAssertEqual(replaced[10], Foundation.Data(varintField(10, 99)))

        var removed = original
        removed[30] = nil
        XCTAssertEqual(original.keys, [10, 30])
        XCTAssertEqual(removed.keys, [10])

        var emptied = original
        emptied.fieldsByTag = [:]
        XCTAssertTrue(emptied.isEmpty)
        XCTAssertEqual(original.rawData, originalBytes)
    }

    func testMutatingTheOriginalDoesNotChangeItsCopies() {
        var original = UnknownFields()
        original[10] = Foundation.Data(varintField(10, 1))
        let copy = original

        original[10] = Foundation.Data(varintField(10, 2))
        original[5] = Foundation.Data(varintField(5, 5))

        XCTAssertEqual(copy.rawData, Foundation.Data(varintField(10, 1)))
        XCTAssertEqual(original.rawData, Foundation.Data(varintField(5, 5) + varintField(10, 2)))
    }

    func testMutatingAUniqueValueKeepsGivingTheRightBytes() {
        var fields = UnknownFields()
        for tag in stride(from: UInt32(50), through: 1, by: -1) {
            fields[tag] = Foundation.Data(varintField(tag, UInt64(tag)))
        }
        XCTAssertEqual(fields.keys, Array(1...50))
        XCTAssertEqual(fields.rawData, Foundation.Data((1...50).flatMap { varintField(UInt32($0), UInt64($0)) }))
    }

    func testUnknownFieldsFromDecodingAreNotChangedByMutatingACopyOfTheMessage() throws {
        let input = Foundation.Data(varintField(1, 7) + varintField(100, 1) + varintField(200, 2))
        let original = try ProtoDecoder().decode(SimpleOptional2.self, from: input)

        var copy = original
        copy.unknownFields[150] = Foundation.Data(varintField(150, 3))
        copy.opt_int32 = 8

        XCTAssertEqual(original.opt_int32, 7)
        XCTAssertEqual(original.unknownFields.keys, [100, 200])
        XCTAssertEqual(try ProtoEncoder().encode(original), input)
        XCTAssertEqual(copy.unknownFields.keys, [100, 150, 200])
        XCTAssertNotEqual(original, copy)
    }

    func testEqualityDoesNotDependOnSharingTheSameBuffer() throws {
        let input = Foundation.Data(varintField(100, 1) + varintField(200, 2))
        let first = try ProtoDecoder().decode(SimpleOptional2.self, from: input)
        let second = try ProtoDecoder().decode(SimpleOptional2.self, from: input)
        let copy = first

        XCTAssertEqual(first.unknownFields, second.unknownFields)
        XCTAssertEqual(first.unknownFields, copy.unknownFields)
        XCTAssertEqual(first.unknownFields.hashValue, second.unknownFields.hashValue)
    }

    func testExtensibleUnknownFieldsCopyOnWriteWithTheSharedBuffer() throws {
        var original = ExtensibleUnknownFields()
        original[10] = Foundation.Data(varintField(10, 1))
        var copy = original

        copy[10] = Foundation.Data(varintField(10, 2))
        copy[20] = Foundation.Data(varintField(20, 3))

        XCTAssertEqual(original.rawData, Foundation.Data(varintField(10, 1)))
        XCTAssertEqual(copy.rawData, Foundation.Data(varintField(10, 2) + varintField(20, 3)))
    }

    func testUnknownFieldsCanBeSharedAcrossThreads() {
        var fields = UnknownFields()
        for tag in 1...100 { fields[UInt32(tag)] = Foundation.Data(varintField(UInt32(tag), 1)) }
        let expected = fields.rawData
        let results = (0..<8).map { _ in Atomic(false) }

        DispatchQueue.concurrentPerform(iterations: 8) { index in
            var local = fields
            local[1000 + UInt32(index)] = Foundation.Data(varintField(1000 + UInt32(index), 1))
            results[index].value = fields.rawData == expected && local.keys.count == 101
        }

        XCTAssertTrue(results.allSatisfy { $0.value })
        XCTAssertEqual(fields.rawData, expected)
    }

    // MARK: - Helpers

    private func varintBytes(_ value: UInt64) -> [UInt8] {
        var result: [UInt8] = []
        var remaining = value
        while remaining >= 0x80 {
            result.append(UInt8(remaining & 0x7F) | 0x80)
            remaining >>= 7
        }
        result.append(UInt8(remaining))
        return result
    }

    private func key(_ tag: UInt32, _ wireType: UInt64) -> [UInt8] {
        varintBytes((UInt64(tag) << 3) | wireType)
    }

    private func varintField(_ tag: UInt32, _ value: UInt64) -> [UInt8] {
        key(tag, 0) + varintBytes(value)
    }

    private func fixed64Field(_ tag: UInt32, _ value: UInt64) -> [UInt8] {
        key(tag, 1) + (0..<8).map { UInt8((value >> (8 * UInt64($0))) & 0xFF) }
    }

    private func lengthField(_ tag: UInt32, _ bytes: [UInt8]) -> [UInt8] {
        key(tag, 2) + varintBytes(UInt64(bytes.count)) + bytes
    }

    private func groupField(_ tag: UInt32, inner: [UInt8]) -> [UInt8] {
        key(tag, 3) + inner + key(tag, 4)
    }

    private func fixed32Field(_ tag: UInt32, _ value: UInt32) -> [UInt8] {
        key(tag, 5) + (0..<4).map { UInt8((value >> (8 * UInt32($0))) & 0xFF) }
    }
}

private final class Atomic<T>: @unchecked Sendable {
    var value: T
    init(_ value: T) { self.value = value }
}
