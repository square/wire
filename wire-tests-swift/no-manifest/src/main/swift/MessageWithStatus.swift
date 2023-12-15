// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.MessageWithStatus in same_name_enum.proto
import Foundation
import Wire

public struct MessageWithStatus {

    public var unknownFields: Foundation.Data = .init()

    public init() {
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension MessageWithStatus : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension MessageWithStatus : Hashable {
}
#endif

extension MessageWithStatus : Sendable {
}

extension MessageWithStatus : ProtoDefaultedValue {

    public static var defaultedValue: MessageWithStatus {
        MessageWithStatus()
    }
}

extension MessageWithStatus : ProtoMessage {

    public static func protoMessageTypeURL() -> Swift.String {
        return "type.googleapis.com/squareup.protos.kotlin.MessageWithStatus"
    }

}

extension MessageWithStatus : Proto2Codable {

    public init(from protoReader: Wire.ProtoReader) throws {
        let token = try protoReader.beginMessage()
        while let tag = try protoReader.nextTag(token: token) {
            switch tag {
            default: try protoReader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try protoReader.endMessage(token: token)

    }

    public func encode(to protoWriter: Wire.ProtoWriter) throws {
        try protoWriter.writeUnknownFields(unknownFields)
    }

}

#if !WIRE_REMOVE_CODABLE
extension MessageWithStatus : Codable {

    public enum CodingKeys : Swift.CodingKey {
    }

}
#endif

/**
 * Subtypes within MessageWithStatus
 */
extension MessageWithStatus {

    public enum Status : Swift.Int32, Swift.CaseIterable, Wire.ProtoEnum {

        case A = 1

        public var description: Swift.String {
            switch self {
            case .A: return "A"
            }
        }

    }

}

extension MessageWithStatus.Status : Sendable {
}
