// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.tostring.VeryLongProtoNameCausingBrokenLineBreaks in to_string.proto
import Foundation
import Wire

/**
 * https://github.com/square/wire/issues/1125
 */
public struct VeryLongProtoNameCausingBrokenLineBreaks {

    public var foo: String?
    public var unknownFields: Data = .init()

    public init(foo: String? = nil) {
        self.foo = foo
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension VeryLongProtoNameCausingBrokenLineBreaks : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension VeryLongProtoNameCausingBrokenLineBreaks : Hashable {
}
#endif

#if swift(>=5.5)
extension VeryLongProtoNameCausingBrokenLineBreaks : Sendable {
}
#endif

extension VeryLongProtoNameCausingBrokenLineBreaks : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.tostring.VeryLongProtoNameCausingBrokenLineBreaks"
    }
}

extension VeryLongProtoNameCausingBrokenLineBreaks : Proto2Codable {
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
extension VeryLongProtoNameCausingBrokenLineBreaks : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: VeryLongProtoNameCausingBrokenLineBreaks.CodingKeys.self)
        self.foo = try container.decodeIfPresent(String.self, forKey: .foo)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: VeryLongProtoNameCausingBrokenLineBreaks.CodingKeys.self)
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.foo != nil {
            try container.encode(self.foo, forKey: .foo)
        }
    }

    public enum CodingKeys : String, CodingKey {

        case foo

    }
}
#endif
