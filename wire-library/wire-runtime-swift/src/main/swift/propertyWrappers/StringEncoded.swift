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

/// Converts values to/from their string equivalent when serializing with Codable.
@propertyWrapper
public struct StringEncoded<Value : StringCodable & Codable> {
    public var wrappedValue: Value

    public init(wrappedValue: Value) {
        self.wrappedValue = wrappedValue
    }
}

extension StringEncoded : Decodable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        guard !container.decodeNil() else {
            let value = try Self.create(optionalEncodedValue: nil)
            self.init(wrappedValue: value)
            return
        }

        if let stringValue = try? container.decode(String.self) {
            let value = try Self.create(optionalEncodedValue: stringValue)
            self.init(wrappedValue: value)
            return
        }

        let value = try container.decode(Value.self)
        self.init(wrappedValue: value)
    }

    private static func create(optionalEncodedValue: String?) throws -> Value {
        guard let encodedValue = optionalEncodedValue else {
            return try valueForNil()
        }
        return try Value(encodedValue: encodedValue)
    }

    private static func valueForNil() throws -> Value {
        if let optionalClass = Value.self as? OptionalStringDecodable.Type {
            let value = try optionalClass.valueForNil()
            return value as! Value
        }
        throw ProtoDecoder.Error.unparsableString(type: Value.self, value: nil)
    }
}

extension StringEncoded : Encodable {
    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        if shouldEncodeNil() {
            try container.encodeNil()
        } else {
            switch encoder.stringEncodedEncodingStrategy {
            case .raw:
                try container.encode(wrappedValue)

            case .string:
                try container.encode(wrappedValue.stringEncodedValue())
            }
        }
    }

    private func shouldEncodeNil() -> Bool {
        if let instance = wrappedValue as? OptionalStringEncodable {
            return instance.shouldEncodeNil()
        } else {
            return false
        }
    }
}

extension StringEncoded : Equatable where Value : Equatable {
}

extension StringEncoded : Hashable where Value : Hashable {
}

#if swift(>=5.5)
extension StringEncoded : Sendable where Value : Sendable {
}
#endif

public extension KeyedDecodingContainer {
    func decode<T: OptionalStringCodable>(
        _: StringEncoded<T>.Type,
        forKey key: Key
    ) throws -> StringEncoded<T> {
        if let value = try decodeIfPresent(StringEncoded<T>.self, forKey: key) {
            return value
        } else {
            return try StringEncoded(wrappedValue: T.valueForNil())
        }
    }
}
