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

extension ProtoEncoder {
    /// The encoding strategy to use for ProtoEnum types
    /// Defaults to .string
    public enum JSONEnumEncodingStrategy {
        case string
        case integer
    }
}

extension CodingUserInfoKey {
    /// Control the encoding of ProtoEnum types
    /// - SeeAlso: ProtoEncoder.JSONEnumEncodingStrategy
    public static let wireEnumEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.EnumEncodingStrategy")!

    /// Control the decoding of ProtoEnum types
    /// - Note: Non-optional values will _always_ throw
    /// - SeeAlso: ProtoDecoder.UnknownEnumValueDecodingStrategy
    public static let wireEnumDecodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.EnumDecodingStrategy")!
}

extension Encoder {
    var enumEncodingStrategy: ProtoEncoder.JSONEnumEncodingStrategy {
        let preferred = userInfo[.wireEnumEncodingStrategy] as? ProtoEncoder.JSONEnumEncodingStrategy
        return preferred ?? .string
    }
}

extension Decoder {
    var enumDecodingStrategy: ProtoDecoder.UnknownEnumValueDecodingStrategy {
        let prefered = userInfo[.wireEnumDecodingStrategy] as? ProtoDecoder.UnknownEnumValueDecodingStrategy
        return prefered ?? .throwError
    }
}
