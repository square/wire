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

/// This is a dictionary intended for storing raw data plus memoized values.
///
/// It should generally support `Dictionary<>`'s APIs to access the raw data.
/// Mutating the raw data will clear any associated memorized values.
public struct ExtensibleUnknownFields {
    public typealias BackingStore = Dictionary<UInt32, Data>
    public typealias Key = BackingStore.Key
    public typealias Value = BackingStore.Value

    internal private(set) var rawData: BackingStore
    private var memoizedData: [Key: MemoizedValue]

    public init() {
        rawData = [:]
        memoizedData = [:]
    }

    internal init(rawData: BackingStore) {
        self.rawData = rawData
        self.memoizedData = rawData.mapValues { _ in
            MemoizedValue()
        }
    }

    public subscript(key: Key) -> Value? {
        get {
            rawData[key]
        }
        set {
            rawData[key] = newValue
            memoizedData[key] = newValue.map { _ in
                MemoizedValue()
            }
        }
    }

    /// This will fetch a memoized value based on the backing data.
    func getParsedField<T>(fieldNumber: Key, compute: (Value) throws -> T) rethrows -> T? {
        guard let data = rawData[fieldNumber] else {
            return nil
        }
        return try memoizedData[fieldNumber]?.get {
            try compute(data)
        }
    }

   /// This will set both a memoized value and its backing data.
    mutating func setParsedField<T>(fieldNumber: Key, value: T?, compute: (T) throws -> Value) rethrows {
        guard let value else {
            rawData[fieldNumber] = nil
            memoizedData[fieldNumber] = nil
            return
        }
        let data = try compute(value)
        rawData[fieldNumber] = data
        memoizedData[fieldNumber] = MemoizedValue(cached: value)
    }
}

extension ExtensibleUnknownFields: Codable, Equatable, Hashable, Sendable {
    public init(from decoder: any Decoder) throws {
        let rawData = try BackingStore(from: decoder)
        self.init(rawData: rawData)
    }

    public func encode(to encoder: any Encoder) throws {
        try rawData.encode(to: encoder)
    }

    public static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.rawData == rhs.rawData
    }

    public func hash(into hasher: inout Hasher) {
        rawData.hash(into: &hasher)
    }
}

extension ExtensibleUnknownFields: CustomDebugStringConvertible, CustomStringConvertible {
    public var description: String { rawData.description }
    public var debugDescription: String { rawData.debugDescription }
}

extension ExtensibleUnknownFields: ExpressibleByDictionaryLiteral {
    public init(dictionaryLiteral: (Key, Value)...) {
        self.init(uniqueKeysWithValues: dictionaryLiteral)
    }

    public init<S>(uniqueKeysWithValues keysAndValues: S) where S : Sequence, S.Element == (Key, Value) {
        self.init(rawData: BackingStore(uniqueKeysWithValues: keysAndValues))
    }
}

extension ExtensibleUnknownFields: Collection {
    public typealias Index = BackingStore.Index
    public typealias Element = BackingStore.Element

    public var isEmpty: Bool { rawData.isEmpty }
    public var count: Int { rawData.count }

    public var startIndex: BackingStore.Index {rawData.startIndex }
    public var endIndex: BackingStore.Index { rawData.endIndex }
    public var indices: BackingStore.Indices { rawData.indices }
    public var underestimatedCount: Int { rawData.underestimatedCount }

    public func index(after i: BackingStore.Index) -> BackingStore.Index {
        rawData.index(after: i)
    }

    public subscript(index: Index) -> Element {
        rawData[index]
    }

    public func index(forKey key: Key) -> Index? {
        rawData.index(forKey: key)
    }

    public var keys: BackingStore.Keys { rawData.keys }
    public var values: BackingStore.Values { rawData.values }
}

/// This holds a memoized value in a type erased manner.
///
/// It is a class type to allow for immutable get operations.
private class MemoizedValue: @unchecked Sendable {
    private var cached: Any?

    private let lock = NSLock()

    init(cached: Any? = nil) {
        self.cached = cached
    }

    /// This will attempt to fetch a cached value in the expected output type.
    ///
    /// If the requested value is the wrong type _or_ of it has not been instantiated yet, it will be computed.
    func get<T>(compute: () throws -> T) rethrows -> T {
        try lock.withLock {
            if let value = cached as? T {
                return value
            }

            let newValue = try compute()
            cached = newValue
            return newValue
        }
    }
}
