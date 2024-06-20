// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.swift_modules.SwiftEdgeCases in swift_edge_cases.proto
import Wire

public struct SwiftEdgeCases {

    @ProtoDefaulted
    public var `return`: String?
    public var error: SwiftEdgeCases.Error?
    public var type: SwiftEdgeCases.Type_?
    public var unknownFields: UnknownFields = .init()

    public init(configure: (inout Self) -> Swift.Void = { _ in }) {
        configure(&self)
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension SwiftEdgeCases : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension SwiftEdgeCases : Hashable {
}
#endif

extension SwiftEdgeCases : Sendable {
}

extension SwiftEdgeCases : ProtoDefaultedValue {

    public static var defaultedValue: Self {
        .init()
    }
}

extension SwiftEdgeCases : ProtoMessage {

    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.kotlin.swift_modules.SwiftEdgeCases"
    }

}

extension SwiftEdgeCases : Proto2Codable {

    public init(from protoReader: ProtoReader) throws {
        var `return`: String? = nil
        var error: SwiftEdgeCases.Error? = nil
        var type: SwiftEdgeCases.Type_? = nil

        let token = try protoReader.beginMessage()
        while let tag = try protoReader.nextTag(token: token) {
            switch tag {
            case 1: `return` = try protoReader.decode(String.self)
            case 2: error = try protoReader.decode(SwiftEdgeCases.Error.self)
            case 3: type = try protoReader.decode(SwiftEdgeCases.Type_.self)
            default: try protoReader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try protoReader.endMessage(token: token)

        self._return.wrappedValue = `return`
        self.error = error
        self.type = type
    }

    public func encode(to protoWriter: ProtoWriter) throws {
        try protoWriter.encode(tag: 1, value: self.`return`)
        try protoWriter.encode(tag: 2, value: self.error)
        try protoWriter.encode(tag: 3, value: self.type)
        try protoWriter.writeUnknownFields(unknownFields)
    }

}

#if !WIRE_REMOVE_CODABLE
extension SwiftEdgeCases : Codable {

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: StringLiteralCodingKeys.self)
        self._return.wrappedValue = try container.decodeIfPresent(String.self, forKey: "return")
        self.error = try container.decodeIfPresent(SwiftEdgeCases.Error.self, forKey: "error")
        self.type = try container.decodeIfPresent(SwiftEdgeCases.Type_.self, forKey: "type")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringLiteralCodingKeys.self)

        try container.encodeIfPresent(self.`return`, forKey: "return")
        try container.encodeIfPresent(self.error, forKey: "error")
        try container.encodeIfPresent(self.type, forKey: "type")
    }

}
#endif

/**
 * Subtypes within SwiftEdgeCases
 */
extension SwiftEdgeCases {

    public enum Error : Int32, CaseIterable, Proto2Enum {

        case UNKNOWN = 0
        case INNER_BAD_VALUE = 1

        public var description: String {
            switch self {
            case .UNKNOWN: return "UNKNOWN"
            case .INNER_BAD_VALUE: return "INNER_BAD_VALUE"
            }
        }

    }

    public enum Type_ : Int32, CaseIterable, Proto2Enum {

        case INACTIVE = 0
        case ACTIVE = 1

        public var description: String {
            switch self {
            case .INACTIVE: return "INACTIVE"
            case .ACTIVE: return "ACTIVE"
            }
        }

    }

}

extension SwiftEdgeCases.Error : Sendable {
}

extension SwiftEdgeCases.Type_ : Sendable {
}
