//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

extension Data {

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
        var data = Data(capacity: len)
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
