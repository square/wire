// Code generated by Wire protocol buffer compiler, do not edit.
// Source: com.squareup.wire.protos.kotlin.map.MappyTwo in map.proto
import Foundation
import Wire

public struct MappyTwo {

    public var string_enums: [String : ValueEnum]
    public var int_things: [Int64 : Thing]
    public var string_ints: [String : Int64]
    public var int_things_two: [Int32 : Thing]
    public var unknownFields: Data = .init()

    public init(
        string_enums: [String : ValueEnum] = [:],
        int_things: [Int64 : Thing] = [:],
        string_ints: [String : Int64] = [:],
        int_things_two: [Int32 : Thing] = [:]
    ) {
        self.string_enums = string_enums
        self.int_things = int_things
        self.string_ints = string_ints
        self.int_things_two = int_things_two
    }

    public enum ValueEnum : UInt32, CaseIterable, ProtoEnum {

        case DEFAULT = 0
        case FOO = 1
        case BAR = 2

        public var description: String {
            switch self {
            case .DEFAULT: return "DEFAULT"
            case .FOO: return "FOO"
            case .BAR: return "BAR"
            }
        }

    }

}

#if swift(>=5.5)
extension MappyTwo.ValueEnum : Sendable {
}
#endif

#if !WIRE_REMOVE_EQUATABLE
extension MappyTwo : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension MappyTwo : Hashable {
}
#endif

#if swift(>=5.5)
extension MappyTwo : Sendable {
}
#endif

extension MappyTwo : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/com.squareup.wire.protos.kotlin.map.MappyTwo"
    }
}

extension MappyTwo : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var string_enums: [String : MappyTwo.ValueEnum] = [:]
        var int_things: [Int64 : Thing] = [:]
        var string_ints: [String : Int64] = [:]
        var int_things_two: [Int32 : Thing] = [:]

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: try reader.decode(into: &string_enums)
            case 2: try reader.decode(into: &int_things, keyEncoding: .signed)
            case 3: try reader.decode(into: &string_ints, valueEncoding: .signed)
            case 4: try reader.decode(into: &int_things_two, keyEncoding: .signed)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.string_enums = string_enums
        self.int_things = int_things
        self.string_ints = string_ints
        self.int_things_two = int_things_two
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.string_enums)
        try writer.encode(tag: 2, value: self.int_things, keyEncoding: .signed)
        try writer.encode(tag: 3, value: self.string_ints, valueEncoding: .signed)
        try writer.encode(tag: 4, value: self.int_things_two, keyEncoding: .signed)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension MappyTwo : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: MappyTwo.CodingKeys.self)
        self.string_enums = try container.decodeIfPresent(ProtoMapEnumValues<String, MappyTwo.ValueEnum>.self, forKey: "stringEnums")?.wrappedValue ??
                container.decodeIfPresent(ProtoMapEnumValues<String, MappyTwo.ValueEnum>.self, forKey: "string_enums")?.wrappedValue ?? [:]
        self.int_things = try container.decodeIfPresent(ProtoMap<Int64, Thing>.self, forKey: "intThings")?.wrappedValue ??
                container.decodeIfPresent(ProtoMap<Int64, Thing>.self, forKey: "int_things")?.wrappedValue ?? [:]
        self.string_ints = try container.decodeIfPresent(ProtoMapStringEncodedValues<String, Int64>.self, forKey: "stringInts")?.wrappedValue ??
                container.decodeIfPresent(ProtoMapStringEncodedValues<String, Int64>.self, forKey: "string_ints")?.wrappedValue ?? [:]
        self.int_things_two = try container.decodeIfPresent(ProtoMap<Int32, Thing>.self, forKey: "intThingsTwo")?.wrappedValue ??
                container.decodeIfPresent(ProtoMap<Int32, Thing>.self, forKey: "int_things_two")?.wrappedValue ?? [:]
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: MappyTwo.CodingKeys.self)
        let preferCamelCase = encoder.protoKeyNameEncodingStrategy == .camelCase

        if encoder.protoDefaultValuesEncodingStrategy == .emit || !self.string_enums.isEmpty {
            try container.encode(ProtoMapEnumValues(wrappedValue: self.string_enums), forKey: preferCamelCase ? "stringEnums" : "string_enums")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || !self.int_things.isEmpty {
            try container.encode(ProtoMap(wrappedValue: self.int_things), forKey: preferCamelCase ? "intThings" : "int_things")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || !self.string_ints.isEmpty {
            try container.encode(ProtoMapStringEncodedValues(wrappedValue: self.string_ints), forKey: preferCamelCase ? "stringInts" : "string_ints")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || !self.int_things_two.isEmpty {
            try container.encode(ProtoMap(wrappedValue: self.int_things_two), forKey: preferCamelCase ? "intThingsTwo" : "int_things_two")
        }
    }

    public struct CodingKeys : CodingKey, ExpressibleByStringLiteral {

        public let stringValue: String
        public let intValue: Int?

        public init(stringValue: String) {
            self.stringValue = stringValue
            self.intValue = nil
        }

        public init?(intValue: Int) {
            self.stringValue = intValue.description
            self.intValue = intValue
        }

        public init(stringLiteral: String) {
            self.stringValue = stringLiteral
            self.intValue = nil
        }

    }
}
#endif
