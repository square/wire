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

/// The encoding strategy to use for JSONEnum types
/// Defaults to .string
public enum EnumEncodingStrategy {
    case string
    case integer
}

/// The decoding strategy to use for JSONEnum types
/// Defaults to .shouldThrow
/// - Note: @JSONEnum will _always_ throw
public enum EnumDecodingStrategy {
    case shouldThrow
    case shouldSkip
}

extension CodingUserInfoKey {
    /// Control the encoding of the raw value of Enums
    /// - SeeAlso: EnumEncodingStrategy
    public static let wireEnumEncodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.EnumEncodingStrategy")!

    /// Control the decoding of Enums
    /// - SeeAlso: EnumDecodingStrategy
    public static let wireEnumDecodingStrategy = CodingUserInfoKey(rawValue: "com.squareup.wire.wireEnumDecodingStrategy")!
}

extension Encoder {
    var enumEncodingStrategy: EnumEncodingStrategy {
        let preferred = userInfo[.wireEnumEncodingStrategy] as? EnumEncodingStrategy
        return preferred ?? .string
    }
}

extension Decoder {
    var enumDecodingStrategy: EnumDecodingStrategy {
        let prefered = userInfo[.wireEnumDecodingStrategy] as? EnumDecodingStrategy
        return prefered ?? .shouldThrow
    }
}
