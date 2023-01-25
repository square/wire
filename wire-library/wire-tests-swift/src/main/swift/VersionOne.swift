// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.unknownfields.VersionOne in unknown_fields.proto
import Foundation
import Wire

public struct VersionOne {

    public var i: Int32?
    public var obj: NestedVersionOne?
    public var en: EnumVersionOne?
    public var unknownFields: Data = .init()

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
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.kotlin.unknownfields.VersionOne"
    }
}

extension VersionOne : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var i: Int32? = nil
        var obj: NestedVersionOne? = nil
        var en: EnumVersionOne? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: i = try reader.decode(Int32.self)
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

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.i)
        try writer.encode(tag: 7, value: self.obj)
        try writer.encode(tag: 8, value: self.en)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension VersionOne : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: StringLiteralCodingKeys.self)
        self.i = try container.decodeIfPresent(Int32.self, forKey: "i")
        self.obj = try container.decodeIfPresent(NestedVersionOne.self, forKey: "obj")
        self.en = try container.decodeIfPresent(EnumVersionOne.self, forKey: "en")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringLiteralCodingKeys.self)
        let includeDefaults = encoder.protoDefaultValuesEncodingStrategy == .include

        if includeDefaults || self.i != nil {
            try container.encode(self.i, forKey: "i")
        }
        if includeDefaults || self.obj != nil {
            try container.encode(self.obj, forKey: "obj")
        }
        if includeDefaults || self.en != nil {
            try container.encode(self.en, forKey: "en")
        }
    }
}
#endif
