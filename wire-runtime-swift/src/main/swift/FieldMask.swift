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
 * Wire implementation of `google.protobuf.FieldMask`.
 *
 * The binary representation is a repeated `paths` string field.
 */
public struct FieldMask {
    public var paths: [String]

    public init(paths: [String] = []) {
        self.paths = paths
    }
}

#if !WIRE_REMOVE_EQUATABLE
extension FieldMask: Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension FieldMask: Hashable {
}
#endif

extension FieldMask: Sendable {
}

extension FieldMask: Proto3Codable {
    // google.protobuf.FieldMask:
    //   repeated string paths = 1;

    public init(from reader: ProtoReader) throws {
        var paths: [String] = []

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1:
                try reader.decode(into: &paths)
            default:
                try reader.readUnknownField(tag: tag)
            }
        }
        // Unknown fields intentionally discarded.
        _ = try reader.endMessage(token: token)

        self.paths = paths
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.paths)
    }
}

extension FieldMask: ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/google.protobuf.FieldMask"
    }
}

#if !WIRE_REMOVE_CODABLE
extension FieldMask: Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let value = try container.decode(String.self)
        self.paths = value.isEmpty
            ? []
            : value.split(separator: ",").map { FieldMask.protoName(String($0)) }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(paths.map(FieldMask.jsonName).joined(separator: ","))
    }

    private static func jsonName(_ path: String) -> String {
        return path
            .split(separator: ".", omittingEmptySubsequences: false)
            .map { lowerCamel(String($0)) }
            .joined(separator: ".")
    }

    private static func protoName(_ path: String) -> String {
        return path
            .split(separator: ".", omittingEmptySubsequences: false)
            .map { snakeCase(String($0)) }
            .joined(separator: ".")
    }

    private static func lowerCamel(_ value: String) -> String {
        var result = ""
        var capitalizeNext = false
        for scalar in value.unicodeScalars {
            if scalar == "_" {
                capitalizeNext = true
            } else if capitalizeNext {
                result.append(String(scalar).uppercased())
                capitalizeNext = false
            } else {
                result.append(String(scalar))
            }
        }
        return result
    }

    private static func snakeCase(_ value: String) -> String {
        var result = ""
        for scalar in value.unicodeScalars {
            if scalar.value >= 65 && scalar.value <= 90 {
                if !result.isEmpty {
                    result.append("_")
                }
                result.append(String(scalar).lowercased())
            } else {
                result.append(String(scalar))
            }
        }
        return result
    }
}
#endif
