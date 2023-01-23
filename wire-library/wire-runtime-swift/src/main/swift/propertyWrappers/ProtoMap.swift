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

private struct StringCodingKey: CodingKey {
    var stringValue: String
    var intValue: Int?

    init(stringValue: String) {
        self.stringValue = stringValue
    }

    init?(intValue: Int) {
        self.stringValue = intValue.description
        self.intValue = intValue
    }
}

@propertyWrapper
public struct ProtoMap<Key : Hashable & LosslessStringConvertible, Value> {
    public var wrappedValue: [Key: Value]

    public init(wrappedValue: [Key: Value]) {
        self.wrappedValue = wrappedValue
    }
}

extension ProtoMap : Encodable where Value : Encodable {
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringCodingKey.self)
        for kvp in wrappedValue {
            let codingKey = StringCodingKey(stringValue: kvp.key.description)
            try container.encode(kvp.value, forKey: codingKey)
        }
    }
}

extension ProtoMap : Decodable where Value : Decodable {
    public init(from decoder: Decoder) throws {
        wrappedValue = [:]

        let container = try decoder.container(keyedBy: StringCodingKey.self)
        for codingKey in container.allKeys {
            guard let key = Key(codingKey.stringValue) else {
                throw ProtoDecoder.Error.unparsableString(type: Key.self, value: codingKey.stringValue)
            }

            wrappedValue[key] = try container.decode(Value.self, forKey: codingKey)
        }
    }
}

extension ProtoMap : Equatable where Value : Equatable {
}

extension ProtoMap : Hashable where Value : Hashable {
}

extension ProtoMap : EmptyInitializable {
    public init() {
        self.init(wrappedValue: [:])
    }
}

#if swift(>=5.5)
extension ProtoMap : Sendable where Key : Sendable, Value : Sendable {
}
#endif

// MARK: - ProtoMapEnumValues

@propertyWrapper
public struct ProtoMapEnumValues<
    Key : Hashable & LosslessStringConvertible,
    Value : ProtoEnum
> {
    public var wrappedValue: [Key: Value]

    public init(wrappedValue: [Key: Value]) {
        self.wrappedValue = wrappedValue
    }
}

extension ProtoMapEnumValues : Encodable {
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringCodingKey.self)
        for kvp in wrappedValue {
            let codingKey = StringCodingKey(stringValue: kvp.key.description)
            try container.encode(kvp.value, forKey: codingKey)
        }
    }
}

extension ProtoMapEnumValues : Decodable {
    public init(from decoder: Decoder) throws {
        wrappedValue = [:]

        let container = try decoder.container(keyedBy: StringCodingKey.self)
        for codingKey in container.allKeys {
            guard let key = Key(codingKey.stringValue) else {
                throw ProtoDecoder.Error.unparsableString(type: Key.self, value: codingKey.stringValue)
            }

            wrappedValue[key] = try container.decode(BoxedEnum<Value>.self, forKey: codingKey).value
        }
    }
}

extension ProtoMapEnumValues : Equatable where Value : Equatable {
}

extension ProtoMapEnumValues : Hashable where Value : Hashable {
}

extension ProtoMapEnumValues : EmptyInitializable {
    public init() {
        self.init(wrappedValue: [:])
    }
}

#if swift(>=5.5)
extension ProtoMapEnumValues : Sendable where Key : Sendable, Value : Sendable {
}
#endif

// MARK: - ProtoMapStringEncodedValues

@propertyWrapper
public struct ProtoMapStringEncodedValues<
    Key : Hashable & LosslessStringConvertible,
    Value : StringCodable & Codable
> {
    public var wrappedValue: [Key: Value]

    public init(wrappedValue: [Key: Value]) {
        self.wrappedValue = wrappedValue
    }
}

extension ProtoMapStringEncodedValues : Encodable {
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringCodingKey.self)
        for kvp in wrappedValue {
            let codingKey = StringCodingKey(stringValue: kvp.key.description)
            let encodedValue = StringEncoded(wrappedValue: kvp.value)
            try container.encode(encodedValue, forKey: codingKey)
        }
    }
}

extension ProtoMapStringEncodedValues : Decodable {
    public init(from decoder: Decoder) throws {
        wrappedValue = [:]

        let container = try decoder.container(keyedBy: StringCodingKey.self)
        for codingKey in container.allKeys {
            guard let key = Key(codingKey.stringValue) else {
                throw ProtoDecoder.Error.unparsableString(type: Key.self, value: codingKey.stringValue)
            }
            let encodedValue = try container.decode(StringEncoded<Value>.self, forKey: codingKey)
            wrappedValue[key] = encodedValue.wrappedValue
        }
    }
}

extension ProtoMapStringEncodedValues : Equatable where Value : Equatable {
}

extension ProtoMapStringEncodedValues : Hashable where Value : Hashable {
}

extension ProtoMapStringEncodedValues : EmptyInitializable {
    public init() {
        self.init(wrappedValue: [:])
    }
}

#if swift(>=5.5)
extension ProtoMapStringEncodedValues : Sendable where Key : Sendable, Value : Sendable {
}
#endif
