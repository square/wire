// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.unknownfields.VersionOne in unknown_fields.proto
import Foundation
import Wire

public struct VersionOne {

    public var i: Int32?
    public var obj: NestedVersionOne?
    public var en: EnumVersionOne?
    public var unknownFields: Foundation.Data = .init()

    public init() {
    }

    @_disfavoredOverload
    public init(
        i: Int32? = nil,
        obj: NestedVersionOne? = nil,
        en: EnumVersionOne? = nil
    ) {
        self.i = i
        self.obj = obj
        self.en = en
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension VersionOne : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension VersionOne : Hashable {
}
#endif

#if swift(>=5.5)
extension VersionOne : Sendable {
}
#endif

extension VersionOne : ProtoMessage {

    public static func protoMessageTypeURL() -> Swift.String {
        return "type.googleapis.com/squareup.protos.kotlin.unknownfields.VersionOne"
    }

}

extension VersionOne : Proto2Codable {

    public init(from reader: Wire.ProtoReader) throws {
        var i: Swift.Int32? = nil
        var obj: NestedVersionOne? = nil
        var en: EnumVersionOne? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: i = try reader.decode(Swift.Int32.self)
            case 7: obj = try reader.decode(NestedVersionOne.self)
            case 8: en = try reader.decode(EnumVersionOne.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.i = i
        self.obj = obj
        self.en = en
    }

    public func encode(to writer: Wire.ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.i)
        try writer.encode(tag: 7, value: self.obj)
        try writer.encode(tag: 8, value: self.en)
        try writer.writeUnknownFields(unknownFields)
    }

}

#if !WIRE_REMOVE_CODABLE
extension VersionOne : Codable {

    public init(from decoder: Swift.Decoder) throws {
        let container = try decoder.container(keyedBy: Wire.StringLiteralCodingKeys.self)
        self.i = try container.decodeIfPresent(Swift.Int32.self, forKey: "i")
        self.obj = try container.decodeIfPresent(NestedVersionOne.self, forKey: "obj")
        self.en = try container.decodeIfPresent(EnumVersionOne.self, forKey: "en")
    }

    public func encode(to encoder: Swift.Encoder) throws {
        var container = encoder.container(keyedBy: Wire.StringLiteralCodingKeys.self)

        try container.encodeIfPresent(self.i, forKey: "i")
        try container.encodeIfPresent(self.obj, forKey: "obj")
        try container.encodeIfPresent(self.en, forKey: "en")
    }

}
#endif
