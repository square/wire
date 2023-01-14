/*
 * Copyright 2023 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation

public protocol SequenceInitializableCollection: Collection {
    init<S: Sequence>(_ contentsOf: S) where S.Element == Element
}

extension Array : SequenceInitializableCollection {}
extension Set : SequenceInitializableCollection {}

/// Converts an array of values to/from their string equivalent when serializing with Codable.
@propertyWrapper
public struct StringEncodedValues<ValuesHolder>
where ValuesHolder : SequenceInitializableCollection,
      ValuesHolder.Element : StringCodable
{
    public typealias Value = ValuesHolder.Element

    public var wrappedValue: ValuesHolder

    public init(wrappedValue: ValuesHolder) {
        self.wrappedValue = wrappedValue
    }
}

extension StringEncodedValues : Codable {
    public init(from decoder: Decoder) throws {
        var container = try decoder.unkeyedContainer()

        var results: [Value] = []
        if let count = container.count {
            results.reserveCapacity(count)
        }

        while !container.isAtEnd {
            let value = try container.decode(StringEncoded<Value>.self).wrappedValue
            results.append(value)
        }

        self.init(wrappedValue: ValuesHolder(results))
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.unkeyedContainer()

        for value in wrappedValue {
            let wrapped = StringEncoded(wrappedValue: value)
            try container.encode(wrapped)
        }
    }
}

extension StringEncodedValues : EmptyInitializable where ValuesHolder : EmptyInitializable {
    public init() {
        self.init(wrappedValue: .init())
    }
}

extension StringEncodedValues : Equatable where ValuesHolder : Equatable {
}

extension StringEncodedValues : Hashable where ValuesHolder : Hashable {
}

#if swift(>=5.5)
extension StringEncodedValues : Sendable where ValuesHolder : Sendable {
}
#endif
