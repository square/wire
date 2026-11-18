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

#if swift(>=5.7)

@available(macOS 13, iOS 16, watchOS 9, tvOS 16, *)
extension Swift.Duration {
    /// The largest and smallest values for which `components` does not trap.
    /// `components` needs the whole seconds to fit in an `Int64`.
    fileprivate static let largestRepresentable: Swift.Duration = .seconds(Int64.max) + .nanoseconds(999_999_999)
    fileprivate static let smallestRepresentable: Swift.Duration = .seconds(Int64.min) - .nanoseconds(999_999_999)

    fileprivate var clampedToRepresentableComponents: Swift.Duration {
        return Swift.min(Swift.max(self, Swift.Duration.smallestRepresentable), Swift.Duration.largestRepresentable)
    }
}

@available(macOS 13, iOS 16, watchOS 9, tvOS 16, *)
extension Wire.Duration {
    public func toSwiftDuration() -> Swift.Duration {
        // The sum can be out of range when `seconds` is near the limits of `Int64`.
        return (.seconds(seconds) + .nanoseconds(nanos)).clampedToRepresentableComponents
    }

    public init(duration: Swift.Duration) {
        // Reading `components` traps for values with more seconds than an `Int64` can hold.
        let components = duration.clampedToRepresentableComponents.components
        self.init(
            seconds: components.seconds,
            nanos: Int32(truncatingIfNeeded: components.attoseconds / 1_000_000_000)
        )
    }
}

#endif

#if !WIRE_REMOVE_CODABLE

extension Wire.Duration : Codable {
    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        let prefix = seconds < 0 || nanos < 0 ? "-" : ""
        // `magnitude` does not trap for `Int64.min` and `Int32.min`, unlike `abs`.
        let seconds = seconds.magnitude
        let nanos = nanos.magnitude

        let encoded: String = {
            if nanos == 0 {
                return String(format: "%@%llus", prefix, seconds)
            } else if nanos % 1_000_000 == 0 {
                return String(format: "%@%llu.%03us", prefix, seconds, nanos / 1_000_000)
            } else if nanos % 1_000 == 0 {
                return String(format: "%@%llu.%06us", prefix, seconds, nanos / 1_000)
            } else {
                return String(format: "%@%llu.%09us", prefix, seconds, nanos)
            }
        }()

        try container.encode(encoded)
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let rawString = try container.decode(String.self)
        let isNegative = rawString.starts(with: "-")

        var string = rawString
        guard let last = string.popLast(), last == "s" else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Invalid duration format \(string)")
        }
        if isNegative {
            string.removeFirst()
        }

        var seconds: Int64?
        let nanos: Int32?

        let components = string.split(separator: ".")
        switch components.count {
        case 1:
            seconds = Int64(components[0])
            nanos = 0

        case 2:
            seconds = Int64(components[0])
            nanos = Int32(components[1].padding(toLength: 9, withPad: "0", startingAt: 0))

        default:
            seconds = nil
            nanos = nil
        }

        // The magnitude of `Int64.min` does not fit in an `Int64`, but `-9223372036854775808s` is valid.
        var isInt64Min = false
        if isNegative, seconds == nil, nanos != nil, let magnitude = UInt64(components[0]), magnitude == Int64.min.magnitude {
            isInt64Min = true
            seconds = Int64.min
        }

        guard let seconds = seconds, let nanos = nanos else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Invalid duration \(string)s")
        }

        if isNegative {
            // Negating `Int64.min` overflows. This happens for `--9223372036854775808s`.
            let negated = isInt64Min ? (partialValue: seconds, overflow: false) : (0 as Int64).subtractingReportingOverflow(seconds)
            guard !negated.overflow else {
                throw DecodingError.dataCorruptedError(in: container, debugDescription: "Duration out of range \(rawString)")
            }
            self.init(seconds: negated.partialValue, nanos: -nanos)
        } else {
            self.init(seconds: seconds, nanos: nanos)
        }
    }
}

#endif
