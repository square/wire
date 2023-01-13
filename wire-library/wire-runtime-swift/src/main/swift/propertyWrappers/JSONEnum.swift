/*
 * Copyright 2021 Square Inc.
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

/// The encoding strategy to use for JSONEnum types'
/// Defaults to .string
public enum EnumEncodingStrategy {
    case string
    case integer
}

extension CodingUserInfoKey {
    /// Control the encoding of Enums
    /// - SeeAlso: EnumEncodingStrategy
    public static let wireEnumEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.EnumEncoding")!
}

private extension Encoder {
    var enumEncodingStrategy: EnumEncodingStrategy {
        let preferred = userInfo[.wireEnumEncodingStrategy] as? EnumEncodingStrategy
        return preferred ?? .string
    }
}

/**
 Converts enums to/from their string equivalent when serializing to/from JSON.
 This matches the Proto3 JSON spec: https://developers.google.com/protocol-buffers/docs/proto3#json
 */
@propertyWrapper
public struct JSONEnum<T : CaseIterable & Hashable & RawRepresentable> : Codable, Hashable where T.RawValue == UInt32 {
    public var wrappedValue: T

    public init(wrappedValue: T) {
        self.wrappedValue = wrappedValue
    }

    public init(from decoder: Decoder) throws {
        // We support decoding from either the string value or the field index.
        let container = try decoder.singleValueContainer()

        if let string = try? container.decode(String.self) {
            try self.init(string: string)
        } else {
            // If the value wasn't a string, then look for the field index instead.
            let fieldNumber = try container.decode(UInt32.self)
            try self.init(fieldNumber: fieldNumber)
        }
    }

    public func encode(to encoder: Encoder) throws {
        switch encoder.enumEncodingStrategy {
        case .string:
            try String(describing: wrappedValue).encode(to: encoder)

        case .integer:
            try wrappedValue.rawValue.encode(to: encoder)
        }
    }

    private init(string: String) throws {
        guard let value = T.allCases.first(where: { "\($0)" == string }) else {
            throw ProtoDecoder.Error.unknownEnumString(type: T.self, string: string)
        }
        self.wrappedValue = value
    }

    private init(fieldNumber: UInt32) throws {
        guard let value = T.init(rawValue: fieldNumber) else {
            throw ProtoDecoder.Error.unknownEnumCase(type: T.self, fieldNumber: fieldNumber)
        }
        self.wrappedValue = value
    }
}

extension JSONEnum : Sendable where T : Sendable {
}
