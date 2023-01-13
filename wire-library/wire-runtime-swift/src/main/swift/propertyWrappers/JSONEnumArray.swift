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

/**
 Converts enums to/from their string equivalent when serializing to/from JSON.
 This matches the Proto3 JSON spec: https://developers.google.com/protocol-buffers/docs/proto3#json
 */
@propertyWrapper
public struct JSONEnumArray<T : CaseIterable & Hashable & RawRepresentable> : Codable, Hashable where T.RawValue == UInt32 {
    public var wrappedValue: [T]

    public init(wrappedValue: [T]) {
        self.wrappedValue = wrappedValue
    }

    public init(from decoder: Decoder) throws {
        var container = try decoder.unkeyedContainer()

        wrappedValue = []
        if let count = container.count {
            wrappedValue.reserveCapacity(count)
        }

        while !container.isAtEnd {
            let value = try container.decode(JSONOptionalEnum<T>.self)
            guard let value = value.wrappedValue else {
                continue
            }
            wrappedValue.append(value)
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.unkeyedContainer()
        try container.encode(
            contentsOf: wrappedValue.lazy.map(JSONEnum.init(wrappedValue:))
        )
    }
}

extension JSONEnumArray : Sendable where T : Sendable {
}

public extension KeyedDecodingContainer {
    func decode<T: CaseIterable & Hashable & RawRepresentable>(
        _: JSONEnumArray<T>.Type,
        forKey key: Key
    ) throws -> JSONEnumArray<T> {
        if let value = try decodeIfPresent(JSONEnumArray<T>.self, forKey: key) {
            return value
        } else {
            return JSONEnumArray(wrappedValue: [])
        }
    }
}
