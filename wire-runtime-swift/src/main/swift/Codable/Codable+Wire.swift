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

// MARK: - Nesting depth

extension Decoder {
    /**
     Returns the keyed container that generated `Codable` initializers decode a message from.

     This overload is more specific than `Decoder.container(keyedBy:)`, so generated code calls it.
     It throws a `DecodingError.dataCorrupted` when the message is nested `ProtoReader.recursionLimit` levels deep, the same limit as the binary decoder.
     The `underlyingError` of its context is a `MessageNestingDepthError`.
     Each level is a field name in the coding path, so singular, oneof, repeated, and map fields all count the same.
     Array indexes and map keys do not count.
     The limit reduces the risk of a stack overflow, but it cannot remove it: the stack that a level needs grows with the number of fields in the message.
     */
    public func container(
        keyedBy type: StringLiteralCodingKeys.Type
    ) throws -> KeyedDecodingContainer<StringLiteralCodingKeys> {
        // Array indexes have an `intValue`, and `ProtoMap` gives its entry keys one.
        let depth = codingPath.reduce(0) { $0 + ($1.intValue == nil ? 1 : 0) }
        if depth >= ProtoReader.recursionLimit {
            throw DecodingError.dataCorrupted(
                DecodingError.Context(
                    codingPath: codingPath,
                    debugDescription: "Exceeded the maximum nesting depth of \(ProtoReader.recursionLimit) for a message",
                    underlyingError: MessageNestingDepthError()
                )
            )
        }
        return try keyedContainer(of: self, keyedBy: type)
    }
}

/**
 The `underlyingError` of the `DecodingError` that JSON decoding throws for a message that is nested too deeply.
 Match on it instead of on the description of the error.
 */
public struct MessageNestingDepthError: Error {
    public init() {}
}

/** Calls the generic `Decoder.container(keyedBy:)`, which the overload above would otherwise hide. */
internal func keyedContainer<Key: CodingKey>(
    of decoder: Decoder,
    keyedBy type: Key.Type
) throws -> KeyedDecodingContainer<Key> {
    return try decoder.container(keyedBy: type)
}

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

// MARK: - OneOfDecoder

/**
 Decodes the members of one `oneof` from a JSON object.

 Generated `Codable` initializers create one decoder for each `oneof`.
 They call `decode(_:from:as:forKeys:)` once for each member, in declaration order.
 A member that is missing or `null` is skipped.
 If two members are present and not `null`, the second call throws a `DecodingError.dataCorrupted`.
 The message of the error names the `oneof` and both JSON keys.
 This is the same rule as the protobuf runtimes from Google.

 A member can have two keys: its camelCase JSON name and its proto name.
 The first key that is not `null` sets the member, and the later keys of that member are not read.
 A JSON object that has both keys of one member is not an error.
 */
public struct OneOfDecoder<Value> {
    private let name: String
    private var selectedKey: String?

    /// The member that the JSON object sets, or `nil` if it sets none.
    public private(set) var value: Value?

    /// - Parameter name: The proto name of the `oneof`.
    public init(name: String) {
        self.name = name
    }

    public mutating func decode<Key : CodingKey, T : Decodable>(
        _ type: T.Type,
        from container: KeyedDecodingContainer<Key>,
        as makeValue: (T) -> Value,
        forKeys keys: Key...
    ) throws {
        for key in keys {
            if let member = try container.decodeIfPresent(type, forKey: key) {
                try select(makeValue(member), key: key, in: container)
                return
            }
        }
    }

    /// Decodes an enum with `decodeIfPresent(_:forKey:)` for `ProtoEnum`, which reads the decoding strategy.
    public mutating func decode<Key : CodingKey, T : ProtoEnum>(
        _ type: T.Type,
        from container: KeyedDecodingContainer<Key>,
        as makeValue: (T) -> Value,
        forKeys keys: Key...
    ) throws {
        for key in keys {
            if let member = try container.decodeIfPresent(type, forKey: key) {
                try select(makeValue(member), key: key, in: container)
                return
            }
        }
    }

    private mutating func select<Key : CodingKey>(
        _ member: Value,
        key: Key,
        in container: KeyedDecodingContainer<Key>
    ) throws {
        if let selectedKey = selectedKey {
            throw DecodingError.dataCorrupted(
                DecodingError.Context(
                    codingPath: container.codingPath + [key],
                    debugDescription: "Cannot set the oneof '\(name)' with the key '\(key.stringValue)'. The key '\(selectedKey)' already set it."
                )
            )
        }
        value = member
        selectedKey = key.stringValue
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
