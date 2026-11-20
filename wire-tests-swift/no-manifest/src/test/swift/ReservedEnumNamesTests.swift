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

/// Covers enum constants named `self`, `init`, `Type` and `Protocol`.
final class ReservedEnumNamesTests : XCTestCase {
    func testConstantsKeepTheirRawValuesAndProtoNames() {
        XCTAssertEqual(SwiftReservedEnum.allCases, [.`self`, .`init`, .`Type`, .`Protocol`])
        XCTAssertEqual(SwiftReservedEnum.allCases.map { $0.rawValue }, [0, 1, 2, 3])
        XCTAssertEqual(SwiftReservedEnum.allCases.map { $0.description }, ["self", "init", "Type", "Protocol"])
    }

    func testUnsetFieldsFallBackOnTheirDeclaredDefaults() {
        let message = SwiftReservedEnumNames()

        XCTAssertNil(message.default_self)
        XCTAssertEqual(message.$default_self, .`self`)
        XCTAssertNil(message.default_init)
        XCTAssertEqual(message.$default_init, .`init`)
        XCTAssertNil(message.default_type)
        XCTAssertEqual(message.$default_type, .`Type`)
        XCTAssertNil(message.default_protocol)
        XCTAssertEqual(message.$default_protocol, .`Protocol`)
        XCTAssertNil(message.plain)
        XCTAssertEqual(message.list, [])
    }

    func testBinaryRoundTrip() throws {
        let message = SwiftReservedEnumNames {
            $0.default_self = .`Protocol`
            $0.default_init = .`Type`
            $0.default_type = .`init`
            $0.default_protocol = .`self`
            $0.plain = .`Type`
            $0.list = [.`Protocol`, .`self`, .`init`, .`Type`]
        }

        let data = try ProtoEncoder().encode(message)
        let decoded = try ProtoDecoder().decode(SwiftReservedEnumNames.self, from: data)

        XCTAssertEqual(decoded, message)
        XCTAssertEqual(decoded.list, [.`Protocol`, .`self`, .`init`, .`Type`])
    }

    func testJsonUsesTheProtoNames() throws {
        let message = SwiftReservedEnumNames {
            $0.default_self = .`Protocol`
            $0.default_init = .`Type`
            $0.default_type = .`init`
            $0.default_protocol = .`self`
            $0.plain = .`Type`
            $0.list = [.`Protocol`, .`self`]
        }
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys

        let json = try encoder.encode(message)

        let expected = """
        {\
        "defaultInit":"Type",\
        "defaultProtocol":"self",\
        "defaultSelf":"Protocol",\
        "defaultType":"init",\
        "list":["Protocol","self"],\
        "plain":"Type"\
        }
        """
        XCTAssertEqual(String(data: json, encoding: .utf8), expected)
        XCTAssertEqual(try JSONDecoder().decode(SwiftReservedEnumNames.self, from: json), message)
    }

    func testJsonDecodesEveryConstantByProtoName() throws {
        let json = Data(#"{"list":["self","init","Type","Protocol"]}"#.utf8)

        let decoded = try JSONDecoder().decode(SwiftReservedEnumNames.self, from: json)

        XCTAssertEqual(decoded.list, [.`self`, .`init`, .`Type`, .`Protocol`])
    }
}
