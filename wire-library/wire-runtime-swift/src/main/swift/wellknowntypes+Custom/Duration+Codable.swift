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

#if swift(>=5.7)

@available(macOS 13, iOS 16, watchOS 9, tvOS 16, *)
private extension Swift.Duration {
    var nanos: Int32 {
        let nanos = components.attoseconds * 1_000_000_000
        return Int32(truncatingIfNeeded: nanos)
    }
}

@available(macOS 13, iOS 16, watchOS 9, tvOS 16, *)
extension Wire.Duration {
    public var attos: Int64 {
        Int64(nanos) / 1_000_000_000
    }

    public func toSwiftDuration() -> Swift.Duration {
        return Swift.Duration(secondsComponent: seconds, attosecondsComponent: attos)
    }

    public init(duration: Swift.Duration) {
        self.init(
            seconds: duration.components.seconds,
            nanos: duration.nanos
        )
    }
}

#endif

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

        let seconds: Int64?
        let nanos: Int32?

        let components = string.split(separator: ".")
        switch components.count {
        case 1:
            seconds = Int64(components[0])
            nanos = 0

        case 2:
            seconds = Int64(components[0])
            nanos = Int32(components[1])

        default:
            seconds = nil
            nanos = nil
        }

        guard let seconds = seconds, let nanos = nanos else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Invalid duration \(string)s")
        }

        self.init(seconds: seconds, nanos: nanos)
    }
}

#endif
