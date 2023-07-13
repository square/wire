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

extension JSONDecoder {
    /// The Decoding strategy to use for ProtoEnum types
    /// - Note: Defaults to .throwError
    /// - SeeAlso: [Proto3 JSON Mapping](https://developers.google.com/protocol-buffers/docs/proto3#json)
    public enum EnumDecodingStrategy {
        /// Throws an error when encountering unknown enum values in single-value fields or collections.
        case throwError
        /// Defaults the unknown enum value to nil for single-value fields.
        ///
        /// With this option, unknown values in collections are removed from the collection in which they originated where possible.
        /// - Note: This is "free" for `Array<Enum>` and `Set<Enum>`
        /// - Note: For dictionaries, it is necessary to wrap it in `@ProtoMapEnumValues`
        case returnNil
    }
}

extension JSONEncoder {
    /// The encoding strategy to use for ProtoEnum types
    /// - Note: Defaults to .string
    /// - SeeAlso: [Proto3 JSON Mapping](https://developers.google.com/protocol-buffers/docs/proto3#json)
    public enum EnumEncodingStrategy {
        /// Encodes the name of the case as the value, like `"myEnum": "FOO"`
        case string
        /// Encodes the field value of the case as the value, like `"myEnum": 3`
        case integer
    }

    /// The encoding strategy to use for key names in Codable implementations
    /// - Note: Defaults to .camelCase
    /// - SeeAlso: [Proto3 JSON Mapping](https://developers.google.com/protocol-buffers/docs/proto3#json)
    public enum KeyNameEncodingStrategy {
        // Convert key names to `camelCase`
        case camelCase
        // Maintain the original field names, typically `snake_case`
        case fieldName
    }

    /// The encoding strategy to use when a value is equivalent to its proto default
    /// - Note: Defaults to .skip
    /// - SeeAlso: [Proto3 JSON Mapping](https://developers.google.com/protocol-buffers/docs/proto3#json)
    public enum DefaultValuesEncodingStrategy {
        // Skip "default" values
        case skip
        // Include "default" values
        case include
    }
}

extension CodingUserInfoKey {
    /// Control the encoding of ProtoEnum types
    ///
    /// You probably will want to just set `JSONEncoder.enumEncodingStrategy`
    /// - SeeAlso: JSONEncoder.EnumEncodingStrategy
    public static let wireEnumEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.EnumEncodingStrategy")!

    /// Control the decoding of ProtoEnum types
    ///
    /// You probably will want to just set `JSONDecoder.enumEncodingStrategy`
    /// - Note: Non-optional values will _always_ throw
    /// - SeeAlso: JSONDecoder.EnumDecodingStrategy
    public static let wireEnumDecodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.EnumDecodingStrategy")!

    /// Control the encoding of proto key names
    ///
    /// You probably will want to just set `JSONEncoder.protoKeyNameEncodingStrategy`
    /// - SeeAlso: JSONEncoder.KeyNameEncodingStrategy
    public static let wireKeyNameEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.KeyNameEncodingStrategy")!

    /// Control the encoding of "default" values
    ///
    /// You probably will want to just set `JSONEncoder.protoDefaultValuesEncodingStrategy`
    /// - SeeAlso: JSONEncoder.DefaultValuesEncodingStrategy
    public static let wireDefaultValuesEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.DefaultValuesEncodingStrategy")!
}

extension Encoder {
    public var protoEnumEncodingStrategy: JSONEncoder.EnumEncodingStrategy {
        let preferred = userInfo[.wireEnumEncodingStrategy] as? JSONEncoder.EnumEncodingStrategy
        return preferred ?? .string
    }

    public var protoKeyNameEncodingStrategy: JSONEncoder.KeyNameEncodingStrategy {
        let preferred = userInfo[.wireKeyNameEncodingStrategy] as? JSONEncoder.KeyNameEncodingStrategy
        return preferred ?? .camelCase
    }

    public var protoDefaultValuesEncodingStrategy: JSONEncoder.DefaultValuesEncodingStrategy {
        let preferred = userInfo[.wireDefaultValuesEncodingStrategy] as? JSONEncoder.DefaultValuesEncodingStrategy
        return preferred ?? .skip
    }
}

extension Decoder {
    public var protoEnumDecodingStrategy: JSONDecoder.EnumDecodingStrategy {
        let preferred = userInfo[.wireEnumDecodingStrategy] as? JSONDecoder.EnumDecodingStrategy
        return preferred ?? .throwError
    }
}

// MARK: - JSON Coders

extension JSONEncoder {
    public var protoEnumEncodingStrategy: JSONEncoder.EnumEncodingStrategy {
        get {
            let preferred = userInfo[.wireEnumEncodingStrategy] as? JSONEncoder.EnumEncodingStrategy
            return preferred ?? .string
        }
        set {
            userInfo[.wireEnumEncodingStrategy] = newValue
        }
    }

    public var protoKeyNameEncodingStrategy: JSONEncoder.KeyNameEncodingStrategy {
        get {
            let preferred = userInfo[.wireKeyNameEncodingStrategy] as? JSONEncoder.KeyNameEncodingStrategy
            return preferred ?? .camelCase
        }
        set {
            userInfo[.wireKeyNameEncodingStrategy] = newValue
        }
    }

    public var protoDefaultValuesEncodingStrategy: JSONEncoder.DefaultValuesEncodingStrategy {
        get {
            let preferred = userInfo[.wireDefaultValuesEncodingStrategy] as? JSONEncoder.DefaultValuesEncodingStrategy
            return preferred ?? .skip
        }
        set {
            userInfo[.wireDefaultValuesEncodingStrategy] = newValue
        }
    }
}

extension JSONDecoder {
    public var protoEnumDecodingStrategy: JSONDecoder.EnumDecodingStrategy {
        get {
            let preferred = userInfo[.wireEnumDecodingStrategy] as? JSONDecoder.EnumDecodingStrategy
            return preferred ?? .throwError
        }
        set {
            userInfo[.wireEnumDecodingStrategy] = newValue
        }
    }
}

// MARK: - String-Friendly Coding Keys

public struct StringLiteralCodingKeys : CodingKey, ExpressibleByStringLiteral {
    public let stringValue: String
    public let intValue: Int?

    public init(stringValue: String) {
        self.stringValue = stringValue
        self.intValue = nil
    }

    public init?(intValue: Int) {
        self.stringValue = intValue.description
        self.intValue = intValue
    }

    public init(stringLiteral: String) {
        self.stringValue = stringLiteral
        self.intValue = nil
    }
}
