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
 * Wire implementation of `google.protobuf.ListValue`.
 *
 * The JSON representation is a JSON array.
 */
public struct ListValue {
    public var values: [StructValue]

    public init(values: [StructValue] = []) {
        self.values = values
    }
}

#if !WIRE_REMOVE_EQUATABLE
extension ListValue: Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension ListValue: Hashable {
}
#endif

extension ListValue: Sendable {
}

extension ListValue: Proto3Codable {
    // google.protobuf.ListValue:
    //   repeated Value values = 1;

    public init(from reader: ProtoReader) throws {
        var values: [StructValue] = []

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1:
                try reader.decode(into: &values)
            default:
                try reader.readUnknownField(tag: tag)
            }
        }
        // Unknown fields intentionally discarded.
        _ = try reader.endMessage(token: token)

        self.values = values
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.values)
    }
}

extension ListValue: ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/google.protobuf.ListValue"
    }
}

#if !WIRE_REMOVE_CODABLE
extension ListValue: Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        self.values = try container.decode([StructValue].self)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(self.values)
    }
}
#endif
