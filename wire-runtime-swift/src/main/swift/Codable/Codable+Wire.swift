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

// MARK: decode(_:,firstOfKeys:)

extension KeyedDecodingContainer {
    public func decodeIfPresent<T>(
        _ type: T.Type,
        firstOfKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> T? where T : Decodable {
        return try decodeIfPresent(type, forKey: firstKey) ?? decodeIfPresent(type, forKey: secondKey)
    }

    public func decode<T>(
        _ type: T.Type,
        firstOfKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> T where T : Decodable {
        guard let value = try decodeIfPresent(type, firstOfKeys: firstKey, secondKey) else {
            throw DecodingError.keyNotFound(
                firstKey,
                DecodingError.Context(
                    codingPath: codingPath,
                    debugDescription: "decode(_:,firstOfKeys:) could not find a valid key"
                )
            )
        }
        return value
    }
}

// MARK: - ProtoEnum

extension KeyedDecodingContainer {
    public func decodeIfPresent<T : ProtoEnum>(
        _ type: T.Type,
        forKey key: Key
    ) throws -> T? {
        guard contains(key) else {
            return nil
        }

        let box = try decode(BoxedEnum<T>.self, forKey: key)
        return box.value
    }

    public func decode<T : ProtoEnum>(
        _ type: Array<T>.Type,
        forKey key: Key
    ) throws -> [T] {
        return try decodeProtoArray(T.self, forKey: key)
    }

    public func decode<T : ProtoEnum>(
        _ type: Set<T>.Type,
        forKey key: Key
    ) throws -> Set<T> {
        return Set(try decodeProtoArray(T.self, forKey: key))
    }

    internal func decode<T : ProtoEnum>(safeEnum: T.Type, forKey key: Key) throws -> T? {
        return try decode(BoxedEnum<T>.self, forKey: key).value
    }
}

extension UnkeyedDecodingContainer {
    internal mutating func decode<T : ProtoEnum>(safeEnum: T.Type) throws -> T? {
        return try decode(BoxedEnum<T>.self).value
    }
}

// MARK: - decode(stringEncoded:,forKey:)

extension KeyedDecodingContainer {
    public func decode<T : StringDecodable>(
        stringEncoded type: T.Type,
        forKey key: Key
    ) throws -> T {
        try decode(StringEncoded<T>.self, forKey: key).wrappedValue
    }

    public func decodeIfPresent<T : StringDecodable>(
        stringEncoded type: T.Type,
        forKey key: Key
    ) throws -> T? {
        try decodeIfPresent(StringEncoded<T>.self, forKey: key)?.wrappedValue
    }

    public func decode<T : StringDecodable>(
        stringEncoded type: T.Type,
        firstOfKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> T {
        try decode(StringEncoded<T>.self, firstOfKeys: firstKey, secondKey).wrappedValue
    }

    public func decodeIfPresent<T : StringDecodable>(
        stringEncoded type: T.Type,
        firstOfKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> T? {
        try decodeIfPresent(StringEncoded<T>.self, firstOfKeys: firstKey, secondKey)?.wrappedValue
    }
}

extension UnkeyedDecodingContainer {
    public mutating func decode<T : StringDecodable>(
        stringEncoded value: T.Type
    ) throws -> T {
        try decode(StringEncoded<T>.self).wrappedValue
    }
}

// MARK: - encode(stringEncoded:,forKey:)

extension KeyedEncodingContainer {
    public mutating func encode<T : StringEncodable>(
        stringEncoded value: T,
        forKey key: KeyedEncodingContainer<K>.Key
    ) throws {
        try encode(StringEncoded(wrappedValue: value), forKey: key)
    }

    public mutating func encode<T : StringEncodable>(
        stringEncoded value: T?,
        forKey key: KeyedEncodingContainer<K>.Key
    ) throws {
        guard let value = value else {
            try encodeNil(forKey: key)
            return
        }
        try encode(stringEncoded: value, forKey: key)
    }

    public mutating func encodeIfPresent<T : StringEncodable>(
        stringEncoded value: T?,
        forKey key: KeyedEncodingContainer<K>.Key
    ) throws {
        guard let value = value else {
            return
        }
        try encode(stringEncoded: value, forKey: key)
    }
}

extension UnkeyedEncodingContainer {
    public mutating func encode<T : StringEncodable>(
        stringEncoded value: T
    ) throws {
        try encode(StringEncoded(wrappedValue: value))
    }
}

// MARK: - decodeProtoArray()

extension KeyedDecodingContainer {
    public func decodeProtoArray<T : Decodable>(
        _ type: T.Type,
        forKey key: Key
    ) throws -> [T] {
        try decodeIfPresent(ProtoArray<T>.self, forKey: key)?.wrappedValue ?? []
    }

    public func decodeProtoArray<T : Decodable>(
        _ type: T.Type,
        firstOfKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> [T] {
        try decodeIfPresent(ProtoArray<T>.self, firstOfKeys: firstKey, secondKey)?.wrappedValue ?? []
    }
}

// MARK: - encodeProtoArray()

extension KeyedEncodingContainer {
    public mutating func encodeProtoArray<T : Encodable>(
        _ value: [T],
        forKey key: KeyedEncodingContainer<K>.Key
    ) throws {
        try encode(ProtoArray(wrappedValue: value), forKey: key)
    }
}

// MARK: - decodeProtoMap()

extension KeyedDecodingContainer {
    public func decodeProtoMap<MapKey : Hashable & LosslessStringConvertible, MapValue : Decodable>(
        _ type: [MapKey : MapValue].Type,
        forKey key: Key
    ) throws -> [MapKey : MapValue] {
        try decodeIfPresent(ProtoMap<MapKey, MapValue>.self, forKey: key)?.wrappedValue ?? [:]
    }

    public func decodeProtoMap<MapKey : Hashable & LosslessStringConvertible, MapValue : Decodable>(
        _ type: [MapKey : MapValue].Type,
        firstOfKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> [MapKey : MapValue] {
        try decodeIfPresent(ProtoMap<MapKey, MapValue>.self, firstOfKeys: firstKey, secondKey)?.wrappedValue ?? [:]
    }
}

// MARK: - encodeProtoMap()

extension KeyedEncodingContainer {
    public mutating func encodeProtoMap<MapKey : Hashable & LosslessStringConvertible, MapValue : Encodable>(
        _ value: [MapKey : MapValue],
        forKey key: KeyedEncodingContainer<K>.Key
    ) throws {
        try encode(ProtoMap(wrappedValue: value), forKey: key)
    }
}
