/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation
import XCTest
@testable import Wire

final class RoundTripTests: XCTestCase {

    func testPersonEncodeDecode() throws {
        let personData = Data(json_data: "")
        let person = Person(
            name: "Luke Skywalker",
            id: 42,
            email: "luke@skywalker.net",
            phone: [.init(number: "800-555-1234", type: .WORK)],
            aliases: ["Nerfherder"],
            data: personData
        )

        let encoder = ProtoEncoder()
        let data = try encoder.encode(person)

        let decoder = ProtoDecoder()
        let decodedPerson = try decoder.decode(Person.self, from: data)

        XCTAssertEqual(decodedPerson, person)
    }

    // ensure that fields set to their identity value survive a roundtrip when omitted over the wire
    func testProto3IdentityValues() throws {
        let empty = EmptyOmitted(
            numeric_value: 0,
            string_value: "",
            bytes_value: Foundation.Data(),
            bool_value: false,
            enum_value: .UNKNOWN,
            message_value: nil,
            repeated_value: [],
            map_value: [:]
        )

        let encoder = ProtoEncoder()
        let data = try encoder.encode(empty)

        let decoder = ProtoDecoder()
        let decodedEmpty = try decoder.decode(EmptyOmitted.self, from: data)

        XCTAssertEqual(decodedEmpty, empty)
    }

}
