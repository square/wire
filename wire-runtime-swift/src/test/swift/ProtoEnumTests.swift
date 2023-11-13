/*
 * Copyright (C) 2023 Square, Inc.
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

import Foundation
import XCTest
@testable import Wire

final class ProtoEnumTests: XCTestCase {

    func testEnumValuesRoundTrip() throws {
        let encoder = ProtoEncoder()
        let decoder = ProtoDecoder()

        for value in NegativeValueEnum.allCases {
            let message = NegativeValueMessage {
                $0.value = value
            }

            let data = try encoder.encode(message)
            let decodedMessage = try decoder.decode(NegativeValueMessage.self, from: data)

            XCTAssertEqual(decodedMessage, message, "Could not roundtrip \(value)")
        }
    }

    func testEnumValuesDecodeAsInt32() throws {
        let encoder = ProtoEncoder()
        let decoder = ProtoDecoder()

        for value in NegativeValueEnum.allCases {
            let message = NegativeValueMessage {
                $0.value = value
            }

            let data = try encoder.encode(message)
            let decodedMessage = try decoder.decode(RawNegativeValueMessage.self, from: data)

            XCTAssertEqual(decodedMessage.value, value.rawValue, "Could not roundtrip \(value)")
        }
    }

    func testInt32ValuesDecodeAsEnumValues() throws {
        let encoder = ProtoEncoder()
        let decoder = ProtoDecoder()

        for value in NegativeValueEnum.allCases {
            let message = RawNegativeValueMessage {
                $0.value = value.rawValue
            }

            let data = try encoder.encode(message)
            let decodedMessage = try decoder.decode(NegativeValueMessage.self, from: data)

            XCTAssertEqual(decodedMessage.value, value, "Could not roundtrip \(value)")
        }
    }

}
