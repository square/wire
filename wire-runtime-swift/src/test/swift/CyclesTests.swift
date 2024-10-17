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

final class CyclesTests: XCTestCase {
    func testOneOfFieldCycle() throws {
        let f = F {
            $0.action = .g(G())
        }

        let encoder = ProtoEncoder()
        let data = try encoder.encode(f)

        let decoder = ProtoDecoder()
        let decodedMessage = try decoder.decode(F.self, from: data)

        XCTAssertEqual(decodedMessage, f)
    }
}
