/*
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Foundation
import XCTest
@testable import WireTests

final class JsonLitmusTest : XCTestCase {
    func testSimpleRoundtrip() {
        let expectedPerson = Person(
            id: 42,
            name: "Luke Skywalker"
        ) {
            $0.email = "luke@skywalker.net"
            $0.phone = [
                Person.PhoneNumber(number: "800-555-1234") { $0.type = .WORK },
            ]
            $0.aliases = ["Nerfherder"]
        }
        let expectedJson = """
        {\
        "email":"luke@skywalker.net",\
        "id":42,\
        "phone":[{"number":"800-555-1234","type":"WORK"}],\
        "name":"Luke Skywalker",\
        "aliases":["Nerfherder"]\
        }
        """

        let jsonData = try! JSONEncoder().encode(expectedPerson)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        let actualPerson = try! JSONDecoder().decode(Person.self, from: jsonData)
        XCTAssertEqual(expectedPerson, actualPerson)
    }

    func testCopyOnWrite() {
        let original = AllTypes(
            req_int32: 0,
            req_uint32: 1,
            req_sint32: 2,
            req_fixed32: 3,
            req_sfixed32: 4,
            req_int64: 5,
            req_uint64: 6,
            req_sint64: 7,
            req_fixed64: 8,
            req_sfixed64: 9,
            req_bool: true,
            req_float: 0,
            req_double: 1,
            req_string: "Hello",
            req_bytes: Data(),
            req_nested_enum: .A,
            req_nested_message: .init()
        )

        XCTAssertEqual(original.opt_bool, nil)

        var copy = original
        copy.opt_bool = true

        XCTAssertEqual(original.opt_bool, nil)
        XCTAssertEqual(copy.opt_bool, true)
    }
}
