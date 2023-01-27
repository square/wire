// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.Percents in percents_in_kdoc.proto
import Foundation
import Wire

public struct Percents {

    /**
     * e.g. "No limits, free to send and just 2.75% to receive".
     */
    public var text: String?
    public var unknownFields: Data = .init()

    public init(text: String? = nil) {
        self.text = text
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension Percents : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension Percents : Hashable {
}
#endif

#if swift(>=5.5)
extension Percents : Sendable {
}
#endif

extension Percents : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.kotlin.Percents"
    }
}

extension Percents : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var text: String? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: text = try reader.decode(String.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.text = text
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.text)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension Percents : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: StringLiteralCodingKeys.self)
        self.text = try container.decodeIfPresent(String.self, forKey: "text")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringLiteralCodingKeys.self)
        let includeDefaults = encoder.protoDefaultValuesEncodingStrategy == .include

        if includeDefaults || !self.text.isDefaultProtoValue {
            try container.encode(self.text, forKey: "text")
        }
    }
}
#endif
