// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.unknownfields.NestedVersionOne in unknown_fields.proto
import Foundation
import Wire

public struct NestedVersionOne {

    public var i: Int32?
    public var unknownFields: Data = .init()

    public init(i: Int32? = nil) {
        self.i = i
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension NestedVersionOne : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension NestedVersionOne : Hashable {
}
#endif

#if swift(>=5.5)
extension NestedVersionOne : Sendable {
}
#endif

extension NestedVersionOne : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.kotlin.unknownfields.NestedVersionOne"
    }
}

extension NestedVersionOne : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var i: Int32? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: i = try reader.decode(Int32.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.i = i
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.i)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension NestedVersionOne : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: NestedVersionOne.CodingKeys.self)
        self.i = try container.decodeIfPresent(Int32.self, forKey: "i")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: NestedVersionOne.CodingKeys.self)

        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.i != nil {
            try container.encode(self.i, forKey: "i")
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
