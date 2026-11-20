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

/// The fields of a message that its schema does not know, kept in their wire format.
///
/// All fields share one `Data` value, so the memory used is close to the size of the input.
/// Fields are ordered by tag, and fields with the same tag keep the order in which they were read.
/// This is the order Wire writes them in, and two values are equal when their bytes are equal.
///
/// Use `fieldsByTag` to read or replace the fields as a dictionary of tag to wire bytes.
public struct UnknownFields: Sendable {
    public typealias Key = UInt32
    public typealias Value = Data

    /// The wire bytes of every field, ordered by tag.
    public private(set) var rawData: Data

    public init() {
        rawData = Data()
    }

    /// Creates unknown fields from wire bytes.
    ///
    /// Fields are reordered by tag when necessary.
    /// Bytes that are not valid fields are kept at the end, as they are.
    public init(rawData: Data) {
        self.rawData = rawData
        self.canonicalize()
    }

    /// Creates unknown fields from bytes that are already ordered by tag.
    internal init(orderedRawData: Data) {
        self.rawData = orderedRawData
    }

    /// Creates unknown fields from a dictionary of tag to wire bytes.
    ///
    /// Each value must hold the complete fields for its tag, as returned by `fieldsByTag`.
    public init(fieldsByTag: [UInt32: Data]) {
        var data = Data()
        for tag in fieldsByTag.keys.sorted() {
            data.append(fieldsByTag[tag]!)
        }
        self.rawData = data
    }

    public var isEmpty: Bool { rawData.isEmpty }

    /// The number of distinct tags.
    public var count: Int {
        var count = 0
        var last: UInt32?
        scanFields { tag, _, _ in
            if tag != last {
                count += 1
                last = tag
            }
            return true
        }
        return count
    }

    /// The wire bytes of the fields with each tag.
    ///
    /// A value holds every occurrence of the tag, one after the other.
    /// Setting this property replaces all unknown fields.
    public var fieldsByTag: [UInt32: Data] {
        get {
            var result: [UInt32: Data] = [:]
            rawData.withUnsafeBytes { bytes in
                _ = Self.scan(bytes) { tag, start, end in
                    result[tag, default: Data()].append(bytes, start..<end)
                    return true
                }
            }
            return result
        }
        set {
            self = UnknownFields(fieldsByTag: newValue)
        }
    }

    /// The wire bytes of the fields with this tag, or nil if there are none.
    ///
    /// Setting a value replaces all fields with the tag.
    /// The value must hold only fields with this tag.
    /// This reads and writes in time linear to the size of the data.
    public subscript(key: UInt32) -> Data? {
        get {
            var result: Data?
            rawData.withUnsafeBytes { bytes in
                _ = Self.scan(bytes) { tag, start, end in
                    if tag > key { return false }
                    if tag == key {
                        result = result ?? Data()
                        result?.append(bytes, start..<end)
                    }
                    return true
                }
            }
            return result
        }
        set {
            var lower: Int?
            var upper = 0
            let parsedEnd = rawData.withUnsafeBytes { bytes in
                Self.scan(bytes) { tag, start, end in
                    if tag < key { return true }
                    if lower == nil { lower = start }
                    if tag > key { return false }
                    upper = end
                    return true
                }
            }
            let insertAt = lower ?? parsedEnd
            let removeEnd = Swift.max(upper, insertAt)
            var result = Data()
            result.reserveCapacity(rawData.count + (newValue?.count ?? 0))
            rawData.withUnsafeBytes { bytes in
                result.append(bytes, 0..<insertAt)
                if let newValue { result.append(newValue) }
                result.append(bytes, removeEnd...)
            }
            rawData = result
        }
    }

    // MARK: - Scanning

    /// Calls `body` with the tag and the byte range of each field until `body` returns false.
    ///
    /// Returns the offset where scanning stopped.
    /// It is the end of the data unless a field is malformed.
    @discardableResult
    private func scanFields(_ body: (UInt32, Int, Int) -> Bool) -> Int {
        rawData.withUnsafeBytes { Self.scan($0, body) }
    }

    @discardableResult
    private static func scan(_ bytes: UnsafeRawBufferPointer, _ body: (UInt32, Int, Int) -> Bool) -> Int {
        var position = 0
        while position < bytes.count {
            let start = position
            guard let key = readVarint(bytes, &position), skipValue(bytes, &position, wireType: Int(key & 7)) else {
                return start
            }
            if !body(UInt32(truncatingIfNeeded: key) >> 3, start, position) {
                return start
            }
        }
        return position
    }

