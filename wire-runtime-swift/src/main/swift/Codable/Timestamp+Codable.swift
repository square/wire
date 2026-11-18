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

extension TimeInterval {
    func decomposed() -> (seconds: Int64, nanos: Int32) {
        // `Int64(_:)` traps on NaN, infinity, and values outside the `Int64` range.
        guard self.isFinite else {
            switch self {
            case .infinity: return (Int64.max, 0)
            case -.infinity: return (Int64.min, 0)
            default: return (0, 0)
            }
        }

        let rounded = self.rounded()
        guard let seconds = Int64(exactly: rounded) else {
            return (rounded < 0 ? Int64.min : Int64.max, 0)
        }
        let remainder = self.remainder(dividingBy: 1)
        let nanos = Int32(remainder * 1_000_000_000)

        return (seconds, nanos)
    }

    init(seconds: Int64, nanos: Int32) {
        self.init(seconds)
        self += TimeInterval(nanos) / 1_000_000_000
    }
}

extension Wire.Timestamp {
    public var timeIntervalSince1970: TimeInterval {
        TimeInterval(seconds: seconds, nanos: nanos)
    }

    public init(timeIntervalSince1970: TimeInterval) {
        let decomposed = timeIntervalSince1970.decomposed()

        self.init(
            seconds: decomposed.seconds,
            nanos: decomposed.nanos
        )
    }

    private var timeIntervalSinceReferenceDate: TimeInterval {
        let offset = Int64(Date.timeIntervalBetween1970AndReferenceDate)
        let (referenceSeconds, overflow) = seconds.subtractingReportingOverflow(offset)
        if overflow {
            // `seconds` is within `offset` of `Int64.min`. Subtract in `Double` instead of trapping.
            return TimeInterval(seconds: seconds, nanos: nanos) - TimeInterval(offset)
        }

        // This should be marginally less lossy than using Epoch time
        return TimeInterval(seconds: referenceSeconds, nanos: nanos)
    }

    private init(timeIntervalSinceReferenceDate: TimeInterval) {
        guard timeIntervalSinceReferenceDate.isFinite else {
            // NaN becomes the epoch and infinities clamp, like `init(timeIntervalSince1970:)`.
            self.init(timeIntervalSince1970: timeIntervalSinceReferenceDate)
            return
        }

        // This should be marginally less lossy than using Epoch time
        let decomposed = timeIntervalSinceReferenceDate.decomposed()
        let offset = Int64(Date.timeIntervalBetween1970AndReferenceDate)
        // Clamp instead of trapping when the sum does not fit in an `Int64`.
        let (seconds, overflow) = decomposed.seconds.addingReportingOverflow(offset)

        self.init(
            seconds: overflow ? Int64.max : seconds,
            nanos: decomposed.nanos
        )
    }

    public var date: Date {
        Date(timeIntervalSinceReferenceDate: timeIntervalSinceReferenceDate)
    }

    public init(date: Date) {
        self.init(timeIntervalSinceReferenceDate: date.timeIntervalSinceReferenceDate)
    }
}

#if !WIRE_REMOVE_CODABLE

nonisolated(unsafe) private let rfc3339: ISO8601DateFormatter = {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = .withInternetDateTime
    return formatter
}()

extension Wire.Timestamp : Codable {
    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()

        let string = rfc3339.string(from: date)
        try container.encode(string)
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        let string = try container.decode(String.self)
        guard let date = rfc3339.date(from: string) else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Could not create RFC3339 date from \(string)")
        }
        self.init(date: date)
    }
}

#endif
