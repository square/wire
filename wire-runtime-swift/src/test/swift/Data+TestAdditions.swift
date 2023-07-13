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

extension Foundation.Data {

    init?(hexEncoded string: String) {
        // Allow underscores, spaces, newlines, and comments for improved readability of the hex constants.
        // This replacement is slow, but is fine for testing.
        let string = string
            .split(separator: "\n")
            .compactMap { $0.split(separator: "/").first }
            .joined(separator: "")
            .replacingOccurrences(of: "_", with: "")
            .replacingOccurrences(of: " ", with: "")

        let len = string.count / 2
        var data = Foundation.Data(capacity: len)
        for i in 0..<len {
            let j = string.index(string.startIndex, offsetBy: i*2)
            let k = string.index(j, offsetBy: 2)
            let bytes = string[j..<k]
            if bytes == "_" {
                continue
            }
            if var num = UInt8(bytes, radix: 16) {
                data.append(&num, count: 1)
            } else {
                return nil
            }
        }
        self = data
    }

    /** A convenience method used for debugging */
    func hexEncodedString() -> String {
        return map { String(format: "%02x", $0) }.joined().uppercased()
    }

}
