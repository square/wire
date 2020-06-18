//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation
import Wire

public struct Person : Equatable, Proto2Codable {

    // MARK: - Properties

    public var name: String
    public var id: Int32
    public var email: String?

    public let unknownFields: Data

    // MARK: - Init

    public init(
        name: String,
        id: Int32,
        email: String? = nil
    ) {
        self.name = name
        self.id = id
        self.email = email
        self.unknownFields = .init()
    }

    // MARK: - ProtoDecodable

    public init(from reader: ProtoReader) throws {
        var name: String?
        var id: Int32?
        var email: String?

        let unknownFields = try reader.forEachTag { tag in
            switch tag {
            case 1: name = try reader.decode(String.self)
            case 2: id = try reader.decode(Int32.self)
            case 3: email = try reader.decode(String.self)
            default: fatalError("Unknown tag")
            }
        }

        self.name = try Person.checkIfMissing(name, "name")
        self.id = try Person.checkIfMissing(id, "id")
        self.email = email
        self.unknownFields = unknownFields
    }

    // MARK: - ProtoEncodable

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: name)
        try writer.encode(tag: 2, value: id)
        try writer.encode(tag: 3, value: email)
    }

    // MARK: -

    public enum PhoneType: UInt32, CaseIterable {
        case MOBILE = 0
        case HOME = 1
        case WORK = 2
    }

    // MARK: -

    public struct PhoneNumber : Equatable, Proto2Codable {

        // MARK: - Properties

        public var number: String
        public var type: PhoneType?

        public let unknownFields: Data

        // MARK: - Init

        public init(
            number: String,
            type: PhoneType? = nil
        ) {
            self.number = number
            self.type = type
            self.unknownFields = .init()
        }

        // MARK: - ProtoDecodable

        public init(from reader: ProtoReader) throws {
            var number: String?
            var type: PhoneType?
            let unknownFields = try reader.forEachTag { tag in
                switch tag {
                case 1: number = try reader.decode(String.self)
                case 2: type = try reader.decode(PhoneType.self)
                default: try reader.readUnknownField(tag: tag)
                }
            }

            self.number = try Person.checkIfMissing(number, "number")
            self.type = type
            self.unknownFields = unknownFields
        }

        // MARK: - ProtoEncodable

        public func encode(to writer: ProtoWriter) throws {
            try writer.encode(tag: 1, value: number)
            try writer.encode(tag: 2, value: type)
        }

    }

}
