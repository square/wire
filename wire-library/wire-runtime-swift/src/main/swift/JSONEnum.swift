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
        do {
            let string = try container.decode(String.self)
            guard let value = T.allCases.first(where: { "\($0)" == string }) else {
                throw ProtoDecoder.Error.unknownEnumString(type: T.self, string: string)
            }
            self.wrappedValue = value
        } catch {
            // If the value wasn't a string, then look for the field index instead.
            let fieldNumber = try container.decode(UInt32.self)
            guard let value = T.init(rawValue: fieldNumber) else {
                throw ProtoDecoder.Error.unknownEnumCase(type: T.self, fieldNumber: fieldNumber)
            }
            self.wrappedValue = value
        }
    }

    public func encode(to encoder: Encoder) throws {
        try "\(wrappedValue)".encode(to: encoder)
    }

}
