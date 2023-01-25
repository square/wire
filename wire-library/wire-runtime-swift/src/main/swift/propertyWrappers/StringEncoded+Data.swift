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

extension Data : StringCodable {
    public func stringEncodedValue(in encoder: Encoder) throws -> String {
        return base64EncodedString()
    }

    public init(encodedValue: String, from decoder: Decoder) throws {
        let possibleValue = Data(base64Encoded: encodedValue) ??
            Data(base64URLEncoded: encodedValue)

        guard let value = possibleValue else {
            throw ProtoDecoder.Error.unparsableString(
                type: Data.self,
                value: encodedValue
            )
        }
        self = value
    }
}

public extension Data {
    /// Returns Base-64 URL-Safe encoded data.
    /// - SeeAlso: [RFC4648](https://www.rfc-editor.org/rfc/rfc4648#section-5)
    func base64URLEncodedData(options: Data.Base64EncodingOptions = []) -> Data {
        let string = base64URLEncodedString(options: options)
        return Data(string.utf8)
    }

    /// Returns a Base-64 URL-Safe encoded string.
    /// - SeeAlso: [RFC4648](https://www.rfc-editor.org/rfc/rfc4648#section-5)
    func base64URLEncodedString(options: Data.Base64EncodingOptions = []) -> String {
        let base64 = base64EncodedString(options: options)
        return base64
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "=", with: "")
    }

    /// Initialize Data from Base-64 URL-Safe encoded data.
    /// - SeeAlso: [RFC4648](https://www.rfc-editor.org/rfc/rfc4648#section-5)
    init?(
        base64URLEncoded base64URLData: Data,
        options: Data.Base64DecodingOptions = []
    ) {
        guard let base64URLString = String(data: base64URLData, encoding: .utf8) else {
            return nil
        }
        self.init(base64URLEncoded: base64URLString, options: options)
    }

    /// Initialize Data from a Base-64 URL-Safe encoded string.
    /// - SeeAlso: [RFC4648](https://www.rfc-editor.org/rfc/rfc4648#section-5)
    init?(
        base64URLEncoded base64URLString: String,
        options: Data.Base64DecodingOptions = []
    ) {
        // https://en.wikipedia.org/wiki/Base64#Output_padding
        let count = base64URLString.count
        let remainder = 4 - (count % 4)
        let desiredLength = count + remainder

        let base64String = base64URLString
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
            .padding(toLength: desiredLength, withPad: "=", startingAt: 0)

        self.init(base64Encoded: base64String, options: options)
    }
}
