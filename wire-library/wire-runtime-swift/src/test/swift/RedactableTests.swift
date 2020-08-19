//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation
import XCTest
@testable import Wire

final class RedactableTests: XCTestCase {

    func testRedactedField() {
        let redacted = Redacted(name: "Foo")
        XCTAssertEqual(
            redacted.description,
            "Redacted(name: <redacted>, nested: nil, choice: nil, unknownFields: 0 bytes)"
        )
    }

    func testRedactedOneOf() {
        let redacted1 = Redacted(name: "Foo", choice: .yes("yes"))
        XCTAssertEqual(
            redacted1.description,
            "Redacted(name: <redacted>, nested: nil, choice: Optional(Choice(yes: \"yes\")), unknownFields: 0 bytes)"
        )
        let redacted2 = Redacted(name: "Foo", choice: .no("no"))
        XCTAssertEqual(
            redacted2.description,
            "Redacted(name: <redacted>, nested: nil, choice: Optional(Choice(no: <redacted>)), unknownFields: 0 bytes)"
        )
    }

    func testRedactedFieldWithNilValue() {
        let redacted = Redacted2(name: "foo")
        XCTAssertEqual(
            redacted.description,
            // The *_redacted fields should show as `nil` and not redacted since `nil`
            // is still valuable data and can't expose sensitive information.
            "Redacted2(name: \"foo\", fully_redacted: nil, partially_redacted: nil, unknownFields: 0 bytes)"
        )
    }

    func testNestedRedactedField() {
        let redacted = Redacted2(
            name: "foo",
            partially_redacted: .init(
                name: "bar",
                enabled: true
            )
        )
        XCTAssertEqual(
            redacted.description,
            "Redacted2(name: \"foo\", fully_redacted: nil, partially_redacted: Optional(Redacted3(name: \"bar\", enabled: <redacted>, unknownFields: 0 bytes)), unknownFields: 0 bytes)"
        )
    }

}
