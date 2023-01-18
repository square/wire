// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.DeprecatedProto in deprecated.proto
import Foundation
import Wire

public struct DeprecatedProto {

    @available(*, deprecated)
    public var foo: String?
    public var unknownFields: Data = .init()

    public init(foo: String? = nil) {
        self.foo = foo
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension DeprecatedProto : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension DeprecatedProto : Hashable {
}
#endif

#if swift(>=5.5)
extension DeprecatedProto : Sendable {
}
#endif

extension DeprecatedProto : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.kotlin.DeprecatedProto"
    }
}

extension DeprecatedProto : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var foo: String? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: foo = try reader.decode(String.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.foo = foo
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.foo)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension DeprecatedProto : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DeprecatedProto.CodingKeys.self)
        self.foo = try container.decodeIfPresent(String.self, forKey: "foo")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: DeprecatedProto.CodingKeys.self)
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.foo != nil {
            try container.encode(self.foo, forKey: "foo")
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
