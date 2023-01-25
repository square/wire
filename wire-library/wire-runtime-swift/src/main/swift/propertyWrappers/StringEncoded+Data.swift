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
        switch encoder.stringEncodedDataEncodingStrategy {
        case .base64:
            return base64EncodedString()

        case .base64url:
            return base64urlEncodedString()
        }
    }

    public init(encodedValue: String, from decoder: Decoder) throws {
        let possibleValue = Data(base64Encoded: encodedValue) ??
            Data(base64urlEncoded: encodedValue)

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
    func base64urlEncodedData(options: Data.Base64EncodingOptions = []) -> Data {
        let string = base64urlEncodedString(options: options)
        return Data(string.utf8)
    }

    func base64urlEncodedString(options: Data.Base64EncodingOptions = []) -> String {
        let base64 = base64EncodedString(options: options)
        return base64
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "=", with: "")
    }

    init?(
        base64urlEncoded base64urlData: Data,
        options: Data.Base64DecodingOptions = []
    ) {
        guard let base64urlString = String(data: base64urlData, encoding: .utf8) else {
            return nil
        }
        self.init(base64urlEncoded: base64urlString, options: options)
    }

    init?(
        base64urlEncoded base64urlString: String,
        options: Data.Base64DecodingOptions = []
    ) {
        // https://en.wikipedia.org/wiki/Base64#Output_padding
        let count = base64urlString.count
        let remainder = 4 - (count % 4)
        let desiredLength = count + remainder

        let base64String = base64urlString
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
            .padding(toLength: desiredLength, withPad: "=", startingAt: 0)

        self.init(base64Encoded: base64String, options: options)
    }
}
