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
@testable import WireTests

final class JsonLitmusTest : XCTestCase {
  func testSimpleRoundtrip() {
    let expectedPerson = Person(
      name: "Luke Skywalker",
      id: 42,
      email: "luke@skywalker.net",
      phone: [.init(number: "800-555-1234", type: .WORK)],
      aliases: ["Nerfherder"]
    )
    let expectedJson = """
    {\
    "email":"luke@skywalker.net",\
    "id":42,\
    "phone":[{"number":"800-555-1234","type":2}],\
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
}
