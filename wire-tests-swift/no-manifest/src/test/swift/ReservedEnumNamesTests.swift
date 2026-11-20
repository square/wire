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
import module_one
import module_two
@testable import WireTests

/// Covers enum constants named `self`, `init`, `Type`, `Protocol`, `Any` and `allCases`, and an enum named `Protocol`.
final class ReservedEnumNamesTests : XCTestCase {
    func testConstantsKeepTheirRawValuesAndProtoNames() {
        // The case named `allCases` makes `SwiftReservedEnum.allCases` ambiguous without a type.
        let all: [SwiftReservedEnum] = SwiftReservedEnum.allCases
        XCTAssertEqual(all, [.`self`, .`init`, .`Type`, .`Protocol`, .`Any`, .allCases])
        XCTAssertEqual(all.map { $0.rawValue }, [0, 1, 2, 3, 4, 5])
        XCTAssertEqual(all.map { $0.description }, ["self", "init", "Type", "Protocol", "Any", "allCases"])
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

    func testJsonDecodesAnyAndAllCasesByProtoName() throws {
        let json = Data(#"{"list":["Any","allCases"]}"#.utf8)

        let decoded = try JSONDecoder().decode(SwiftReservedEnumNames.self, from: json)

        XCTAssertEqual(decoded.list, [.`Any`, .allCases])
    }

    func testNestedEnumNamedProtocolIsSuffixed() throws {
        let message = SwiftReservedTypeNames {
            $0.protocol = .PROTOCOL_ZERO
            $0.protocols = [.PROTOCOL_ONE, .PROTOCOL_ZERO]
        }
        XCTAssertEqual(SwiftReservedTypeNames().$default_protocol, SwiftReservedTypeNames.Protocol_.PROTOCOL_ONE)

        let data = try ProtoEncoder().encode(message)
        let decoded = try ProtoDecoder().decode(SwiftReservedTypeNames.self, from: data)

        XCTAssertEqual(decoded, message)
        XCTAssertEqual(decoded.protocols.map { $0.description }, ["PROTOCOL_ONE", "PROTOCOL_ZERO"])
    }

    func testProto3ZeroConstantsAreTheDefaultedValues() {
        XCTAssertEqual(SwiftProto3TypeZero.defaultedValue, .`Type`)
        XCTAssertEqual(SwiftProto3SelfZero.defaultedValue, .`self`)
        XCTAssertEqual(SwiftProto3InitZero.defaultedValue, .`init`)
        XCTAssertEqual(SwiftProto3ProtocolZero.defaultedValue, .`Protocol`)
        XCTAssertEqual(SwiftProto3AnyZero.defaultedValue, .`Any`)
        XCTAssertEqual(SwiftProto3AllCasesZero.defaultedValue, .allCases_)
        XCTAssertEqual(SwiftProto3SameNameZero.defaultedValue, .SwiftProto3SameNameZero)

        let message = SwiftProto3ReservedEnumNames()
        XCTAssertNil(message.type_zero)
        XCTAssertEqual(message.$type_zero, .`Type`)
        XCTAssertEqual(message.$self_zero, .`self`)
        XCTAssertEqual(message.$init_zero, .`init`)
        XCTAssertEqual(message.$protocol_zero, .`Protocol`)
        XCTAssertEqual(message.$any_zero, .`Any`)
        XCTAssertEqual(message.$all_cases_zero, .allCases_)
        XCTAssertEqual(message.$same_name_zero, .SwiftProto3SameNameZero)
    }

    func testProto3ConstantsKeepTheirProtoNames() throws {
        XCTAssertEqual(SwiftProto3AllCasesZero.allCases_.description, "allCases")
        let all: [SwiftProto3AllCasesZero] = SwiftProto3AllCasesZero.allCases
        XCTAssertEqual(all, [.allCases_, .OTHER_ALL_CASES])

        let message = SwiftProto3ReservedEnumNames {
            $0.type_zero = .OTHER
            $0.self_zero = .OTHER_SELF
            $0.init_zero = .OTHER_INIT
            $0.protocol_zero = .OTHER_PROTOCOL
            $0.any_zero = .OTHER_ANY
            $0.all_cases_zero = .OTHER_ALL_CASES
            $0.same_name_zero = .OTHER_SAME_NAME
            $0.defaulted_value_name = .DEFAULTED_VALUE_ZERO
        }
        let data = try ProtoEncoder().encode(message)
        XCTAssertEqual(try ProtoDecoder().decode(SwiftProto3ReservedEnumNames.self, from: data), message)

        let decoded = try JSONDecoder().decode(
            SwiftProto3ReservedEnumNames.self,
            from: Data(#"{"allCasesZero":"allCases","typeZero":"Type","sameNameZero":"SwiftProto3SameNameZero"}"#.utf8)
        )
        XCTAssertEqual(decoded.all_cases_zero, .allCases_)
        XCTAssertEqual(decoded.type_zero, .`Type`)
        XCTAssertEqual(decoded.same_name_zero, .SwiftProto3SameNameZero)
    }

    func testOneofNamedProtocolIsSuffixed() throws {
        let message = SwiftOneofProtocol { $0.protocol = SwiftOneofProtocol.Protocol_.b(7) }

        let data = try ProtoEncoder().encode(message)
        let decoded = try ProtoDecoder().decode(SwiftOneofProtocol.self, from: data)

        XCTAssertEqual(decoded, message)
        XCTAssertEqual(decoded.protocol, .b(7))
        XCTAssertEqual(try JSONDecoder().decode(SwiftOneofProtocol.self, from: JSONEncoder().encode(message)), message)
    }

    func testOneofNamedTypeKeepsItsEnumName() throws {
        let message = SwiftOneofType { $0.type = .c("x") }

        let data = try ProtoEncoder().encode(message)

        XCTAssertEqual(try ProtoDecoder().decode(SwiftOneofType.self, from: data), message)
    }

    func testDefaultNamedLikeItsEnumInProto2() {
        let message = SwiftSameNameOuter()

        XCTAssertNil(message.top)
        XCTAssertEqual(message.$top, .SwiftSameNameEnum)
        XCTAssertEqual(message.$inner, .SwiftSameNameOuter)
    }

    func testProto3ZeroConstantNamedLikeTheOuterType() throws {
        XCTAssertEqual(SwiftProto3Outer.Kind.defaultedValue, .SwiftProto3Outer)
        XCTAssertEqual(SwiftProto3Outer().$kind, .SwiftProto3Outer)

        let message = SwiftProto3Outer { $0.kind = .KIND_OK }
        let data = try ProtoEncoder().encode(message)
        XCTAssertEqual(try ProtoDecoder().decode(SwiftProto3Outer.self, from: data), message)
    }

    func testRenamedConstantsUseTheProtoNameInJson() throws {
        let message = SwiftProto3ReservedEnumNames {
            $0.type_zero = .OTHER
            $0.self_zero = .OTHER_SELF
            $0.init_zero = .OTHER_INIT
            $0.protocol_zero = .OTHER_PROTOCOL
            $0.any_zero = .OTHER_ANY
            $0.all_cases_zero = .allCases_
            $0.same_name_zero = .OTHER_SAME_NAME
            $0.defaulted_value_name = .defaultedValue_
        }
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys

        let json = try encoder.encode(message)

        let text = String(data: json, encoding: .utf8)!
        XCTAssertTrue(text.contains("\"allCasesZero\":\"allCases\""), text)
        XCTAssertTrue(text.contains("\"defaultedValueName\":\"defaultedValue\""), text)
        XCTAssertFalse(text.contains("allCases_"), text)
        XCTAssertFalse(text.contains("defaultedValue_"), text)
        XCTAssertEqual(try JSONDecoder().decode(SwiftProto3ReservedEnumNames.self, from: json), message)
    }

    func testProto3ConstantNamedDefaultedValueKeepsItsProtoName() {
        XCTAssertEqual(SwiftProto3DefaultedValueName.defaultedValue, .DEFAULTED_VALUE_ZERO)
        XCTAssertEqual(SwiftProto3DefaultedValueName.defaultedValue_.description, "defaultedValue")
        XCTAssertEqual(SwiftProto3DefaultedValueName.defaultedValue_.rawValue, 1)
    }

    func testProto2ConstantNamedDefaultedValueKeepsItsName() {
        XCTAssertEqual(SwiftProto2DefaultedValueName.defaultedValue.description, "defaultedValue")
        XCTAssertEqual(SwiftProto2DefaultedValueName.defaultedValue.rawValue, 0)
    }

    func testMessageNamedProtocolInAnotherModule() throws {
        let message = SwiftModuleTwoMessage {
            $0.protocol = module_one.`Protocol` { $0.name = "n" }
            $0.protocols = [module_one.`Protocol`()]
        }

        let data = try ProtoEncoder().encode(message)
        let decoded = try ProtoDecoder().decode(SwiftModuleTwoMessage.self, from: data)

        XCTAssertEqual(decoded, message)
        XCTAssertEqual(decoded.protocol?.name, "n")
        XCTAssertEqual(try JSONDecoder().decode(SwiftModuleTwoMessage.self, from: JSONEncoder().encode(message)), message)
    }
}
