// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.swift_modules.SwiftModuleOneMessage in swift_module_one.proto
import Foundation
import Wire

public struct SwiftModuleOneMessage {

    public var name: String
    public var unknownFields: Foundation.Data = .init()

    public init(name: String) {
        self.name = name
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension SwiftModuleOneMessage : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension SwiftModuleOneMessage : Hashable {
}
#endif

extension SwiftModuleOneMessage : Sendable {
}

extension SwiftModuleOneMessage : ProtoMessage {

    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.kotlin.swift_modules.SwiftModuleOneMessage"
    }

}

extension SwiftModuleOneMessage : Proto2Codable {

    public init(from protoReader: ProtoReader) throws {
        var name: String? = nil

        let token = try protoReader.beginMessage()
        while let tag = try protoReader.nextTag(token: token) {
            switch tag {
            case 1: name = try protoReader.decode(String.self)
            default: try protoReader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try protoReader.endMessage(token: token)

        self.name = try SwiftModuleOneMessage.checkIfMissing(name, "name")
    }

    public func encode(to protoWriter: ProtoWriter) throws {
        try protoWriter.encode(tag: 1, value: self.name)
        try protoWriter.writeUnknownFields(unknownFields)
    }

}

#if !WIRE_REMOVE_CODABLE
extension SwiftModuleOneMessage : Codable {

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: StringLiteralCodingKeys.self)
        self.name = try container.decode(String.self, forKey: "name")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringLiteralCodingKeys.self)
        let includeDefaults = encoder.protoDefaultValuesEncodingStrategy == .include

        if includeDefaults || !self.name.isEmpty {
            try container.encode(self.name, forKey: "name")
        }
    }

}
#endif
