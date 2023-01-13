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

/**
 Converts enums to/from their string equivalent when serializing to/from JSON.
 This matches the Proto3 JSON spec: https://developers.google.com/protocol-buffers/docs/proto3#json
 */
@propertyWrapper
public struct JSONOptionalEnum<T : CaseIterable & Hashable & RawRepresentable> : Codable, Hashable where T.RawValue == UInt32 {
    private var storage: JSONEnum<T>?

    public var wrappedValue: T? {
        get {
            storage?.wrappedValue
        }
        set {
            storage = newValue.map(JSONEnum.init(wrappedValue:))
        }
    }

    public init(wrappedValue: T?) {
        storage = wrappedValue.map(JSONEnum.init(wrappedValue:))
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        if container.decodeNil() {
            storage = nil
        } else {
            switch decoder.enumDecodingStrategy {
            case .shouldSkip:
                storage = try? container.decode(JSONEnum<T>.self)

            case .shouldThrow:
                storage = try container.decode(JSONEnum<T>.self)
            }
        }
    }

    public func encode(to encoder: Encoder) throws {
        try storage.encode(to: encoder)
    }
}

extension JSONOptionalEnum : Sendable where T : Sendable {
}

public extension KeyedDecodingContainer {
    func decode<T: CaseIterable & Hashable & RawRepresentable>(
        _: JSONOptionalEnum<T>.Type,
        forKey key: Key
    ) throws -> JSONOptionalEnum<T> {
        if let value = try decodeIfPresent(JSONOptionalEnum<T>.self, forKey: key) {
            return value
        } else {
            return JSONOptionalEnum(wrappedValue: nil)
        }
    }
}
