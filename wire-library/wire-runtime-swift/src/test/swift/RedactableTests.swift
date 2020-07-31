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
            "Redacted(name: <redacted>, nested: nil, unknownFields: 0 bytes)"
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
