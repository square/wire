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

/// Common protocol that all Wire generated enums conform to
/// - Note: All ProtoEnums will convert to/from their field and string equivalent when serializing via Codable.
/// This matches the Proto3 JSON spec: https://developers.google.com/protocol-buffers/docs/proto3#json
public protocol ProtoEnum : LosslessStringConvertible, CaseIterable, RawRepresentable, Codable where RawValue == UInt32 {
}

extension ProtoEnum {
    public init?(_ description: String) {
        guard let result = Self.allCases.first(where: { $0.description == description }) else {
            return nil
        }
        self = result
    }

    public init(from decoder: Decoder) throws {
        // We support decoding from either the string value or the field index.
        let container = try decoder.singleValueContainer()

        if let string = try? container.decode(String.self) {
            guard let value = Self(string) else {
                throw ProtoDecoder.Error.unknownEnumString(type: Self.self, string: string)
            }
            self = value
        } else {
            // If the value wasn't a string, then look for the field index instead.
            let fieldNumber = try container.decode(UInt32.self)
            guard let value = Self(rawValue: fieldNumber) else {
                throw ProtoDecoder.Error.unknownEnumCase(type: Self.self, fieldNumber: fieldNumber)
            }

            self = value
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        switch encoder.enumEncodingStrategy {
        case .string:
            try container.encode(description)

        case .integer:
            try container.encode(rawValue)
        }
    }
}

/// This  type gives me access to `decoder.enumDecodingStrategy`
private struct BoxedEnum<T: ProtoEnum> : Decodable {
    let value: T?

    init(value: T? = nil) {
        self.value = value
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        guard !container.decodeNil() else {
            self.init()
            return
        }

        switch decoder.enumDecodingStrategy {
        case .shouldSkip:
            let value = try? container.decode(T.self)
            self.init(value: value)

        case .shouldThrow:
            let value = try container.decode(T.self)
            self.init(value: value)
        }
    }
}

public extension KeyedDecodingContainer {
    func decodeIfPresent<T: ProtoEnum>(
        _: T.Type,
        forKey key: Key
    ) throws -> T? {
        guard contains(key) else {
            return nil
        }

        let box = try decode(BoxedEnum<T>.self, forKey: key)
        return box.value
    }

    func decode<T: ProtoEnum>(
        _: Array<T>.Type,
        forKey key: Key
    ) throws -> [T] {
        var container = try nestedUnkeyedContainer(forKey: key)

        var results: [T] = []
        if let count = container.count {
            results.reserveCapacity(count)
        }

        while !container.isAtEnd {
            let box = try container.decode(BoxedEnum<T>.self)
            if let value = box.value {
                results.append(value)
            }
        }

        return results
    }

    func decode<T: ProtoEnum>(
        _: Set<T>.Type,
        forKey key: Key
    ) throws -> Set<T> {
        var container = try nestedUnkeyedContainer(forKey: key)

        var results: Set<T> = []
        if let count = container.count {
            results.reserveCapacity(count)
        }

        while !container.isAtEnd {
            let box = try container.decode(BoxedEnum<T>.self)
            if let value = box.value {
                results.insert(value)
            }
        }

        return results
    }
}
