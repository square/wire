// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.simple.ExternalMessage in external_message.proto
import Foundation
import Wire

public struct ExternalMessage {

    @Defaulted(defaultValue: 20)
    public var f: Float?
    public var unknownFields: Foundation.Data = .init()

    public init() {
    }

    @_disfavoredOverload
    public init(f: Float? = nil) {
        _f.wrappedValue = f
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension ExternalMessage : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension ExternalMessage : Hashable {
}
#endif

#if swift(>=5.5)
extension ExternalMessage : Sendable {
}
#endif

extension ExternalMessage : ProtoMessage {

    public static func protoMessageTypeURL() -> Swift.String {
        return "type.googleapis.com/squareup.protos.kotlin.simple.ExternalMessage"
    }

}

extension ExternalMessage : Proto2Codable {

    public init(from reader: Wire.ProtoReader) throws {
        var f: Swift.Float? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: f = try reader.decode(Swift.Float.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        _f.wrappedValue = f
    }

    public func encode(to writer: Wire.ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.f)
        try writer.writeUnknownFields(unknownFields)
    }

}

#if !WIRE_REMOVE_CODABLE
extension ExternalMessage : Codable {

    public init(from decoder: Swift.Decoder) throws {
        let container = try decoder.container(keyedBy: Wire.StringLiteralCodingKeys.self)
        _f.wrappedValue = try container.decodeIfPresent(Swift.Float.self, forKey: "f")
    }

    public func encode(to encoder: Swift.Encoder) throws {
        var container = encoder.container(keyedBy: Wire.StringLiteralCodingKeys.self)

        try container.encodeIfPresent(self.f, forKey: "f")
    }

}
#endif
