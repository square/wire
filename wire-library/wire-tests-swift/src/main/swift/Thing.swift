// Code generated by Wire protocol buffer compiler, do not edit.
// Source: com.squareup.wire.protos.kotlin.map.Thing in map.proto
import Foundation
import Wire

public struct Thing {

    public var name: String?
    public var unknownFields: Data = .init()

    public init(name: String? = nil) {
        self.name = name
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension Thing : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension Thing : Hashable {
}
#endif

#if swift(>=5.5)
extension Thing : Sendable {
}
#endif

extension Thing : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/com.squareup.wire.protos.kotlin.map.Thing"
    }
}

extension Thing : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var name: String? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: name = try reader.decode(String.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.name = name
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.name)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension Thing : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: Thing.CodingKeys.self)
        self.name = try container.decodeIfPresent(String.self, forKey: "name")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: Thing.CodingKeys.self)
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.name != nil {
            try container.encode(self.name, forKey: "name")
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
