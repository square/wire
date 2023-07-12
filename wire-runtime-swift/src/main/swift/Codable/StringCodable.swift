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

/// Protocol that enables Codable support via the @StringEncoded property wrapper
public typealias StringCodable = StringEncodable & StringDecodable

/// Protocol that enables Encodable support via the @StringEncoded property wrapper
public protocol StringEncodable : Encodable {
    /// Serialize an encoded value via `String`
    func stringEncodedValue() throws -> String
}

/// Protocol that enables Decodable support via the @StringEncoded property wrapper
public protocol StringDecodable : Decodable {
    /// Losslessly deserialize a value derived from `encodedValue`
    init(encodedValue: String) throws
}

extension Int64: StringCodable {}
extension UInt64: StringCodable {}

// MARK: - LosslessStringConvertible

extension StringEncodable where Self : LosslessStringConvertible {
    public func stringEncodedValue() throws -> String {
        return description
    }
}

extension StringDecodable where Self : LosslessStringConvertible {
    public init(encodedValue: String) throws {
        guard let value = Self.init(encodedValue) else {
            throw ProtoDecoder.Error.unparsableString(type: Self.self, value: encodedValue)
        }
        self = value
    }
}
