// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.OptionalEnumUser in optional_enum.proto
import Foundation
import Wire

public struct OptionalEnumUser {

    public var optional_enum: OptionalEnumUser.OptionalEnum?
    public var unknownFields: Foundation.Data = .init()

    public init(configure: (inout Self) -> Swift.Void = { _ in }) {
        configure(&self)
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension OptionalEnumUser : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension OptionalEnumUser : Hashable {
}
#endif

extension OptionalEnumUser : Sendable {
}

extension OptionalEnumUser : ProtoDefaultedValue {

    public static var defaultedValue: OptionalEnumUser {
        OptionalEnumUser()
    }
}

extension OptionalEnumUser : ProtoMessage {

    public static func protoMessageTypeURL() -> Swift.String {
        return "type.googleapis.com/squareup.protos.kotlin.OptionalEnumUser"
    }

}

extension OptionalEnumUser : Proto2Codable {

    public init(from protoReader: Wire.ProtoReader) throws {
        var optional_enum: OptionalEnumUser.OptionalEnum? = nil

        let token = try protoReader.beginMessage()
        while let tag = try protoReader.nextTag(token: token) {
            switch tag {
            case 1: optional_enum = try protoReader.decode(OptionalEnumUser.OptionalEnum.self)
            default: try protoReader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try protoReader.endMessage(token: token)

        self.optional_enum = optional_enum
    }

    public func encode(to protoWriter: Wire.ProtoWriter) throws {
        try protoWriter.encode(tag: 1, value: self.optional_enum)
        try protoWriter.writeUnknownFields(unknownFields)
    }

}

#if !WIRE_REMOVE_CODABLE
extension OptionalEnumUser : Codable {

    public init(from decoder: Swift.Decoder) throws {
        let container = try decoder.container(keyedBy: Wire.StringLiteralCodingKeys.self)
        self.optional_enum = try container.decodeIfPresent(OptionalEnumUser.OptionalEnum.self, firstOfKeys: "optionalEnum", "optional_enum")
    }

    public func encode(to encoder: Swift.Encoder) throws {
        var container = encoder.container(keyedBy: Wire.StringLiteralCodingKeys.self)
        let preferCamelCase = encoder.protoKeyNameEncodingStrategy == .camelCase

        try container.encodeIfPresent(self.optional_enum, forKey: preferCamelCase ? "optionalEnum" : "optional_enum")
    }

}
#endif

/**
 * Subtypes within OptionalEnumUser
 */
extension OptionalEnumUser {

    public enum OptionalEnum : Swift.Int32, Swift.CaseIterable, Wire.ProtoEnum {

        case FOO = 1
        case BAR = 2

        public var description: Swift.String {
            switch self {
            case .FOO: return "FOO"
            case .BAR: return "BAR"
            }
        }

    }

}

extension OptionalEnumUser.OptionalEnum : Sendable {
}
