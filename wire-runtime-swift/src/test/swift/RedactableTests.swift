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
