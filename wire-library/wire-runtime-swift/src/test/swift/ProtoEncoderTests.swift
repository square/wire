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
import Wire
import XCTest

final class ProtoEncoderTests: XCTestCase {

    func testEncodeEmptyProtoMessage() throws {
        let object = EmptyMessage()
        let encoder = ProtoEncoder()
        let data = try encoder.encode(object)

        XCTAssertEqual(data, Data())
    }

    func testEncodeEmptyJSONMessage() throws {
        let object = EmptyMessage()
        let encoder = JSONEncoder()
        let data = try encoder.encode(object)
        let jsonString = try XCTUnwrap(String(data: data, encoding: .utf8))

        XCTAssertEqual(jsonString, "{}")
    }
}
