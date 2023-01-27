// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.packed_encoding.EmbeddedMessage in packed_encoding.proto
import Foundation
import Wire

public struct EmbeddedMessage {

    public var inner_repeated_number: [Int32]
    public var inner_number_after: Int32?
    public var unknownFields: Data = .init()

    public init(inner_repeated_number: [Int32] = [], inner_number_after: Int32? = nil) {
        self.inner_repeated_number = inner_repeated_number
        self.inner_number_after = inner_number_after
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension EmbeddedMessage : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension EmbeddedMessage : Hashable {
}
#endif

#if swift(>=5.5)
extension EmbeddedMessage : Sendable {
}
#endif

extension EmbeddedMessage : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.packed_encoding.EmbeddedMessage"
    }
}

extension EmbeddedMessage : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var inner_repeated_number: [Int32] = []
        var inner_number_after: Int32? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: try reader.decode(into: &inner_repeated_number)
            case 2: inner_number_after = try reader.decode(Int32.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.inner_repeated_number = inner_repeated_number
        self.inner_number_after = inner_number_after
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.inner_repeated_number, packed: true)
        try writer.encode(tag: 2, value: self.inner_number_after)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension EmbeddedMessage : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: StringLiteralCodingKeys.self)
        self.inner_repeated_number = try container.decodeProtoArray(Int32.self, firstOfKeys: "innerRepeatedNumber", "inner_repeated_number")
        self.inner_number_after = try container.decodeIfPresent(Int32.self, firstOfKeys: "innerNumberAfter", "inner_number_after")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringLiteralCodingKeys.self)
        let preferCamelCase = encoder.protoKeyNameEncodingStrategy == .camelCase
        let includeDefaults = encoder.protoDefaultValuesEncodingStrategy == .include

        if includeDefaults || !self.inner_repeated_number.isEmpty {
            try container.encodeProtoArray(self.inner_repeated_number, forKey: preferCamelCase ? "innerRepeatedNumber" : "inner_repeated_number")
        }
        if includeDefaults || !self.inner_number_after.isDefaultProtoValue {
            try container.encode(self.inner_number_after, forKey: preferCamelCase ? "innerNumberAfter" : "inner_number_after")
        }
    }
}
#endif
