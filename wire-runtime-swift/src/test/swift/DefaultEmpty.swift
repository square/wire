/*
 * Copyright (C) 2023 Square, Inc.
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

/// Allow creation of an instance with an empty `init()`
public protocol EmptyInitializable {
    init()
}

@propertyWrapper
public struct DefaultEmpty<T: EmptyInitializable> {
    public var wrappedValue: T

    public init(wrappedValue: T) {
        self.wrappedValue = wrappedValue
    }
}

extension DefaultEmpty : EmptyInitializable {
    public init() {
        self.init(wrappedValue: T())
    }
}

extension DefaultEmpty : Equatable where T : Equatable {
}

extension DefaultEmpty : Hashable where T : Hashable {
}

extension DefaultEmpty : Encodable where T : Encodable {
    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(wrappedValue)
    }
}

extension DefaultEmpty : Decodable where T : Decodable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        wrappedValue = try container.decode(T.self)
    }
}

#if swift(>=5.5)
extension DefaultEmpty : Sendable where T : Sendable {
}
#endif

public extension KeyedDecodingContainer {
    func decode<T: EmptyInitializable & Decodable>(
        _: DefaultEmpty<T>.Type,
        forKey key: Key
    ) throws -> DefaultEmpty<T> {
        if let value = try decodeIfPresent(DefaultEmpty<T>.self, forKey: key) {
            return value
        } else {
            return DefaultEmpty()
        }
    }
}

// MARK: - Implementations

extension Array : EmptyInitializable {
}

extension Dictionary : EmptyInitializable {
}

extension Foundation.Data : EmptyInitializable {
}

extension Int64 : EmptyInitializable {
    public init() {
        self = .zero
    }
}

extension UInt64 : EmptyInitializable {
    public init() {
        self = .zero
    }
}

extension StringEncoded : EmptyInitializable where Value : EmptyInitializable {
    public init() {
        self.init(wrappedValue: Value())
    }
}

extension ProtoArray : EmptyInitializable {
    public init() {
        self.init(wrappedValue: [])
    }
}

extension ProtoMap : EmptyInitializable {
    public init() {
        self.init(wrappedValue: [:])
    }
}
