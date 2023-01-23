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

extension JSONDecoder {
    /// The Decoding strategy to use for ProtoEnum types
    /// Defaults to .throwError
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
    /// Defaults to .string
    public enum EnumEncodingStrategy {
        /// Encodes the name of the case as the value, like `"myEnum": "FOO"`
        case string
        /// Encodes the field value of the case as the value, like `"myEnum": 3`
        case integer
    }

    /// The encoding strategy to use for StringEncoded types that are themselves Encodable
    /// Defaults to .string
    /// - Note: ProtoMap Dictionary keys are always encoded as strings
    public enum StringEncodedEncodingStrategy {
        /// Encodes the string-encoded value, like `"myValue": "1"`
        case string
        /// Encodes the raw Encodable value, like `"myValue": 1`
        case raw
    }

    /// The encoding strategy to use for key names
    /// Defaults to .camelCase
    public enum KeyNameEncodingStrategy {
        // Convert key names to `camelCase`
        case camelCase
        // Maintain the original field names, typically `snake_case`
        case fieldName
    }

    /// The encoding strategy to use for optional values and collections
    /// Defaults to .skip
    public enum DefaultValuesEncodingStrategy {
        // Skip "default" values
        case skip
        // Emit values
        case emit
    }

}

public extension CodingUserInfoKey {
    /// Control the encoding of ProtoEnum types
    ///
    /// You probably will want to just set `JSONEncoder.enumEncodingStrategy`
    /// - SeeAlso: JSONEncoder.EnumEncodingStrategy
    static let wireEnumEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.EnumEncodingStrategy")!

    /// Control the decoding of ProtoEnum types
    ///
    /// You probably will want to just set `JSONDecoder.enumEncodingStrategy`
    /// - Note: Non-optional values will _always_ throw
    /// - SeeAlso: JSONDecoder.EnumDecodingStrategy
    static let wireEnumDecodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.EnumDecodingStrategy")!

    /// Control the encoding of StringEncoded values that are themselves Encodable
    /// - SeeAlso: JSONEncoder.StringEncodedEncodingStrategy
    static let wireStringEncodedEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.StringEncodedEncodingStrategy")!

    /// Control the encoding of proto key names
    /// - SeeAlso: JSONEncoder.KeyNameEncodingStrategy
    static let wireKeyNameEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.KeyNameEncodingStrategy")!

    /// Control the encoding of "default" values
    /// - SeeAlso: JSONEncoder.DefaultValuesEncodingStrategy
    static let wireDefaultValuesEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.DefaultValuesEncodingStrategy")!
}

public extension Encoder {
    var protoEnumEncodingStrategy: JSONEncoder.EnumEncodingStrategy {
        let preferred = userInfo[.wireEnumEncodingStrategy] as? JSONEncoder.EnumEncodingStrategy
        return preferred ?? .string
    }

    var stringEncodedEnccodingStrategy: JSONEncoder.StringEncodedEncodingStrategy {
        let preferred = userInfo[.wireStringEncodedEncodingStrategy] as? JSONEncoder.StringEncodedEncodingStrategy
        return preferred ?? .string
    }

    var protoKeyNameEncodingStrategy: JSONEncoder.KeyNameEncodingStrategy {
        let preferred = userInfo[.wireKeyNameEncodingStrategy] as? JSONEncoder.KeyNameEncodingStrategy
        return preferred ?? .camelCase
    }

    var protoDefaultValuesEncodingStrategy: JSONEncoder.DefaultValuesEncodingStrategy {
        let preferred = userInfo[.wireDefaultValuesEncodingStrategy] as? JSONEncoder.DefaultValuesEncodingStrategy
        return preferred ?? .skip
    }
}

public extension Decoder {
    var protoEnumDecodingStrategy: JSONDecoder.EnumDecodingStrategy {
        let preferred = userInfo[.wireEnumDecodingStrategy] as? JSONDecoder.EnumDecodingStrategy
        return preferred ?? .throwError
    }
}

// MARK: - JSON Coders

public extension JSONEncoder {
    var protoEnumEncodingStrategy: JSONEncoder.EnumEncodingStrategy {
        get {
            let preferred = userInfo[.wireEnumEncodingStrategy] as? JSONEncoder.EnumEncodingStrategy
            return preferred ?? .string
        }
        set {
            userInfo[.wireEnumEncodingStrategy] = newValue
        }
    }

    var stringEncodedEncodingStrategy: JSONEncoder.StringEncodedEncodingStrategy {
        get {
            let preferred = userInfo[.wireStringEncodedEncodingStrategy] as? JSONEncoder.StringEncodedEncodingStrategy
            return preferred ?? .string
        }
        set {
            userInfo[.wireStringEncodedEncodingStrategy] = newValue
        }
    }

    var protoKeyNameEncodingStrategy: JSONEncoder.KeyNameEncodingStrategy {
        get {
            let preferred = userInfo[.wireKeyNameEncodingStrategy] as? JSONEncoder.KeyNameEncodingStrategy
            return preferred ?? .camelCase
        }
        set {
            userInfo[.wireKeyNameEncodingStrategy] = newValue
        }
    }

    var protoDefaultValuesEncodingStrategy: JSONEncoder.DefaultValuesEncodingStrategy {
        get {
            let preferred = userInfo[.wireDefaultValuesEncodingStrategy] as? JSONEncoder.DefaultValuesEncodingStrategy
            return preferred ?? .skip
        }
        set {
            userInfo[.wireDefaultValuesEncodingStrategy] = newValue
        }
    }
}

public extension JSONDecoder {
    var protoEnumDecodingStrategy: JSONDecoder.EnumDecodingStrategy {
        get {
            let preferred = userInfo[.wireEnumDecodingStrategy] as? JSONDecoder.EnumDecodingStrategy
            return preferred ?? .throwError
        }
        set {
            userInfo[.wireEnumDecodingStrategy] = newValue
        }
    }
}
