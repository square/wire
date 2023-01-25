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

extension TimeInterval {
    func decomposed() -> (seconds: Int64, nanos: Int32) {
        let seconds = Int64(self.rounded())
        let remainder = self.remainder(dividingBy: 1)
        let nanos = Int32(remainder * pow(10, 9))

        return (seconds, nanos)
    }

    init(seconds: Int64, nanos: Int32) {
        self.init(seconds)
        self += TimeInterval(nanos) / pow(10, 9)
    }
}

extension Wire.Timestamp {
    /// Creates a timestamp equivalent to the this value
    /// - Note: This is somewhat lossy
    public var timeInterval: TimeInterval {
        TimeInterval(seconds: seconds, nanos: nanos)
    }

    /// Initialize a Wire Timestamp from a TimeInterval.
    /// - Note: This is somewhat lossy
    public init(timeInterval: TimeInterval) {
        let decomposed = timeInterval.decomposed()

        self.init(
            seconds: decomposed.seconds,
            nanos: decomposed.nanos
        )
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

        let date = Date(timeIntervalSinceReferenceDate: timeInterval)
        let string = rfc3339.string(from: date)
        try container.encode(string)
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        let string = try container.decode(String.self)
        guard let date = rfc3339.date(from: string) else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Could not create RFC3339 date from \(string)")
        }
        self.init(timeInterval: date.timeIntervalSinceReferenceDate)
    }
}

#endif
