/*
 * Copyright (C) 2024 Square, Inc.
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

/// The unknown fields of an extensible message, plus the values parsed from them.
///
/// The fields are stored as one `UnknownFields` value, so the memory used is close to the size of the input.
/// Values parsed for extension fields are memoized by tag, and mutating the fields clears the memoized value of that tag.
/// A dictionary value must hold only complete wire fields that carry its key as their tag.
/// The dictionary style accessors (`subscript`, `keys`, `values`, `count`, `removeValue` and `updateValue`)
/// read the bytes each time, so their cost is linear in the size of the data.
public struct ExtensibleUnknownFields {
    public typealias Key = UInt32
    public typealias Value = Data

    internal private(set) var fields: UnknownFields

    /// Memoized values. This is nil only when there are no fields.
    private var memoizedValues: MemoizedValues?

    public init() {
        fields = UnknownFields()
        memoizedValues = nil
    }

    public init(_ fields: UnknownFields) {
        self.fields = fields
        self.memoizedValues = fields.isEmpty ? nil : MemoizedValues()
    }

    /// The wire bytes of every field, ordered by tag.
    public var rawData: Data { fields.rawData }

    public init(rawData: Data) {
        self.init(UnknownFields(rawData: rawData))
    }

    public init(fieldsByTag: [UInt32: Data]) {
        self.init(UnknownFields(fieldsByTag: fieldsByTag))
    }

    /// Creates empty unknown fields. The capacity is ignored.
    public init(minimumCapacity: Int) {
        self.init()
    }

    /// The wire bytes of the fields with each tag.
    ///
    /// Setting this property replaces all unknown fields and clears all memoized values.
    /// Each value that is set must hold only complete wire fields that carry its key as their tag.
    public var fieldsByTag: [UInt32: Data] {
        get { fields.fieldsByTag }
        set { self = ExtensibleUnknownFields(fieldsByTag: newValue) }
    }

    /// The wire bytes of the fields with this tag, or nil if there are none.
    ///
    /// Setting a value replaces all fields with the tag and clears its memoized value.
    /// The value must hold only complete wire fields that carry this tag.
    /// This reads and writes in time linear to the size of the data.
    public subscript(key: Key) -> Value? {
        get {
            fields[key]
        }
        set {
            // Clearing a tag that is not there changes nothing, so skip the copy.
            if newValue == nil, fields[key] == nil { return }
            fields[key] = newValue
            updateMemoizedValues(for: key, to: nil)
        }
    }

    /// The tags that have fields, in ascending order. This reads all the data.
    public var keys: [UInt32] { fields.keys }

    /// The wire bytes of the fields with each tag, in ascending tag order. This reads all the data.
    public var values: [Data] { fields.values }

    /// Removes all fields with this tag and returns their bytes, if there were any.
    @discardableResult
    public mutating func removeValue(forKey key: Key) -> Value? {
        let old = self[key]
        self[key] = nil
        return old
    }

    /// Replaces all fields with this tag and returns their previous bytes, if there were any.
    @discardableResult
    public mutating func updateValue(_ value: Value, forKey key: Key) -> Value? {
        let old = self[key]
        self[key] = value
        return old
    }

    /// The wire bytes of the fields with this tag, or the default if there are none.
    public subscript(key: Key, default defaultValue: @autoclosure () -> Value) -> Value {
        get { self[key] ?? defaultValue() }
        set { self[key] = newValue }
    }

    /// This will fetch a memoized value based on the backing data.
    ///
    /// A tag that is not there is memoized too, so repeated reads do not scan the data.
    func getParsedField<T>(fieldNumber: Key, compute: (Value) throws -> T) rethrows -> T? {
        if let memoizedValues, let memoized = memoizedValues.entry(for: fieldNumber) {
            if memoized is AbsentField { return nil }
            if let value = memoized as? T { return value }
        }
        guard let data = fields[fieldNumber] else {
            memoizedValues?.store(AbsentField(), for: fieldNumber)
            return nil
        }
        let value = try compute(data)
        memoizedValues?.store(value, for: fieldNumber)
        return value
    }

    /// This will set both a memoized value and its backing data.
    mutating func setParsedField<T>(fieldNumber: Key, value: T?, compute: (T) throws -> Value) rethrows {
        guard let value else {
            if fields[fieldNumber] == nil { return }
            fields[fieldNumber] = nil
            updateMemoizedValues(for: fieldNumber, to: nil)
            return
        }
        fields[fieldNumber] = try compute(value)
        updateMemoizedValues(for: fieldNumber, to: value)
    }

    private mutating func updateMemoizedValues(for key: Key, to value: Any?) {
        if fields.isEmpty {
            memoizedValues = nil
            return
        }
        // Copies share the memoized values, so write to them only when this is the only copy.
        if memoizedValues == nil {
            memoizedValues = MemoizedValues()
        } else if !isKnownUniquelyReferenced(&memoizedValues) {
            memoizedValues = memoizedValues?.copy()
        }
        memoizedValues?.store(value, for: key)
    }
}

/// The coded form is the one of `UnknownFields`: a dictionary of tag to wire bytes.
/// Bytes that are not complete wire fields are not coded, and memoized values are not coded.
extension ExtensibleUnknownFields: Codable, Equatable, Hashable, Sendable {
    public init(from decoder: any Decoder) throws {
        self.init(UnknownFields(uncheckedFieldsByTag: try [UInt32: Data](from: decoder)))
    }

    public func encode(to encoder: any Encoder) throws {
        try fields.encode(to: encoder)
    }

    public static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.fields == rhs.fields
    }

    public func hash(into hasher: inout Hasher) {
        fields.hash(into: &hasher)
    }
}

extension ExtensibleUnknownFields: CustomDebugStringConvertible, CustomStringConvertible {
    public var description: String { fields.description }
    public var debugDescription: String { fields.debugDescription }
}

extension ExtensibleUnknownFields: ExpressibleByDictionaryLiteral {
    public init(dictionaryLiteral elements: (Key, Value)...) {
        self.init(uniqueKeysWithValues: elements)
    }

    public init<S>(uniqueKeysWithValues keysAndValues: S) where S: Sequence, S.Element == (Key, Value) {
        self.init(UnknownFields(uniqueKeysWithValues: keysAndValues))
    }
}

/// Iterates over the tags in ascending order, with the wire bytes of each tag.
extension ExtensibleUnknownFields: Sequence {
    public typealias Element = UnknownFields.Element

    public var isEmpty: Bool { fields.isEmpty }

    /// The number of distinct tags.
    public var count: Int { fields.count }

    public func makeIterator() -> IndexingIterator<[Element]> {
        fields.makeIterator()
    }
}

/// This holds memoized values by tag in a type erased manner.
///
/// It is a class type to allow for immutable get operations.
private final class MemoizedValues: @unchecked Sendable {
    private var values: [UInt32: Any]

    private let lock = NSLock()

    init(values: [UInt32: Any] = [:]) {
        self.values = values
    }

    func copy() -> MemoizedValues {
        lock.withLock { MemoizedValues(values: values) }
    }

    /// This returns the cached value, or an `AbsentField` if the tag is known to be missing.
    func entry(for key: UInt32) -> Any? {
        lock.withLock { values[key] }
    }

    func store(_ value: Any?, for key: UInt32) {
        lock.withLock { values[key] = value }
    }
}

/// Marks a tag that was looked up and has no fields.
private struct AbsentField {}
