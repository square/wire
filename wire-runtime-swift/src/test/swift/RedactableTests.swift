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
@testable import Wire

final class RedactableTests: XCTestCase {

    func testRedactedField() {
        let redacted = Redacted(name: "Foo")
        XCTAssertEqual(
            redacted.description,
            "Redacted(name: <redacted>, nested: nil, choice: nil, unknownFields: [:])"
        )
    }

    func testRedactedOneOf() {
        let redacted1 = Redacted(name: "Foo") {
            $0.choice = .yes("yes")
        }
        XCTAssertEqual(
            redacted1.description,
            "Redacted(name: <redacted>, nested: nil, choice: Optional(Choice(yes: \"yes\")), unknownFields: [:])"
        )
        let redacted2 = Redacted(name: "Foo") {
            $0.choice = .no("no")
        }
        XCTAssertEqual(
            redacted2.description,
            "Redacted(name: <redacted>, nested: nil, choice: Optional(Choice(no: <redacted>)), unknownFields: [:])"
        )
    }

    func testRedactedFieldWithNilValue() {
        let redacted = Redacted2(name: "foo")
        XCTAssertEqual(
            redacted.description,
            // The *_redacted fields should show as `nil` and not redacted since `nil`
            // is still valuable data and can't expose sensitive information.
            "Redacted2(name: \"foo\", fully_redacted: nil, partially_redacted: nil, unknownFields: [:])"
        )
    }

    func testNestedRedactedField() {
        let redacted = Redacted2(name: "foo") {
            $0.partially_redacted = Redacted3(name: "bar") {
                $0.enabled = true
            }
        }
        XCTAssertEqual(
            redacted.description,
            "Redacted2(name: \"foo\", fully_redacted: nil, partially_redacted: Optional(Redacted3(name: \"bar\", enabled: <redacted>, unknownFields: [:])), unknownFields: [:])"
        )
    }

    func testHeapStorageRedaction() {
        XCTAssertEqual(
            RedactedLargeMessage.Storage.RedactedKeys.a,
            RedactedLargeMessage.RedactedKeys.a
        )
    }

    func testLargeMessageRedactedUnsafeNameField() {
        let redacted = RedactedLargeMessage {
            $0.description_ = "foo"
        }
        XCTAssertEqual(
            redacted.description,
            "Storage(a: <redacted>, b: ProtoDefaulted<String>(wrappedValue: nil), c: ProtoDefaulted<String>(wrappedValue: nil), d: ProtoDefaulted<String>(wrappedValue: nil), e: ProtoDefaulted<String>(wrappedValue: nil), f: ProtoDefaulted<String>(wrappedValue: nil), g: ProtoDefaulted<String>(wrappedValue: nil), h: ProtoDefaulted<String>(wrappedValue: nil), i: ProtoDefaulted<String>(wrappedValue: nil), j: ProtoDefaulted<String>(wrappedValue: nil), k: ProtoDefaulted<String>(wrappedValue: nil), l: ProtoDefaulted<String>(wrappedValue: nil), m: ProtoDefaulted<String>(wrappedValue: nil), n: ProtoDefaulted<String>(wrappedValue: nil), o: ProtoDefaulted<String>(wrappedValue: nil), p: ProtoDefaulted<String>(wrappedValue: nil), q: ProtoDefaulted<String>(wrappedValue: nil), r: ProtoDefaulted<String>(wrappedValue: nil), s: ProtoDefaulted<String>(wrappedValue: nil), t: ProtoDefaulted<String>(wrappedValue: nil), u: ProtoDefaulted<String>(wrappedValue: nil), v: ProtoDefaulted<String>(wrappedValue: nil), w: ProtoDefaulted<String>(wrappedValue: nil), x: ProtoDefaulted<String>(wrappedValue: nil), y: ProtoDefaulted<String>(wrappedValue: nil), z: ProtoDefaulted<String>(wrappedValue: nil), description: <redacted>, unknownFields: [:])"
        )
    }

    func testLargeMessageRedactedUnsafeOneOfField() {
        let redacted = RedactedLargeMessage.RedactedLargeOneOf() {
            $0.action = .description_("foo")
        }
        XCTAssertEqual(
            redacted.action!.description,
            "Action(description: <redacted>)"
        )
    }
}