    private static func readVarint(_ bytes: UnsafeRawBufferPointer, _ position: inout Int) -> UInt64? {
        var result: UInt64 = 0
        var shift: UInt64 = 0
        while shift < 64, position < bytes.count {
            let byte = bytes[position]
            position += 1
            result |= UInt64(byte & 0x7F) << shift
            if byte < 0x80 {
                return result
            }
            shift += 7
        }
        return nil
    }

    /// Moves `position` past a field value, including a whole group.
    private static func skipValue(_ bytes: UnsafeRawBufferPointer, _ position: inout Int, wireType: Int) -> Bool {
        var wireType = wireType
        var groupDepth = 0
        while true {
            switch wireType {
            case 0:
                guard readVarint(bytes, &position) != nil else { return false }
            case 1:
                guard bytes.count - position >= 8 else { return false }
                position += 8
            case 2:
                guard let length = readVarint(bytes, &position), length <= UInt64(bytes.count - position) else { return false }
                position += Int(length)
            case 3:
                groupDepth += 1
            case 4:
                guard groupDepth > 0 else { return false }
                groupDepth -= 1
            case 5:
                guard bytes.count - position >= 4 else { return false }
                position += 4
            default:
                return false
            }
            if groupDepth == 0 {
                return true
            }
            // Still inside a group, so read the next key.
            guard let key = readVarint(bytes, &position) else { return false }
            wireType = Int(key & 7)
        }
    }

    /// Orders the fields by tag. Fields with the same tag keep their order.
    private mutating func canonicalize() {
        struct Field {
            var tag: UInt32
            var start: Int
            var end: Int
        }
        var fields: [Field] = []
        var isOrdered = true
        var lastTag: UInt32 = 0
        let parsedEnd = scanFields { tag, start, end in
            if tag < lastTag { isOrdered = false }
            lastTag = tag
            fields.append(Field(tag: tag, start: start, end: end))
            return true
        }
        if isOrdered { return }

        fields.sort { ($0.tag, $0.start) < ($1.tag, $1.start) }
        var result = Data()
        result.reserveCapacity(rawData.count)
        rawData.withUnsafeBytes { bytes in
            for field in fields {
                result.append(bytes, field.start..<field.end)
            }
            result.append(bytes, parsedEnd...)
        }
        rawData = result
    }
}

extension UnknownFields: Equatable, Hashable {
    public static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.rawData == rhs.rawData
    }

    public func hash(into hasher: inout Hasher) {
        rawData.hash(into: &hasher)
    }
}

/// The coded form is a dictionary of tag to wire bytes, as it was before unknown fields were stored as one value.
extension UnknownFields: Codable {
    public init(from decoder: any Decoder) throws {
        self.init(fieldsByTag: try [UInt32: Data](from: decoder))
    }

    public func encode(to encoder: any Encoder) throws {
        try fieldsByTag.encode(to: encoder)
    }
}

extension UnknownFields: ExpressibleByDictionaryLiteral {
    public init(dictionaryLiteral elements: (UInt32, Data)...) {
        self.init(uniqueKeysWithValues: elements)
    }

    public init<S>(uniqueKeysWithValues keysAndValues: S) where S: Sequence, S.Element == (UInt32, Data) {
        self.init(fieldsByTag: [UInt32: Data](uniqueKeysWithValues: keysAndValues))
    }
}

/// Iterates over the tags in ascending order, with the wire bytes of each tag.
extension UnknownFields: Sequence {
    public typealias Element = (key: UInt32, value: Data)

    public func makeIterator() -> IndexingIterator<[Element]> {
        let fields = fieldsByTag
        return fields.keys.sorted().map { (key: $0, value: fields[$0]!) }.makeIterator()
    }
}

extension UnknownFields: CustomDebugStringConvertible, CustomStringConvertible {
    public var description: String { fieldsByTag.description }
    public var debugDescription: String { fieldsByTag.debugDescription }
}

private extension Data {
    mutating func append(_ bytes: UnsafeRawBufferPointer, _ range: Range<Int>) {
        guard let base = bytes.baseAddress, !range.isEmpty else { return }
        append(base.advanced(by: range.lowerBound).assumingMemoryBound(to: UInt8.self), count: range.count)
    }

    mutating func append(_ bytes: UnsafeRawBufferPointer, _ range: PartialRangeFrom<Int>) {
        append(bytes, range.lowerBound..<bytes.count)
    }
}
