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

extension ProtoDecoder {
    /// The Decoding strategy to use for ProtoEnum types
    /// Defaults to .throwError
    public typealias CodableEnumDecodingStrategy = UnknownEnumValueDecodingStrategy

    /// The decoding strategy to use for StringEncoded types that are themselves Decodable
    /// Defaults to .disallowRawDecoding
    /// - Note: ProtoMap Dictionary keys are always decoded as strings
    public enum CodableStringEncodedDecodingStrategy {
        case disallowRawDecoding
        case allowRawDecoding
    }
}

extension ProtoEncoder {
    /// The encoding strategy to use for ProtoEnum types
    /// Defaults to .string
    public enum CodableEnumEncodingStrategy {
        case string
        case integer
    }

    /// The encoding strategy to use for StringEncoded types that are themselves Encodable
    /// Defaults to .string
    /// - Note: ProtoMap Dictionary keys are always encoded as strings
    public enum CodableStringEncodedEncodingStrategy {
        case string
        case raw
    }
}

extension CodingUserInfoKey {
    /// Control the encoding of ProtoEnum types
    /// - SeeAlso: ProtoEncoder.CodableEnumEncodingStrategy
    public static let wireEnumEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.EnumEncodingStrategy")!

    /// Control the decoding of ProtoEnum types
    /// - Note: Non-optional values will _always_ throw
    /// - SeeAlso: ProtoDecoder.CodableEnumDecodingStrategy
    public static let wireEnumDecodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.EnumDecodingStrategy")!

    /// Control the encoding of StringEncoded values that are themselves Encodable
    /// - SeeAlso: ProtoEncoder.CodableStringEncodedEncodingStrategy
    static let wireStringEncodedEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.StringEncodedEncodingStrategy")!

    /// Control the decoding of StringEncoded values that are themselves Decodable
    /// - SeeAlso: ProtoDecoder.CodableStringEncodedDecodingStrategy
    public static let wireStringEncodedDecodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.StringEncodedDecodingStrategy")!
}

extension Encoder {
    var enumEncodingStrategy: ProtoEncoder.CodableEnumEncodingStrategy {
        let preferred = userInfo[.wireEnumEncodingStrategy] as? ProtoEncoder.CodableEnumEncodingStrategy
        return preferred ?? .string
    }

    var stringEncodedEnccodingStrategy: ProtoEncoder.CodableStringEncodedEncodingStrategy {
        let preferred = userInfo[.wireStringEncodedEncodingStrategy] as? ProtoEncoder.CodableStringEncodedEncodingStrategy
        return preferred ?? .string
    }
}

extension Decoder {
    var enumDecodingStrategy: ProtoDecoder.CodableEnumDecodingStrategy {
        let preferred = userInfo[.wireEnumDecodingStrategy] as? ProtoDecoder.UnknownEnumValueDecodingStrategy
        return preferred ?? .throwError
    }

    var stringEncodedDecodingStrategy: ProtoDecoder.CodableStringEncodedDecodingStrategy {
        let preferred = userInfo[.wireStringEncodedDecodingStrategy] as? ProtoDecoder.CodableStringEncodedDecodingStrategy
        return preferred ?? .disallowRawDecoding
    }
}
