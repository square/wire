// Code generated by Wire protocol buffer compiler, do not edit.
// Source: com.squareup.wire.protos.kotlin.map.MappyTwo in map.proto
import Foundation
import Wire

public struct MappyTwo {

    public var string_enums: [String : MappyTwo.ValueEnum] = [:]
    public var int_things: [Int64 : Thing] = [:]
    public var string_ints: [String : Int64] = [:]
    public var int_things_two: [Int32 : Thing] = [:]
    public var unknownFields: Foundation.Data = .init()

    public init(configure: (inout Self) -> Swift.Void = { _ in }) {
        configure(&self)
    }

}

#if WIRE_INCLUDE_MEMBERWISE_INITIALIZER
extension MappyTwo {

    @_disfavoredOverload
    @available(*, deprecated)
    public init(
        string_enums: [Swift.String : MappyTwo.ValueEnum] = [:],
        int_things: [Swift.Int64 : Thing] = [:],
        string_ints: [Swift.String : Swift.Int64] = [:],
        int_things_two: [Swift.Int32 : Thing] = [:]
    ) {
        self.string_enums = string_enums
        self.int_things = int_things
        self.string_ints = string_ints
        self.int_things_two = int_things_two
    }

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

    public static func protoMessageTypeURL() -> Swift.String {
        return "type.googleapis.com/com.squareup.wire.protos.kotlin.map.MappyTwo"
    }

}

extension MappyTwo : Proto2Codable {

    public init(from protoReader: Wire.ProtoReader) throws {
        var string_enums: [Swift.String : MappyTwo.ValueEnum] = [:]
        var int_things: [Swift.Int64 : Thing] = [:]
        var string_ints: [Swift.String : Swift.Int64] = [:]
        var int_things_two: [Swift.Int32 : Thing] = [:]

        let token = try protoReader.beginMessage()
        while let tag = try protoReader.nextTag(token: token) {
            switch tag {
            case 1: try protoReader.decode(into: &string_enums)
            case 2: try protoReader.decode(into: &int_things, keyEncoding: .signed)
            case 3: try protoReader.decode(into: &string_ints, valueEncoding: .signed)
            case 4: try protoReader.decode(into: &int_things_two, keyEncoding: .signed)
            default: try protoReader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try protoReader.endMessage(token: token)

        self.string_enums = string_enums
        self.int_things = int_things
        self.string_ints = string_ints
        self.int_things_two = int_things_two
    }

    public func encode(to protoWriter: Wire.ProtoWriter) throws {
        try protoWriter.encode(tag: 1, value: self.string_enums)
        try protoWriter.encode(tag: 2, value: self.int_things, keyEncoding: .signed)
        try protoWriter.encode(tag: 3, value: self.string_ints, valueEncoding: .signed)
        try protoWriter.encode(tag: 4, value: self.int_things_two, keyEncoding: .signed)
        try protoWriter.writeUnknownFields(unknownFields)
    }

}

#if !WIRE_REMOVE_CODABLE
extension MappyTwo : Codable {

    public init(from decoder: Swift.Decoder) throws {
        let container = try decoder.container(keyedBy: Wire.StringLiteralCodingKeys.self)
        self.string_enums = try container.decodeProtoMap([Swift.String : MappyTwo.ValueEnum].self, firstOfKeys: "stringEnums", "string_enums")
        self.int_things = try container.decodeProtoMap([Swift.Int64 : Thing].self, firstOfKeys: "intThings", "int_things")
        self.string_ints = try container.decodeProtoMap([Swift.String : Swift.Int64].self, firstOfKeys: "stringInts", "string_ints")
        self.int_things_two = try container.decodeProtoMap([Swift.Int32 : Thing].self, firstOfKeys: "intThingsTwo", "int_things_two")
    }

    public func encode(to encoder: Swift.Encoder) throws {
        var container = encoder.container(keyedBy: Wire.StringLiteralCodingKeys.self)
        let preferCamelCase = encoder.protoKeyNameEncodingStrategy == .camelCase
        let includeDefaults = encoder.protoDefaultValuesEncodingStrategy == .include

        if includeDefaults || !self.string_enums.isEmpty {
            try container.encodeProtoMap(self.string_enums, forKey: preferCamelCase ? "stringEnums" : "string_enums")
        }
        if includeDefaults || !self.int_things.isEmpty {
            try container.encodeProtoMap(self.int_things, forKey: preferCamelCase ? "intThings" : "int_things")
        }
        if includeDefaults || !self.string_ints.isEmpty {
            try container.encodeProtoMap(self.string_ints, forKey: preferCamelCase ? "stringInts" : "string_ints")
        }
        if includeDefaults || !self.int_things_two.isEmpty {
            try container.encodeProtoMap(self.int_things_two, forKey: preferCamelCase ? "intThingsTwo" : "int_things_two")
        }
    }

}
#endif

/**
 * Subtypes within MappyTwo
 */
extension MappyTwo {

    public enum ValueEnum : Swift.UInt32, Swift.CaseIterable, Wire.ProtoEnum {

        case DEFAULT = 0
        case FOO = 1
        case BAR = 2

        public var description: Swift.String {
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
