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
 * Wire implementation of `google.protobuf.Struct`.
 *
 * The JSON representation is a JSON object (keys are field names, values are `StructValue`s).
 */
public struct StructMessage {
    public var fields: [String: StructValue]

    public init(fields: [String: StructValue] = [:]) {
        self.fields = fields
    }
}

#if !WIRE_REMOVE_EQUATABLE
extension StructMessage: Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension StructMessage: Hashable {
}
#endif

extension StructMessage: Sendable {
}

extension StructMessage: Proto3Codable {
    // map<string, Value> fields = 1;
    // Wire encodes map fields as repeated MapEntry messages.

    public init(from reader: ProtoReader) throws {
        var fields: [String: StructValue] = [:]

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1:
                try reader.decode(into: &fields)
            default:
                try reader.readUnknownField(tag: tag)
            }
        }
        // Unknown fields intentionally discarded.
        _ = try reader.endMessage(token: token)

        self.fields = fields
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.fields)
    }
}

extension StructMessage: ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/google.protobuf.Struct"
    }
}

#if !WIRE_REMOVE_CODABLE
extension StructMessage: Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        self.fields = try container.decode([String: StructValue].self)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(self.fields)
    }
}
#endif
