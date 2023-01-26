// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.unknownfields.VersionTwo in unknown_fields.proto
import Foundation
import Wire

public struct VersionTwo {

    public var i: Int32?
    public var v2_i: Int32?
    public var v2_s: String?
    public var v2_f32: UInt32?
    public var v2_f64: UInt64?
    public var v2_rs: [String]
    public var obj: NestedVersionTwo?
    public var en: EnumVersionTwo?
    public var unknownFields: Data = .init()

    public init(
        i: Int32? = nil,
        v2_i: Int32? = nil,
        v2_s: String? = nil,
        v2_f32: UInt32? = nil,
        v2_f64: UInt64? = nil,
        v2_rs: [String] = [],
        obj: NestedVersionTwo? = nil,
        en: EnumVersionTwo? = nil
    ) {
        self.i = i
        self.v2_i = v2_i
        self.v2_s = v2_s
        self.v2_f32 = v2_f32
        self.v2_f64 = v2_f64
        self.v2_rs = v2_rs
        self.obj = obj
        self.en = en
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension VersionTwo : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension VersionTwo : Hashable {
}
#endif

#if swift(>=5.5)
extension VersionTwo : Sendable {
}
#endif

extension VersionTwo : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.kotlin.unknownfields.VersionTwo"
    }
}

extension VersionTwo : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var i: Int32? = nil
        var v2_i: Int32? = nil
        var v2_s: String? = nil
        var v2_f32: UInt32? = nil
        var v2_f64: UInt64? = nil
        var v2_rs: [String] = []
        var obj: NestedVersionTwo? = nil
        var en: EnumVersionTwo? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: i = try reader.decode(Int32.self)
            case 2: v2_i = try reader.decode(Int32.self)
            case 3: v2_s = try reader.decode(String.self)
            case 4: v2_f32 = try reader.decode(UInt32.self, encoding: .fixed)
            case 5: v2_f64 = try reader.decode(UInt64.self, encoding: .fixed)
            case 6: try reader.decode(into: &v2_rs)
            case 7: obj = try reader.decode(NestedVersionTwo.self)
            case 8: en = try reader.decode(EnumVersionTwo.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.i = i
        self.v2_i = v2_i
        self.v2_s = v2_s
        self.v2_f32 = v2_f32
        self.v2_f64 = v2_f64
        self.v2_rs = v2_rs
        self.obj = obj
        self.en = en
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.i)
        try writer.encode(tag: 2, value: self.v2_i)
        try writer.encode(tag: 3, value: self.v2_s)
        try writer.encode(tag: 4, value: self.v2_f32, encoding: .fixed)
        try writer.encode(tag: 5, value: self.v2_f64, encoding: .fixed)
        try writer.encode(tag: 6, value: self.v2_rs)
        try writer.encode(tag: 7, value: self.obj)
        try writer.encode(tag: 8, value: self.en)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension VersionTwo : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: StringLiteralCodingKeys.self)
        self.i = try container.decodeIfPresent(Int32.self, forKey: "i")
        self.v2_i = try container.decodeFirstIfPresent(Int32.self, forKeys: "v2I", "v2_i")
        self.v2_s = try container.decodeFirstIfPresent(String.self, forKeys: "v2S", "v2_s")
        self.v2_f32 = try container.decodeFirstIfPresent(UInt32.self, forKeys: "v2F32", "v2_f32")
        self.v2_f64 = try container.decodeFirstStringEncodedIfPresent(UInt64.self, forKeys: "v2F64", "v2_f64")
        self.v2_rs = try container.decodeFirstProtoArray(String.self, forKeys: "v2Rs", "v2_rs")
        self.obj = try container.decodeIfPresent(NestedVersionTwo.self, forKey: "obj")
        self.en = try container.decodeIfPresent(EnumVersionTwo.self, forKey: "en")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringLiteralCodingKeys.self)
        let preferCamelCase = encoder.protoKeyNameEncodingStrategy == .camelCase
        let includeDefaults = encoder.protoDefaultValuesEncodingStrategy == .include

        if includeDefaults || self.i != nil {
            try container.encode(self.i, forKey: "i")
        }
        if includeDefaults || self.v2_i != nil {
            try container.encode(self.v2_i, forKey: preferCamelCase ? "v2I" : "v2_i")
        }
        if includeDefaults || self.v2_s != nil {
            try container.encode(self.v2_s, forKey: preferCamelCase ? "v2S" : "v2_s")
        }
        if includeDefaults || self.v2_f32 != nil {
            try container.encode(self.v2_f32, forKey: preferCamelCase ? "v2F32" : "v2_f32")
        }
        if includeDefaults || self.v2_f64 != nil {
            try container.encode(StringEncoded(wrappedValue: self.v2_f64), forKey: preferCamelCase ? "v2F64" : "v2_f64")
        }
        if includeDefaults || !self.v2_rs.isEmpty {
            try container.encode(self.v2_rs, forKey: preferCamelCase ? "v2Rs" : "v2_rs")
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
