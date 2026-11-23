/*
 * Copyright (C) 2026 Square, Inc.
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
import Wire
import XCTest
@testable import WireTests

/// Covers fields and oneofs named `self`, `Self`, `Type`, `Protocol` and `Any`. They keep their proto names, with backticks.
final class ReservedFieldNamesTests : XCTestCase {
    private func binaryRoundTrip<T: ProtoCodable & Equatable>(_ message: T) throws -> T {
        let data = try ProtoEncoder().encode(message)
        let decoded = try ProtoDecoder().decode(T.self, from: data)
        XCTAssertEqual(decoded, message)
        return decoded
    }

    private func jsonString<T: Encodable>(_ message: T) throws -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        return String(data: try encoder.encode(message), encoding: .utf8)!
    }

    private func jsonRoundTrip<T: Codable & Equatable>(_ message: T) throws -> String {
        let json = try JSONEncoder().encode(message)
        XCTAssertEqual(try JSONDecoder().decode(T.self, from: json), message)
        return try jsonString(message)
    }

    func testFieldsNamedLikeSwiftKeywords() throws {
        let message = SwiftFieldNames {
            $0.`self` = "a"
            $0.`Self` = "b"
            $0.`Type` = "c"
            $0.`Protocol` = "d"
            $0.`Any` = "e"
            $0.`init` = ["f", "g"]
        }

        XCTAssertEqual(message.`self`, "a")
        XCTAssertEqual(message.`Self`, "b")
        XCTAssertEqual(message.`Type`, "c")
        XCTAssertEqual(message.`Protocol`, "d")
        XCTAssertEqual(message.`Any`, "e")
        XCTAssertEqual(message.`init`, ["f", "g"])

        let decoded = try binaryRoundTrip(message)
        XCTAssertEqual(decoded.`Protocol`, "d")
        XCTAssertEqual(
            try jsonRoundTrip(message),
            #"{"Any":"e","Protocol":"d","Self":"b","Type":"c","init":["f","g"],"self":"a"}"#
        )
    }

    func testUnsetFieldsNamedLikeSwiftKeywordsAreNil() {
        let message = SwiftFieldNames()

        XCTAssertNil(message.`self`)
        XCTAssertNil(message.`Protocol`)
        XCTAssertNil(message.`Any`)
        XCTAssertEqual(message.$self, "")
        XCTAssertEqual(message.`init`, [])
    }

    func testRequiredFieldsNamedLikeSwiftKeywords() throws {
        let message = SwiftRequiredFieldNames(self: "a", Protocol: "b", Any: "c")

        XCTAssertEqual(message.`self`, "a")
        XCTAssertEqual(message.`Protocol`, "b")
        XCTAssertEqual(message.`Any`, "c")
        _ = try binaryRoundTrip(message)
        XCTAssertEqual(try jsonRoundTrip(message), #"{"Any":"c","Protocol":"b","self":"a"}"#)
    }

    func testMissingRequiredFieldNamedSelfThrows() {
        XCTAssertThrowsError(try ProtoDecoder().decode(SwiftRequiredFieldNames.self, from: Data()))
    }

    func testOneofNamedSelf() throws {
        let message = SwiftOneofSelf { $0.`self` = .a("x") }
        let other = SwiftOneofSelf { $0.`self` = SwiftOneofSelf.`Self`.b(3) }

        XCTAssertEqual(try binaryRoundTrip(message).`self`, .a("x"))
        XCTAssertEqual(try binaryRoundTrip(other).`self`, .b(3))
        XCTAssertEqual(try jsonRoundTrip(message), #"{"a":"x"}"#)
        XCTAssertEqual(try jsonRoundTrip(other), #"{"b":3}"#)
        XCTAssertNil(SwiftOneofSelf().`self`)
        XCTAssertEqual(SwiftOneofSelf.defaultedValue, SwiftOneofSelf())
    }

    func testOneofNamedSelfWithCapitalLetter() throws {
        let message = SwiftOneofSelfUpper { $0.`Self` = .b(4) }

        XCTAssertEqual(try binaryRoundTrip(message).`Self`, .b(4))
        XCTAssertEqual(try jsonRoundTrip(message), #"{"b":4}"#)
    }

    func testOneofNamedProtocolWithCapitalLetter() throws {
        let message = SwiftOneofProtocolUpper { $0.`Protocol` = .a("p") }

        XCTAssertEqual(try binaryRoundTrip(message).`Protocol`, .a("p"))
        XCTAssertEqual(try jsonRoundTrip(message), #"{"a":"p"}"#)
    }

    func testOneofNamedTypeWithCapitalLetter() throws {
        let message = SwiftOneofTypeUpper { $0.`Type` = .b(5) }

        XCTAssertEqual(try binaryRoundTrip(message).`Type`, .b(5))
        XCTAssertEqual(try jsonRoundTrip(message), #"{"b":5}"#)
    }

    func testOneofNamedAny() throws {
        let message = SwiftOneofAny { $0.any = .a("y") }

        XCTAssertEqual(try binaryRoundTrip(message).any, .a("y"))
        XCTAssertEqual(try jsonRoundTrip(message), #"{"a":"y"}"#)
    }

    func testOneofCasesNamedLikeSwiftKeywords() throws {
        let cases: [(SwiftOneofFieldNames.Choice, String)] = [
            (.`self`("a"), #"{"self":"a"}"#),
            (.`Protocol`(2), #"{"Protocol":2}"#),
            (.`Any`(true), #"{"Any":true}"#),
            (.`Type`("c"), #"{"Type":"c"}"#),
            (.`Self`("d"), #"{"Self":"d"}"#),
        ]

        for (choice, json) in cases {
            let message = SwiftOneofFieldNames { $0.choice = choice }
            XCTAssertEqual(try binaryRoundTrip(message).choice, choice)
            XCTAssertEqual(try jsonRoundTrip(message), json)
        }
    }

    func testOneofNamedSelfHidesSelfInNestedTypes() throws {
        let message = SwiftSelfNested {
            $0.`self` = .x(SwiftSelfNested.Inner { $0.a = "in" })
            $0.s = SwiftSelfNested.Self_ { $0.a = "s" }
            $0.i = SwiftSelfNested.Inner { $0.a = "i" }
        }

        let decoded = try binaryRoundTrip(message)
        XCTAssertEqual(decoded.s?.a, "s")
        XCTAssertEqual(decoded.`self`, .x(SwiftSelfNested.Inner { $0.a = "in" }))
        _ = try jsonRoundTrip(message)
        XCTAssertEqual(SwiftSelfNested.Self_.defaultedValue, SwiftSelfNested.Self_())
    }

    func testNestedTypeNamedAny() throws {
        let message = SwiftNestedAny { $0.value = SwiftNestedAny.`Any` { $0.a = "v" } }

        XCTAssertEqual(try binaryRoundTrip(message).value?.a, "v")
        XCTAssertEqual(try jsonRoundTrip(message), #"{"value":{"a":"v"}}"#)
    }

    func testRedactedNamesAreHiddenInTheDescription() throws {
        let message = SwiftRedactedNames {
            $0.`self` = "secret-self"
            $0.`Protocol` = "secret-protocol"
            $0.plain = "visible"
            $0.`Any` = .x("secret-any")
        }

        let description = message.description

        XCTAssertFalse(description.contains("secret-self"), description)
        XCTAssertFalse(description.contains("secret-protocol"), description)
        XCTAssertFalse(description.contains("secret-any"), description)
        XCTAssertTrue(description.contains("visible"), description)
        _ = try binaryRoundTrip(message)
        XCTAssertEqual(SwiftRedactedNames.RedactedKeys(rawValue: "self"), .`self`)
        XCTAssertEqual(SwiftRedactedNames.RedactedKeys(rawValue: "Protocol"), .`Protocol`)
    }

    func testHeapAllocatedMessageWithAOneofNamedSelf() throws {
        let message = SwiftHeapNames(Protocol: "p", Any: "required") {
            $0.`self` = .b(9)
            $0.f16 = "last"
        }

        XCTAssertEqual(message.`Any`, "required")
        let decoded = try binaryRoundTrip(message)
        XCTAssertEqual(decoded.`self`, .b(9))
        XCTAssertEqual(decoded.`Protocol`, "p")
        XCTAssertEqual(decoded.f16, "last")
        let json = try jsonRoundTrip(message)
        XCTAssertTrue(json.contains(#""Any":"required""#), json)
        XCTAssertTrue(json.contains(#""Protocol":"p""#), json)
        XCTAssertTrue(json.contains(#""b":9"#), json)
    }

    func testHeapAllocatedMessageWithAFieldNamedSelf() throws {
        let message = SwiftHeapFieldNames(self: "s") {
            $0.`Protocol` = "p"
        }

        XCTAssertEqual(message.`self`, "s")
        XCTAssertEqual(try binaryRoundTrip(message).`Protocol`, "p")
        _ = try jsonRoundTrip(message)
    }

    func testExtensionFieldsNamedLikeSwiftKeywords() throws {
        let message = SwiftExtensibleNames {
            $0.`self` = "s"
            $0.`Protocol` = "p"
        }

        XCTAssertEqual(message.`Any`, nil)
        XCTAssertEqual(SwiftExtensibleNames.default_Any, "d")
        XCTAssertEqual(SwiftExtensibleNames.fieldNumber_Protocol, 102)
        let decoded = try binaryRoundTrip(message)
        XCTAssertEqual(decoded.`self`, "s")
        XCTAssertEqual(decoded.`Protocol`, "p")
    }

    func testProto3FieldsNamedLikeSwiftKeywords() throws {
        let message = SwiftProto3FieldNames(self: "a", Self: "b", Type: "c", Protocol: "d", Any: "e") {
            $0.optional_protocol = "f"
            $0.`init` = ["g"]
        }

        XCTAssertEqual(try binaryRoundTrip(message).`Protocol`, "d")
        XCTAssertEqual(
            try jsonRoundTrip(message),
            #"{"Any":"e","Protocol":"d","Self":"b","Type":"c","init":["g"],"optionalProtocol":"f","self":"a"}"#
        )
    }

    func testProto3OneofsNamedLikeSwiftKeywords() throws {
        let message = SwiftProto3OneofNames {
            $0.`self` = .b(1)
            $0.`Protocol` = .c("c")
            $0.any = .f(6)
        }

        let decoded = try binaryRoundTrip(message)
        XCTAssertEqual(decoded.`self`, .b(1))
        XCTAssertEqual(decoded.`Protocol`, .c("c"))
        XCTAssertEqual(decoded.any, .f(6))
        XCTAssertEqual(try jsonRoundTrip(message), #"{"b":1,"c":"c","f":6}"#)
    }
}
