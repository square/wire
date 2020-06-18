//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation
import WireRuntime

public struct Person : Equatable, ProtoCodable {

    // MARK: - Properties

    public var name: String
    public var id: Int32
    public var email: String?

    // MARK: - Init

    public init(
        name: String,
        id: Int32,
        email: String? = nil
    ) {
        self.name = name
        self.id = id
        self.email = email
    }

    // MARK: - ProtoDecodable

    public init(from reader: ProtoReader) throws {
        var name: String?
        var id: Int32?
        var email: String?

        try reader.forEachTag { tag in
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
    }

    // MARK: - ProtoEncodable

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: name)
        try writer.encode(tag: 2, value: id)
        try writer.encode(tag: 3, value: email)
    }

}
