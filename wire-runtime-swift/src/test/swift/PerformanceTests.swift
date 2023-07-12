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
import Wire
import XCTest

// Performance tests are disabled by default as they're slow and
// don't provide much value in CI given that CI hardware can vary significantly.
#if false

final class PerformanceTests: XCTestCase {

    func testSmallMessages() {
        measure {
            runTests(stringSize: .small)
        }
    }

    func testVaryingMessageSizes() {
        measure {
            runTests(stringSize: .random)
        }
    }

    func testLargeMessages() {
        measure {
            runTests(stringSize: .large)
        }
    }

    private func runTests(stringSize: Factory.StringSize) {
        let decoder = ProtoDecoder()
        let encoder = ProtoEncoder()

        try! (0 ..< 10_000).forEach { _ in
            let message = Factory.makeMessage(stringSize: stringSize)
            let data = try encoder.encode(message)

            let decodedMessage = try decoder.decode(Nested1.self, from: data)

            XCTAssertEqual(decodedMessage, message)
        }
    }
}

// MARK: -

private enum Factory {

    enum StringSize {
        // < 127 characters; size value of the field will be 1 byte.
        case small

        // 128 < size < 16383; size value of the field will be 2 bytes.
        case medium

        // 16384 < size < 2097151; size value of the field will be 3 bytes.
        case large

        // Return a random string size, with smaller strings being more likely.
        case random
    }

    private static var largeString: String?

    static func makeString(_ size: StringSize) -> String {
        switch size {
        case .small:
            return "Lorem ipsum dolor sit amet, consectetur adipiscing elit"
        case .medium:
            return "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
        case .large:
            if largeString == nil {
                let minimumSize = 16384
                var string = makeString(.medium)
                while string.count < minimumSize {
                    string = string + string
                }
                largeString = string
            }
            return largeString!
        case .random:
            switch (0 ..< 1000).randomElement()! {
            case 0 ... 1:
                return makeString(.large)
            case 2 ... 100:
                return makeString(.medium)
            default:
                return makeString(.small)
            }
        }
    }

    static func makeMessage(stringSize: StringSize) -> Nested1 {
        return Nested1(
            name: makeString(stringSize),
            nested: .init(
                name: makeString(stringSize),
                nested: .init(
                    name: makeString(stringSize),
                    nested: .init(name: makeString(stringSize))
                )
            )
        )
    }
}

#endif
