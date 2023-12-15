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

/// Converts values to/from their string equivalent when serializing with Codable.
@propertyWrapper
struct StringEncoded<Value> {
    var wrappedValue: Value

    init(wrappedValue: Value) {
        self.wrappedValue = wrappedValue
    }
}

extension StringEncoded : Decodable where Value : StringDecodable {
    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        if let stringValue = try? container.decode(String.self) {
            let value = try Value(encodedValue: stringValue)
            self.init(wrappedValue: value)
            return
        }

        let value = try container.decode(Value.self)
        self.init(wrappedValue: value)
    }
}

extension StringEncoded : Encodable where Value : StringEncodable {
    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        try container.encode(wrappedValue.stringEncodedValue())
    }
}

extension StringEncoded : Equatable where Value : Equatable {
}

extension StringEncoded : Hashable where Value : Hashable {
}

extension StringEncoded : Sendable where Value : Sendable {
}
