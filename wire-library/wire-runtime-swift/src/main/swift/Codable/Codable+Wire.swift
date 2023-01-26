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

// MARK: decodeFirst()

extension KeyedDecodingContainer {
    public func decodeFirstIfPresent<T>(
        _ type: T.Type,
        forKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> T? where T : Decodable {
        return try decodeIfPresent(type, forKey: firstKey) ?? decodeIfPresent(type, forKey: secondKey)
    }

    public func decodeFirst<T>(
        _ type: T.Type,
        forKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> T where T : Decodable {
        guard let value = try decodeFirstIfPresent(type, forKeys: firstKey, secondKey) else {
            throw DecodingError.keyNotFound(
                firstKey,
                DecodingError.Context(
                    codingPath: codingPath,
                    debugDescription: "decodeFirst() could not find a valid key"
                )
            )
        }
        return value
    }
}

// MARK: - decodeStringEncoded()

extension KeyedDecodingContainer {
    public func decodeStringEncoded<T: StringDecodable & Decodable>(
        _ type: T.Type,
        forKey key: Key
    ) throws -> T {
        try decode(StringEncoded<T>.self, forKey: key).wrappedValue
    }

    public func decodeStringEncodedIfPresent<T: StringDecodable & Decodable>(
        _ type: T.Type,
        forKey key: Key
    ) throws -> T? {
        try decodeIfPresent(StringEncoded<T>.self, forKey: key)?.wrappedValue
    }

    public func decodeFirstStringEncoded<T : StringDecodable & Decodable>(
        _ type: T.Type,
        forKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> T {
        try decodeFirst(StringEncoded<T>.self, forKeys: firstKey, secondKey).wrappedValue
    }

    public func decodeFirstStringEncodedIfPresent<T : StringDecodable & Decodable>(
        _ type: T.Type,
        forKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> T? {
        try decodeFirstIfPresent(StringEncoded<T>.self, forKeys: firstKey, secondKey)?.wrappedValue
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

    public func decodeFirstProtoArray<T : Decodable>(
        _ type: T.Type,
        forKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> [T] {
        try decodeFirstIfPresent(Array<T>.self, forKeys: firstKey, secondKey) ?? []
    }

    public func decodeProtoArrayStringEncoded<T : StringDecodable & Decodable>(
        _ type: T.Type,
        forKey key: Key
    ) throws -> [T] {
        try decodeIfPresent(StringEncodedValues<Array<T>>.self, forKey: key)?.wrappedValue ?? []
    }

    public func decodeFirstProtoArrayStringEncoded<T : StringDecodable & Decodable>(
        _ type: T.Type,
        forKeys firstKey: Key,
        _ secondKey: Key
    ) throws -> [T] {
        try decodeFirstIfPresent(StringEncodedValues<Array<T>>.self, forKeys: firstKey, secondKey)?.wrappedValue ?? []
    }
}
