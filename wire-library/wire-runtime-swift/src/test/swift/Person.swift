//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation
import WireRuntime

public struct Person : Equatable, ProtoEncodable {

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

    // MARK: - ProtoEncodable

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: name)
        try writer.encode(tag: 2, value: id)
        try writer.encode(tag: 3, value: email)
    }

}