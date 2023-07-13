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
        let seconds = Int64(self.rounded())
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
        // This should be marginally less lossy than using Epoch time
        TimeInterval(
            seconds: seconds - Int64(Date.timeIntervalBetween1970AndReferenceDate),
            nanos: nanos
        )
    }

    private init(timeIntervalSinceReferenceDate: TimeInterval) {
        // This should be marginally less lossy than using Epoch time
        let decomposed = timeIntervalSinceReferenceDate.decomposed()

        self.init(
            seconds: decomposed.seconds + Int64(Date.timeIntervalBetween1970AndReferenceDate),
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

private let rfc3339: ISO8601DateFormatter = {
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
