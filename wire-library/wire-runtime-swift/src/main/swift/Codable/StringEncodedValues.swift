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

/// Converts an array of values to/from their string equivalent when serializing with Codable.
@propertyWrapper
struct StringEncodedValues<Value> {
    var wrappedValue: [Value]

    init(wrappedValue: [Value]) {
        self.wrappedValue = wrappedValue
    }
}

extension StringEncodedValues : Decodable where Value : StringDecodable {
    init(from decoder: Decoder) throws {
        var container = try decoder.unkeyedContainer()

        var results: [Value] = []
        if let count = container.count {
            results.reserveCapacity(count)
        }

        while !container.isAtEnd {
            let value = try container.decode(StringEncoded<Value>.self).wrappedValue
            results.append(value)
        }

        self.init(wrappedValue: results)
    }
}

extension StringEncodedValues : Encodable where Value : StringEncodable {
    func encode(to encoder: Encoder) throws {
        var container = encoder.unkeyedContainer()

        for value in wrappedValue {
            let wrapped = StringEncoded(wrappedValue: value)
            try container.encode(wrapped)
        }
    }
}

extension StringEncodedValues : Equatable where Value : Equatable {
}

extension StringEncodedValues : Hashable where Value : Hashable {
}

#if swift(>=5.5)
extension StringEncodedValues : Sendable where Value : Sendable {
}
#endif
