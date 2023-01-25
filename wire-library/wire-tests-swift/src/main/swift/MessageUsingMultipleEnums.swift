// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.MessageUsingMultipleEnums in same_name_enum.proto
import Foundation
import Wire

/**
 * Enum names must be fully qualified in generated Kotlin
 */
public struct MessageUsingMultipleEnums {

    public var a: MessageWithStatus.Status?
    public var b: OtherMessageWithStatus.Status?
    public var unknownFields: Data = .init()

    public init(a: MessageWithStatus.Status? = nil, b: OtherMessageWithStatus.Status? = nil) {
        self.a = a
        self.b = b
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension MessageUsingMultipleEnums : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension MessageUsingMultipleEnums : Hashable {
}
#endif

#if swift(>=5.5)
extension MessageUsingMultipleEnums : Sendable {
}
#endif

extension MessageUsingMultipleEnums : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.kotlin.MessageUsingMultipleEnums"
    }
}

extension MessageUsingMultipleEnums : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var a: MessageWithStatus.Status? = nil
        var b: OtherMessageWithStatus.Status? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: a = try reader.decode(MessageWithStatus.Status.self)
            case 2: b = try reader.decode(OtherMessageWithStatus.Status.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.a = a
        self.b = b
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.a)
        try writer.encode(tag: 2, value: self.b)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension MessageUsingMultipleEnums : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: MessageUsingMultipleEnums.CodingKeys.self)
        self.a = try container.decodeIfPresent(MessageWithStatus.Status.self, forKey: "a")
        self.b = try container.decodeIfPresent(OtherMessageWithStatus.Status.self, forKey: "b")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: MessageUsingMultipleEnums.CodingKeys.self)
        let includeDefaults = encoder.protoDefaultValuesEncodingStrategy == .include

        if includeDefaults || self.a != nil {
            try container.encode(self.a, forKey: "a")
        }
        if includeDefaults || self.b != nil {
            try container.encode(self.b, forKey: "b")
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
