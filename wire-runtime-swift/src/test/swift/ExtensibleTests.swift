/*
 * Copyright (C) 2024 Square, Inc.
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

final class ExtensibleTests: XCTestCase {
    func testExtensionEncodeDecode() {
        let message = Extensible {
            $0.name = "real field"
            $0.ext_string = "extension string field"
            $0.ext_bool = true
            $0.ext_float = 3.14
            $0.ext_double = 3.14159
            $0.ext_bytes = Foundation.Data("test".utf8)
            $0.ext_int32 = -4
            $0.ext_uint32 = 42
            $0.ext_person = Person(name: "Someone", id: 1, data: Data(json_data: ""))
            $0.ext_phone_type = .MOBILE
        }
        let encoder = ProtoEncoder()
        let data = try! encoder.encode(message)

        let decoder = ProtoDecoder()
        let decodedMessage = try! decoder.decode(Extensible.self, from: data)
        XCTAssertEqual(message, decodedMessage)
        XCTAssertEqual(decodedMessage.name, "real field")
        XCTAssertEqual(decodedMessage.ext_string, "extension string field")
        XCTAssertEqual(decodedMessage.ext_bool, true)
        XCTAssertEqual(decodedMessage.ext_float, 3.14)
        XCTAssertEqual(decodedMessage.ext_double, 3.14159)
        XCTAssertEqual(decodedMessage.ext_bytes, Foundation.Data("test".utf8))
        XCTAssertEqual(decodedMessage.ext_person?.name, "Someone")
        XCTAssertEqual(decodedMessage.ext_person?.id, 1)
        XCTAssertEqual(decodedMessage.ext_int32, -4)
        XCTAssertEqual(decodedMessage.ext_uint32, 42)
        XCTAssertEqual(decodedMessage.ext_phone_type, .MOBILE)
    }

    func testExtensionEncodeDecodeHeapStorageMessage() {
        let message = LargeExtensible {
            $0.value1 = "value1"
            $0.ext_value17 = "ext_value17"
            $0.ext_value18 = "ext_value18"
        }
        let encoder = ProtoEncoder()
        let data = try! encoder.encode(message)

        let decoder = ProtoDecoder()
        let decodedMessage = try! decoder.decode(LargeExtensible.self, from: data)
        XCTAssertEqual(message, decodedMessage)
        XCTAssertEqual(decodedMessage.value1, "value1")
        XCTAssertEqual(decodedMessage.ext_value17, "ext_value17")
        XCTAssertEqual(decodedMessage.ext_value18, "ext_value18")
    }

    func testExtensionCopyOnWrite() {
        let message = LargeExtensible {
            $0.value1 = "value1"
            $0.ext_value17 = "ext_value17"
            $0.ext_value18 = "ext_value18"
        }
        XCTAssertEqual(message.value1, "value1")
        XCTAssertEqual(message.ext_value17, "ext_value17")
        XCTAssertEqual(message.ext_value18, "ext_value18")

        var copy = message
        copy.ext_value17 = "new_ext_value17"
        XCTAssertNotEqual(message, copy)
        XCTAssertEqual(message.ext_value17, "ext_value17")
        XCTAssertEqual(copy.ext_value17, "new_ext_value17")
    }

    func testRepeatedExtensions() {
        let message = LargeExtensible {
            $0.rep_ext_int32 = [0, -1, -2]
            $0.rep_ext_uint32 = [0, 1, 2]
            $0.rep_ext_sint32 = [0, -1, -2]
            $0.rep_ext_fixed32 = [0, 1, 2]
            $0.rep_ext_sfixed32 = [0, 1, 2]
            $0.rep_ext_int64 = [0, -1, -2]
            $0.rep_ext_uint64 = [0, 1, 2]
            $0.rep_ext_sint64 = [0, -1, -2]
            $0.rep_ext_fixed64 = [0, 1, 2]
            $0.rep_ext_sfixed64 = [0, 1, 2]
            $0.rep_ext_bool = [true, false, true]
            $0.rep_ext_float = [3.14, 1.41]
            $0.rep_ext_double = [3.14159, 1.41421]
            $0.rep_ext_string = ["one", "two", "three"]
            $0.rep_ext_bytes = [Foundation.Data("one".utf8), Foundation.Data("two".utf8)]
            $0.rep_ext_person = [Person(name: "First Person", id: 1, data: Data(json_data: "")), Person(name: "Second Person", id: 1, data: Data(json_data: ""))]
            $0.rep_ext_phone_type = [.MOBILE, .HOME]
        }
        let encoder = ProtoEncoder()
        let data = try! encoder.encode(message)

        let decoder = ProtoDecoder()
        let decodedMessage = try! decoder.decode(LargeExtensible.self, from: data)
        XCTAssertEqual(message, decodedMessage)
        XCTAssertEqual(message.rep_ext_int32, [0, -1, -2])
        XCTAssertEqual(message.rep_ext_uint32, [0, 1, 2])
        XCTAssertEqual(message.rep_ext_sint32, [0, -1, -2])
        XCTAssertEqual(message.rep_ext_fixed32, [0, 1, 2])
        XCTAssertEqual(message.rep_ext_sfixed32, [0, 1, 2])
        XCTAssertEqual(message.rep_ext_int64, [0, -1, -2])
        XCTAssertEqual(message.rep_ext_uint64, [0, 1, 2])
        XCTAssertEqual(message.rep_ext_sint64, [0, -1, -2])
        XCTAssertEqual(message.rep_ext_fixed64, [0, 1, 2])
        XCTAssertEqual(message.rep_ext_sfixed64, [0, 1, 2])
        XCTAssertEqual(message.rep_ext_bool, [true, false, true])
        XCTAssertEqual(message.rep_ext_float, [3.14, 1.41])
        XCTAssertEqual(message.rep_ext_double, [3.14159, 1.41421])
        XCTAssertEqual(message.rep_ext_string, ["one", "two", "three"])
        XCTAssertEqual(message.rep_ext_bytes, [Foundation.Data("one".utf8), Foundation.Data("two".utf8)])
        XCTAssertEqual(message.rep_ext_person, [Person(name: "First Person", id: 1, data: Data(json_data: "")), Person(name: "Second Person", id: 1, data: Data(json_data: ""))])
        XCTAssertEqual(message.rep_ext_phone_type, [.MOBILE, .HOME])
    }

    func testExtensionDefaultValues() {
        XCTAssertEqual(LargeExtensible.default_ext_value17, "my extension default value")
        XCTAssertEqual(LargeExtensible.default_ext_value18, "")
    }
}
