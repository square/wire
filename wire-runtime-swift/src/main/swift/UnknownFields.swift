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
/// A dictionary value must hold only complete wire fields that carry its key as their tag.
/// The dictionary style accessors (`subscript`, `keys`, `values`, `count`, `removeValue` and `updateValue`)
/// read the bytes each time, so their cost is linear in the size of the data.
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
        // This also moves a `Data` slice to indices that start at zero.
        self.rawData = Data(rawData)
        self.canonicalize()
    }

    /// Creates unknown fields from bytes that are already ordered by tag.
    internal init(orderedRawData: Data) {
        self.rawData = orderedRawData
    }

    /// Creates unknown fields from a dictionary of tag to wire bytes.
    ///
    /// Each value must hold the complete wire fields for its tag, as returned by `fieldsByTag`.
    /// Bytes that are not fields with the key as their tag are written as they are, but are found under another key or none.
    /// Debug builds assert that each value follows this rule.
    public init(fieldsByTag: [UInt32: Data]) {
        for (tag, data) in fieldsByTag {
            assert(Self.isValid(data, forTag: tag), "Value for tag \(tag) must hold only wire fields with that tag")
        }
        self.init(uncheckedFieldsByTag: fieldsByTag)
    }

    internal init(uncheckedFieldsByTag fieldsByTag: [UInt32: Data]) {
        var data = Data()
        for tag in fieldsByTag.keys.sorted() {
            data.append(fieldsByTag[tag]!)
        }
        self.rawData = data
    }

    /// Creates empty unknown fields. The capacity is ignored.
    public init(minimumCapacity: Int) {
        self.init()
    }

    public var isEmpty: Bool { rawData.isEmpty }

    /// The number of distinct tags. This reads all the data.
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
    /// Each value that is set must hold only complete wire fields that carry its key as their tag.
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

    /// The tags that have fields, in ascending order. This reads all the data.
    public var keys: [UInt32] {
        var result: [UInt32] = []
        scanFields { tag, _, _ in
            if result.last != tag { result.append(tag) }
            return true
        }
        return result
    }

    /// The wire bytes of the fields with each tag, in ascending tag order. This reads all the data.
    public var values: [Data] {
        let fields = fieldsByTag
        return keys.map { fields[$0]! }
    }

    /// Removes all fields with this tag and returns their bytes, if there were any.
    @discardableResult
    public mutating func removeValue(forKey key: UInt32) -> Data? {
        let old = self[key]
        self[key] = nil
        return old
    }

    /// Replaces all fields with this tag and returns their previous bytes, if there were any.
    /// The value must hold only complete wire fields that carry the key as their tag.
    @discardableResult
    public mutating func updateValue(_ value: Data, forKey key: UInt32) -> Data? {
        let old = self[key]
        self[key] = value
        return old
    }

    /// The wire bytes of the fields with this tag, or the default if there are none.
    public subscript(key: UInt32, default defaultValue: @autoclosure () -> Data) -> Data {
        get { self[key] ?? defaultValue() }
        set { self[key] = newValue }
    }

    /// The wire bytes of the fields with this tag, or nil if there are none.
    ///
    /// Setting a value replaces all fields with the tag.
    /// The value must hold only complete wire fields that carry this tag.
    /// Other bytes are written as they are, but are found under another key or none.
    /// Debug builds assert this rule.
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
            if let newValue {
                assert(Self.isValid(newValue, forTag: key), "Value for tag \(key) must hold only wire fields with that tag")
            }
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
            // Removing a tag that is not there changes nothing, so skip the copy.
            if newValue == nil && upper == 0 { return }
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

    /// Whether `data` is made only of complete wire fields that carry `tag`.
    private static func isValid(_ data: Data, forTag tag: UInt32) -> Bool {
        var isValid = true
        let end = data.withUnsafeBytes { bytes in
            scan(bytes) { fieldTag, _, _ in
                if fieldTag != tag { isValid = false }
                return isValid
            }
        }
        return isValid && end == data.count
    }

    /// Orders the fields by tag. Fields with the same tag keep their order.
    private mutating func canonicalize() {
        if let ordered = rawData.withUnsafeBytes({ Self.orderedCopy(of: $0) }) {
            rawData = ordered
        }
    }

    /// Creates unknown fields from bytes that may be out of order, without first copying them to a `Data`.
    internal init(unorderedBytes bytes: UnsafeRawBufferPointer) {
        self.rawData = Self.orderedCopy(of: bytes) ?? Data(bytes)
    }

    /// Returns the fields ordered by tag, or nil if they already are. Fields with the same tag keep their order.
    ///
    /// The extra memory used depends on the number of distinct tags, not on the number of fields.
    private static func orderedCopy(of source: UnsafeRawBufferPointer) -> Data? {
        var isOrdered = true
        var lastTag: UInt32 = 0
        scan(source) { tag, _, _ in
            if tag < lastTag {
                isOrdered = false
                return false
            }
            lastTag = tag
            return true
        }
        if isOrdered { return nil }

        // First pass: add up the bytes of each tag.
        var table: [TagOffset] = []
        var limit = 4096
        let parsedEnd = scan(source) { tag, start, end in
            if table.last?.tag == tag {
                table[table.count - 1].offset += end - start
            } else {
                table.append(TagOffset(tag: tag, offset: end - start))
                if table.count >= limit {
                    mergeEntries(&table)
                    limit = Swift.max(limit, table.count * 2)
                }
            }
            return true
        }
        mergeEntries(&table)
        // Turn the sizes into the offset where the bytes of each tag start.
        var offset = 0
        for index in table.indices {
            let size = table[index].offset
            table[index].offset = offset
            offset += size
        }

        // Second pass: copy the fields to their place.
        var result = Data(count: source.count)
        result.withUnsafeMutableBytes { target in
            guard let sourceBase = source.baseAddress, let targetBase = target.baseAddress else { return }
            var currentTag: UInt32?
            var index = 0
            _ = scan(source) { tag, start, end in
                if tag != currentTag {
                    currentTag = tag
                    var low = 0
                    var high = table.count
                    while low < high {
                        let middle = (low + high) / 2
                        if table[middle].tag < tag { low = middle + 1 } else { high = middle }
                    }
                    index = low
                }
                memcpy(targetBase + table[index].offset, sourceBase + start, end - start)
                table[index].offset += end - start
                return true
            }
            memcpy(targetBase + parsedEnd, sourceBase + parsedEnd, source.count - parsedEnd)
        }
        return result
    }

    /// Sorts entries by tag and adds up the sizes of entries with the same tag.
    private static func mergeEntries(_ table: inout [TagOffset]) {
        table.sort { $0.tag < $1.tag }
        var written = 0
        for index in table.indices {
            if written > 0 && table[written - 1].tag == table[index].tag {
                table[written - 1].offset += table[index].offset
            } else {
                table[written] = table[index]
                written += 1
            }
        }
        table.removeLast(table.count - written)
    }
}

/// The bytes used by a tag, or the offset where they start.
private struct TagOffset {
    var tag: UInt32
    var offset: Int
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
/// Earlier versions use the same format.
///
/// It holds only what `fieldsByTag` returns, so bytes that are not complete wire fields are not coded.
/// This applies only to values that were built from invalid bytes, such as `init(rawData:)` with a truncated field.
/// Unknown fields that come from decoding or from the accessors of this type are coded without loss.
/// Fields are coded by tag, so after decoding the fields of different tags are in ascending tag order, as they always are.
extension UnknownFields: Codable {
    public init(from decoder: any Decoder) throws {
        self.init(uncheckedFieldsByTag: try [UInt32: Data](from: decoder))
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
