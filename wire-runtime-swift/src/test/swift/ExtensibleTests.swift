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
            $0.string = "extension string field"
            $0.int32 = -4
            $0.uint32 = 42
            $0.person = Person(name: "Someone", id: 1, data: Data(json_data: ""))
            $0.phone_type = .MOBILE
        }
        let encoder = ProtoEncoder()
        let data = try! encoder.encode(message)

        let decoder = ProtoDecoder()
        let decodedMessage = try! decoder.decode(Extensible.self, from: data)
        XCTAssertEqual(message, decodedMessage)
        XCTAssertEqual(message.name, "real field")
        XCTAssertEqual(message.string, "extension string field")
        XCTAssertEqual(message.person?.name, "Someone")
        XCTAssertEqual(message.person?.id, 1)
        XCTAssertEqual(message.int32, -4)
        XCTAssertEqual(message.uint32, 42)
        XCTAssertEqual(message.phone_type, .MOBILE)
    }
}
