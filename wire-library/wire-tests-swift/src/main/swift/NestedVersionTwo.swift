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
    public var v2_rs: [String]
    public var unknownFields: Data = .init()

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
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.kotlin.unknownfields.NestedVersionTwo"
    }
}

extension NestedVersionTwo : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var i: Int32? = nil
        var v2_i: Int32? = nil
        var v2_s: String? = nil
        var v2_f32: UInt32? = nil
        var v2_f64: UInt64? = nil
        var v2_rs: [String] = []

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: i = try reader.decode(Int32.self)
            case 2: v2_i = try reader.decode(Int32.self)
            case 3: v2_s = try reader.decode(String.self)
            case 4: v2_f32 = try reader.decode(UInt32.self, encoding: .fixed)
            case 5: v2_f64 = try reader.decode(UInt64.self, encoding: .fixed)
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

    public func encode(to writer: ProtoWriter) throws {
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
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: NestedVersionTwo.CodingKeys.self)
        self.i = try container.decodeIfPresent(Int32.self, forKey: .i)
        self.v2_i = try container.decodeIfPresent(Int32.self, forKey: .v2_i)
        self.v2_s = try container.decodeIfPresent(String.self, forKey: .v2_s)
        self.v2_f32 = try container.decodeIfPresent(UInt32.self, forKey: .v2_f32)
        self.v2_f64 = try container.decodeIfPresent(StringEncoded<UInt64>.self, forKey: .v2_f64)?.wrappedValue
        self.v2_rs = try container.decodeIfPresent([String].self, forKey: .v2_rs) ?? []
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: NestedVersionTwo.CodingKeys.self)
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.i != nil {
            try container.encode(self.i, forKey: .i)
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.v2_i != nil {
            try container.encode(self.v2_i, forKey: .v2_i)
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.v2_s != nil {
            try container.encode(self.v2_s, forKey: .v2_s)
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.v2_f32 != nil {
            try container.encode(self.v2_f32, forKey: .v2_f32)
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.v2_f64 != nil {
            try container.encode(StringEncoded(wrappedValue: self.v2_f64), forKey: .v2_f64)
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || !self.v2_rs.isEmpty {
            try container.encode(self.v2_rs, forKey: .v2_rs)
        }
    }

    public enum CodingKeys : String, CodingKey {

        case i
        case v2_i
        case v2_s
        case v2_f32
        case v2_f64
        case v2_rs

    }
}
#endif
