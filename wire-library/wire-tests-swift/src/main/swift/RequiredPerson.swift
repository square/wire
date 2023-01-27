// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.person.RequiredPerson in person.proto
import Foundation
import Wire

public struct RequiredPerson {

    public var person: Person
    public var unknownFields: Data = .init()

    public init(person: Person) {
        self.person = person
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension RequiredPerson : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension RequiredPerson : Hashable {
}
#endif

#if swift(>=5.5)
extension RequiredPerson : Sendable {
}
#endif

extension RequiredPerson : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.kotlin.person.RequiredPerson"
    }
}

extension RequiredPerson : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var person: Person? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: person = try reader.decode(Person.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.person = try RequiredPerson.checkIfMissing(person, "person")
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.person)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension RequiredPerson : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: StringLiteralCodingKeys.self)
        self.person = try container.decode(Person.self, forKey: "person")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: StringLiteralCodingKeys.self)

        try container.encode(self.person, forKey: "person")
    }
}
#endif