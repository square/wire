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
struct ProtoArray<Value> {
    var wrappedValue: [Value]

    init(wrappedValue: [Value]) {
        self.wrappedValue = wrappedValue
    }
}

extension ProtoArray : Encodable where Value : Encodable {
    func encode(to encoder: Encoder) throws {
        var container = encoder.unkeyedContainer()

        for value in wrappedValue {
            if let value = value as? StringEncodable {
                try container.encode(stringEncoded: value)
            } else {
                try container.encode(value)
            }
        }
    }
}

extension ProtoArray : Decodable where Value : Decodable {
    init(from decoder: Decoder) throws {
        var container = try decoder.unkeyedContainer()

        var results: [Value] = []
        if let count = container.count {
            results.reserveCapacity(count)
        }

        while !container.isAtEnd {
            if let valueType = Value.self as? StringDecodable.Type {
                guard let value = try container.decode(stringEncoded: valueType) as? Value else {
                    throw DecodingError.dataCorruptedError(in: container, debugDescription: "Could not convert value back to expected type")
                }
                results.append(value)
            } else if let valueType = Value.self as? ProtoEnum.Type {
                if let value = try container.decode(safeEnum: valueType) as? Value {
                    results.append(value)
                }
            } else {
                let value = try container.decode(Value.self)
                results.append(value)
            }
        }

        self.init(wrappedValue: results)
    }
}

extension ProtoArray : Equatable where Value : Equatable {
}

extension ProtoArray : Hashable where Value : Hashable {
}

extension ProtoArray : Sendable where Value : Sendable {
}
