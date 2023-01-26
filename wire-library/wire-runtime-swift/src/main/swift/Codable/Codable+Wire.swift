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

// MARK: - decode(stringEncoded:,forKey:)

extension KeyedDecodingContainer {
    public func decode<T: StringDecodable & Decodable>(
        stringEncoded type: T.Type,
        forKey key: Key
    ) throws -> T {
        try decode(StringEncoded<T>.self, forKey: key).wrappedValue
    }

    public func decodeIfPresent<T: StringDecodable & Decodable>(
        stringEncoded type: T.Type,
        forKey key: Key
    ) throws -> T? {
        try decodeIfPresent(StringEncoded<T>.self, forKey: key)?.wrappedValue
    }

    public func decode<T : StringDecodable & Decodable>(
        stringEncoded type: T.Type,
        firstOfKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> T {
        try decode(StringEncoded<T>.self, firstOfKeys: firstKey, secondKey).wrappedValue
    }

    public func decodeIfPresent<T : StringDecodable & Decodable>(
        stringEncoded type: T.Type,
        firstOfKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> T? {
        try decodeIfPresent(StringEncoded<T>.self, firstOfKeys: firstKey, secondKey)?.wrappedValue
    }
}

// MARK: - decodeProtoArray()

extension KeyedDecodingContainer {
    public func decodeProtoArray<T : Decodable>(
        _ type: T.Type,
        forKey key: Key
    ) throws -> [T] {
        try decodeIfPresent(Array<T>.self, forKey: key) ?? []
    }

    public func decodeProtoArray<T : Decodable>(
        _ type: T.Type,
        firstOfKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> [T] {
        try decodeIfPresent(Array<T>.self, firstOfKeys: firstKey, secondKey) ?? []
    }

    public func decodeProtoArray<T : StringDecodable & Decodable>(
        stringEncoded type: T.Type,
        forKey key: Key
    ) throws -> [T] {
        try decodeIfPresent(StringEncodedValues<Array<T>>.self, forKey: key)?.wrappedValue ?? []
    }

    public func decodeProtoArray<T : StringDecodable & Decodable>(
        stringEncoded type: T.Type,
        firstOfKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> [T] {
        try decodeIfPresent(StringEncodedValues<Array<T>>.self, firstOfKeys: firstKey, secondKey)?.wrappedValue ?? []
    }
}
