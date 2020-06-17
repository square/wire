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
    "phone":[{"number":"800-555-1234","type":2,"unknownFields":""}],\
    "unknownFields":"",\
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
