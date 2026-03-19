/*
 * Copyright (C) 2026 Square, Inc.
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

/**
 * Wire implementation of `google.protobuf.NullValue`.
 *
 * The JSON representation is JSON `null`.
 */
public enum StructNull: Int32, CaseIterable, Proto3Enum {

    case NULL_VALUE = 0 // swiftlint:disable:this identifier_name

    public var description: String {
        switch self {
        case .NULL_VALUE: return "NULL_VALUE"
        }
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension StructNull: Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension StructNull: Hashable {
}
#endif

extension StructNull: Sendable {
}

extension StructNull: ProtoDefaultedValue {

    public static var defaultedValue: Self {
        .NULL_VALUE
    }

}

#if !WIRE_REMOVE_CODABLE
extension StructNull: Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        guard container.decodeNil() else {
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Expected null for google.protobuf.NullValue"
            )
        }
        self = .NULL_VALUE
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encodeNil()
    }
}
#endif
