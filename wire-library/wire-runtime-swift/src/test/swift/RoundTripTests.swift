//
//  Created by Eric Firestone on 6/19/20.
//

import Foundation
import XCTest
@testable import Wire

final class RoundTripTests: XCTestCase {

    func testPersonEncodeDecode() throws {
        let person = Person(
            name: "Luke Skywalker",
            id: 42,
            email: "luke@skywalker.net",
            phone: [.init(number: "800-555-1234", type: .WORK)],
            aliases: ["Nerfherder"]
        )

        let encoder = ProtoEncoder()
        let data = try encoder.encode(person)

        let decoder = ProtoDecoder()
        let decodedPerson = try decoder.decode(Person.self, from: data)

        XCTAssertEqual(decodedPerson, person)
    }

}
