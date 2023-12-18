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

@propertyWrapper
struct ProtoMap<Key : Hashable & LosslessStringConvertible, Value> {
    var wrappedValue: [Key: Value]

    init(wrappedValue: [Key: Value]) {
        self.wrappedValue = wrappedValue
    }
}

extension ProtoMap : Encodable where Value : Encodable {
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringLiteralCodingKeys.self)
        for kvp in wrappedValue {
            let codingKey = StringLiteralCodingKeys(stringValue: kvp.key.description)
            if let value = kvp.value as? StringEncodable {
                try container.encode(stringEncoded: value, forKey: codingKey)
            } else {
                try container.encode(kvp.value, forKey: codingKey)
            }
        }
    }
}

extension ProtoMap : Decodable where Value : Decodable {
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: StringLiteralCodingKeys.self)

        var results: [Key: Value] = [:]
        results.reserveCapacity(container.allKeys.count)

        for codingKey in container.allKeys {
            guard let key = Key(codingKey.stringValue) else {
                throw ProtoDecoder.Error.unparsableString(type: Key.self, value: codingKey.stringValue)
            }

            if let valueType = Value.self as? StringDecodable.Type {
                results[key] = try container.decode(stringEncoded: valueType, forKey: codingKey) as? Value
            } else if let valueType = Value.self as? ProtoEnum.Type {
                results[key] = try container.decode(safeEnum: valueType, forKey: codingKey) as? Value
            } else {
                results[key] = try container.decode(Value.self, forKey: codingKey)
            }
        }

        self.init(wrappedValue: results)
    }
}

extension ProtoMap : Equatable where Value : Equatable {
}

extension ProtoMap : Hashable where Value : Hashable {
}

extension ProtoMap : Sendable where Key : Sendable, Value : Sendable {
}
