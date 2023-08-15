// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.unknownfields.NestedVersionTwo in unknown_fields.proto
import Foundation
import Wire

public struct NestedVersionTwo {

    public var i: Int32?
    public var v2_i: Int32?
    public var v2_s: String?
    public var v2_f32: UInt32?
    public var v2_f64: UInt64?
    public var v2_rs: [String] = []
    public var unknownFields: Foundation.Data = .init()

    public init(configure: (inout Self) -> Void = { _ in }) {
        configure(&self)
    }

    @_disfavoredOverload
    public init(
        i: Int32? = nil,
        v2_i: Int32? = nil,
        v2_s: String? = nil,
        v2_f32: UInt32? = nil,
        v2_f64: UInt64? = nil,
        v2_rs: [String] = []
    ) {
        self.i = i
        self.v2_i = v2_i
        self.v2_s = v2_s
        self.v2_f32 = v2_f32
        self.v2_f64 = v2_f64
        self.v2_rs = v2_rs
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension NestedVersionTwo : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension NestedVersionTwo : Hashable {
}
#endif

#if swift(>=5.5)
extension NestedVersionTwo : Sendable {
}
#endif

extension NestedVersionTwo : ProtoMessage {

    public static func protoMessageTypeURL() -> Swift.String {
        return "type.googleapis.com/squareup.protos.kotlin.unknownfields.NestedVersionTwo"
    }

}

extension NestedVersionTwo : Proto2Codable {

    public init(from reader: Wire.ProtoReader) throws {
        var i: Swift.Int32? = nil
        var v2_i: Swift.Int32? = nil
        var v2_s: Swift.String? = nil
        var v2_f32: Swift.UInt32? = nil
        var v2_f64: Swift.UInt64? = nil
        var v2_rs: [Swift.String] = []

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: i = try reader.decode(Swift.Int32.self)
            case 2: v2_i = try reader.decode(Swift.Int32.self)
            case 3: v2_s = try reader.decode(Swift.String.self)
            case 4: v2_f32 = try reader.decode(Swift.UInt32.self, encoding: .fixed)
            case 5: v2_f64 = try reader.decode(Swift.UInt64.self, encoding: .fixed)
            case 6: try reader.decode(into: &v2_rs)
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
    }

    public func encode(to writer: Wire.ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.i)
        try writer.encode(tag: 2, value: self.v2_i)
        try writer.encode(tag: 3, value: self.v2_s)
        try writer.encode(tag: 4, value: self.v2_f32, encoding: .fixed)
        try writer.encode(tag: 5, value: self.v2_f64, encoding: .fixed)
        try writer.encode(tag: 6, value: self.v2_rs)
        try writer.writeUnknownFields(unknownFields)
    }

}

#if !WIRE_REMOVE_CODABLE
extension NestedVersionTwo : Codable {

    public init(from decoder: Swift.Decoder) throws {
        let container = try decoder.container(keyedBy: Wire.StringLiteralCodingKeys.self)
        self.i = try container.decodeIfPresent(Swift.Int32.self, forKey: "i")
        self.v2_i = try container.decodeIfPresent(Swift.Int32.self, firstOfKeys: "v2I", "v2_i")
        self.v2_s = try container.decodeIfPresent(Swift.String.self, firstOfKeys: "v2S", "v2_s")
        self.v2_f32 = try container.decodeIfPresent(Swift.UInt32.self, firstOfKeys: "v2F32", "v2_f32")
        self.v2_f64 = try container.decodeIfPresent(stringEncoded: Swift.UInt64.self, firstOfKeys: "v2F64", "v2_f64")
        self.v2_rs = try container.decodeProtoArray(Swift.String.self, firstOfKeys: "v2Rs", "v2_rs")
    }

    public func encode(to encoder: Swift.Encoder) throws {
        var container = encoder.container(keyedBy: Wire.StringLiteralCodingKeys.self)
        let preferCamelCase = encoder.protoKeyNameEncodingStrategy == .camelCase
        let includeDefaults = encoder.protoDefaultValuesEncodingStrategy == .include

        try container.encodeIfPresent(self.i, forKey: "i")
        try container.encodeIfPresent(self.v2_i, forKey: preferCamelCase ? "v2I" : "v2_i")
        try container.encodeIfPresent(self.v2_s, forKey: preferCamelCase ? "v2S" : "v2_s")
        try container.encodeIfPresent(self.v2_f32, forKey: preferCamelCase ? "v2F32" : "v2_f32")
        try container.encodeIfPresent(stringEncoded: self.v2_f64, forKey: preferCamelCase ? "v2F64" : "v2_f64")
        if includeDefaults || !self.v2_rs.isEmpty {
            try container.encodeProtoArray(self.v2_rs, forKey: preferCamelCase ? "v2Rs" : "v2_rs")
        }
    }

}
#endif
