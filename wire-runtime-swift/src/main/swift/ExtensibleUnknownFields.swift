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

    /// The wire bytes of the fields with each tag.
    ///
    /// Setting this property replaces all unknown fields and clears all memoized values.
    public var fieldsByTag: [UInt32: Data] {
        get { fields.fieldsByTag }
        set { self = ExtensibleUnknownFields(fieldsByTag: newValue) }
    }

    /// The wire bytes of the fields with this tag, or nil if there are none.
    ///
    /// Setting a value replaces all fields with the tag and clears its memoized value.
    public subscript(key: Key) -> Value? {
        get {
            fields[key]
        }
        set {
            fields[key] = newValue
            updateMemoizedValues(for: key, to: nil)
        }
    }

    /// This will fetch a memoized value based on the backing data.
    func getParsedField<T>(fieldNumber: Key, compute: (Value) throws -> T) rethrows -> T? {
        if let memoizedValues, let value: T = memoizedValues.value(for: fieldNumber) {
            return value
        }
        guard let data = fields[fieldNumber] else {
            return nil
        }
        let value = try compute(data)
        memoizedValues?.store(value, for: fieldNumber)
        return value
    }

    /// This will set both a memoized value and its backing data.
    mutating func setParsedField<T>(fieldNumber: Key, value: T?, compute: (T) throws -> Value) rethrows {
        guard let value else {
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

extension ExtensibleUnknownFields: Codable, Equatable, Hashable, Sendable {
    public init(from decoder: any Decoder) throws {
        self.init(UnknownFields(fieldsByTag: try [UInt32: Data](from: decoder)))
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

    /// This returns the cached value if it exists in the expected output type.
    func value<T>(for key: UInt32) -> T? {
        lock.withLock { values[key] as? T }
    }

    func store(_ value: Any?, for key: UInt32) {
        lock.withLock { values[key] = value }
    }
}
