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
 Converts enums to/from their field and string equivalent when serializing via Codable.
 This matches the Proto3 JSON spec: https://developers.google.com/protocol-buffers/docs/proto3#json
 */
@propertyWrapper
public struct ProtoEnumOptionalEncoded<T : ProtoEnum> {
    private var storage: ProtoEnumEncoded<T>?

    public var wrappedValue: T? {
        get {
            storage?.wrappedValue
        }
        set {
            storage = newValue.map(ProtoEnumEncoded.init(wrappedValue:))
        }
    }

    public init(wrappedValue: T?) {
        storage = wrappedValue.map(ProtoEnumEncoded.init(wrappedValue:))
    }
}

extension ProtoEnumOptionalEncoded : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        if container.decodeNil() {
            storage = nil
        } else {
            switch decoder.enumDecodingStrategy {
            case .shouldSkip:
                storage = try? container.decode(ProtoEnumEncoded<T>.self)

            case .shouldThrow:
                storage = try container.decode(ProtoEnumEncoded<T>.self)
            }
        }
    }

    public func encode(to encoder: Encoder) throws {
        try storage.encode(to: encoder)
    }
}

extension ProtoEnumOptionalEncoded : Equatable where T : Equatable {
}

extension ProtoEnumOptionalEncoded : Hashable where T : Hashable {
}

#if swift(>=5.5)
extension ProtoEnumOptionalEncoded : Sendable where T : Sendable {
}
#endif

public extension KeyedDecodingContainer {
    func decode<T: CaseIterable & Hashable & RawRepresentable>(
        _: ProtoEnumOptionalEncoded<T>.Type,
        forKey key: Key
    ) throws -> ProtoEnumOptionalEncoded<T> {
        if let value = try decodeIfPresent(ProtoEnumOptionalEncoded<T>.self, forKey: key) {
            return value
        } else {
            return ProtoEnumOptionalEncoded(wrappedValue: nil)
        }
    }
}
