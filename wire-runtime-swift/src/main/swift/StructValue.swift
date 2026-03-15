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
 * Wire implementation of `google.protobuf.Value`.
 *
 * The JSON representation is a bare JSON value (null, number, string, boolean, object, or array).
 */
public enum StructValue {
    case nullValue
    case numberValue(Double)
    case stringValue(String)
    case boolValue(Bool)
    case structValue(StructMessage)
    case listValue(ListValue)
}

#if !WIRE_REMOVE_EQUATABLE
extension StructValue: Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension StructValue: Hashable {
}
#endif

extension StructValue: Sendable {
}

extension StructValue: Proto3Codable {
    // google.protobuf.Value field tags:
    //   NullValue null_value = 1;
    //   double number_value = 2;
    //   string string_value = 3;
    //   bool bool_value = 4;
    //   Struct struct_value = 5;
    //   ListValue list_value = 6;

    public init(from reader: ProtoReader) throws {
        var result: StructValue?

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1:
                _ = try reader.decode(Int32.self)
                result = .nullValue
            case 2:
                result = .numberValue(try reader.decode(Double.self))
            case 3:
                result = .stringValue(try reader.decode(String.self))
            case 4:
                result = .boolValue(try reader.decode(Bool.self))
            case 5:
                result = .structValue(try reader.decode(StructMessage.self))
            case 6:
                result = .listValue(try reader.decode(ListValue.self))
            default:
                try reader.readUnknownField(tag: tag)
            }
        }
        // Unknown fields intentionally discarded.
        _ = try reader.endMessage(token: token)

        // Empty Value message (no oneof set) is treated as null per the proto spec.
        self = result ?? .nullValue
    }

    public func encode(to writer: ProtoWriter) throws {
        // Scalar cases cast to optional (`as X?`) to bypass proto3 default-value
        // omission — oneof fields must always be serialized.
        switch self {
        case .nullValue:
            try writer.encode(tag: 1, value: Int32(0) as Int32?, encoding: .variable)
        case .numberValue(let value):
            try writer.encode(tag: 2, value: value as Double?)
        case .stringValue(let value):
            try writer.encode(tag: 3, value: value as String?)
        case .boolValue(let value):
            try writer.encode(tag: 4, value: value as Bool?)
        case .structValue(let value):
            try writer.encode(tag: 5, value: value)
        case .listValue(let value):
            try writer.encode(tag: 6, value: value)
        }
    }
}

extension StructValue: ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/google.protobuf.Value"
    }
}

#if !WIRE_REMOVE_CODABLE
extension StructValue: Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        // Foundation's JSONDecoder permits multiple decode attempts on a single
        // SingleValueDecodingContainer; custom Decoder implementations may not.
        if container.decodeNil() {
            self = .nullValue
        }
        // Bool before Double: Foundation's JSONDecoder distinguishes booleans from numbers.
        else if let boolValue = try? container.decode(Bool.self) {
            self = .boolValue(boolValue)
        } else if let numberValue = try? container.decode(Double.self) {
            self = .numberValue(numberValue)
        } else if let stringValue = try? container.decode(String.self) {
            self = .stringValue(stringValue)
        } else if let structValue = try? container.decode(StructMessage.self) {
            self = .structValue(structValue)
        } else if let listValue = try? container.decode(ListValue.self) {
            self = .listValue(listValue)
        } else {
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Unable to decode google.protobuf.Value"
            )
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .nullValue:
            try container.encodeNil()
        case .numberValue(let value):
            try container.encode(value)
        case .stringValue(let value):
            try container.encode(value)
        case .boolValue(let value):
            try container.encode(value)
        case .structValue(let value):
            try container.encode(value)
        case .listValue(let value):
            try container.encode(value)
        }
    }
}
#endif
