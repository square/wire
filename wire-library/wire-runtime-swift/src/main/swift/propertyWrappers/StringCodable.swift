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

/// Protocol that enables Codable support via the @StringEncoded property wrapper
public typealias StringCodable = StringEncodable & StringDecodable

/// Protocol that enables Encodable support via the @StringEncoded property wrapper
public protocol StringEncodable {
    /// Serialize an encoded value via `String`
    func stringEncodedValue(in encoder: Encoder) throws -> String
}

/// Protocol that enables Decodable support via the @StringEncoded property wrapper
public protocol StringDecodable {
    /// Losslessly deserialize a value derived from `encodedValue`
    init(encodedValue: String, from decoder: Decoder) throws
}

extension Int64: StringCodable {}
extension UInt64: StringCodable {}

// MARK: - LosslessStringConvertible

extension StringEncodable where Self : LosslessStringConvertible {
    public func stringEncodedValue(in encoder: Encoder) throws -> String {
        return description
    }
}

extension StringDecodable where Self : LosslessStringConvertible {
    public init(encodedValue: String, from decoder: Decoder) throws {
        guard let value = Self.init(encodedValue) else {
            throw ProtoDecoder.Error.unparsableString(type: Self.self, value: encodedValue)
        }
        self = value
    }
}

// MARK: - OptionalStringCodable

/// Protocol that enables Codable support via the @StringEncoded property wrapper
public typealias OptionalStringCodable = OptionalStringEncodable & OptionalStringDecodable

/// Protocol that enables Codable support via the @StringEncoded property wrapper
public protocol OptionalStringEncodable : StringEncodable {
    /// Enccoded value for nil data
    func shouldEncodeNil() -> Bool
}

/// Protocol that enables Codable support via the @StringEncoded property wrapper
public protocol OptionalStringDecodable : StringDecodable {
    /// Decoded value for nil data
    static func valueForNil() throws -> Self
}

enum StringEncodedError : Error {
    case encodingNil
    case decodingNil
}

extension Optional : StringEncodable, OptionalStringEncodable where Wrapped : StringEncodable {
    public func stringEncodedValue(in encoder: Encoder) throws -> String {
        guard let value = self else {
            throw StringEncodedError.encodingNil
        }
        return try value.stringEncodedValue(in: encoder)
    }

    public func shouldEncodeNil() -> Bool {
        return self == nil
    }
}

extension Optional : StringDecodable, OptionalStringDecodable where Wrapped : StringDecodable {
    public init(encodedValue: String, from decoder: Decoder) throws {
        self = try Wrapped(encodedValue: encodedValue, from: decoder)
    }

    public static func valueForNil() throws -> Self {
        return nil
    }
}
