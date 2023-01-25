/*
 * Copyright 2023 Block Inc.
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

#if !WIRE_REMOVE_CODABLE

extension Wire.Duration : Codable {
    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        let encoded: String = {
            if nanos == 0 {
                return String(format: "%ds", seconds)
            } else if nanos % 1_000_000 == 0 {
                return String(format: "%d.%03ds", seconds, nanos / 1_000_000)
            } else if nanos % 1_000 == 0 {
                return String(format: "%d.%06ds", seconds, nanos / 1_000)
            } else {
                return String(format: "%d.%09ds", seconds, nanos)
            }
        }()

        try container.encode(encoded)
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        var string = try container.decode(String.self)
        guard let last = string.popLast(), last == "s" else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Invalid duration format \(string)")
        }
        guard let interval = TimeInterval(string) else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Invalid duration \(string)s")
        }

        let decomposed = interval.decomposed()

        self.init(seconds: decomposed.seconds, nanos: decomposed.nanos)
    }
}

#endif
