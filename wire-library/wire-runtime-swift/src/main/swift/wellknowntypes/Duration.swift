// Code generated by Wire protocol buffer compiler, do not edit.
// Source: google.protobuf.Duration in google/protobuf/duration.proto
import Foundation

/**
 * A Duration represents a signed, fixed-length span of time represented
 * as a count of seconds and fractions of seconds at nanosecond
 * resolution. It is independent of any calendar and concepts like "day"
 * or "month". It is related to Timestamp in that the difference between
 * two Timestamp values is a Duration and it can be added or subtracted
 * from a Timestamp. Range is approximately +-10,000 years.
 *
 * Example 1: Compute Duration from two Timestamps in pseudo code.
 *
 *     Timestamp start = ...;
 *     Timestamp end = ...;
 *     Duration duration = ...;
 *
 *     duration.seconds = end.seconds - start.seconds;
 *     duration.nanos = end.nanos - start.nanos;
 *
 *     if (duration.seconds < 0 && duration.nanos > 0) {
 *       duration.seconds += 1;
 *       duration.nanos -= 1000000000;
 *     } else if (durations.seconds > 0 && duration.nanos < 0) {
 *       duration.seconds -= 1;
 *       duration.nanos += 1000000000;
 *     }
 *
 * Example 2: Compute Timestamp from Timestamp + Duration in pseudo code.
 *
 *     Timestamp start = ...;
 *     Duration duration = ...;
 *     Timestamp end = ...;
 *
 *     end.seconds = start.seconds + duration.seconds;
 *     end.nanos = start.nanos + duration.nanos;
 *
 *     if (end.nanos < 0) {
 *       end.seconds -= 1;
 *       end.nanos += 1000000000;
 *     } else if (end.nanos >= 1000000000) {
 *       end.seconds += 1;
 *       end.nanos -= 1000000000;
 *     }
 */
public struct Duration {

    /**
     * Signed seconds of the span of time. Must be from -315,576,000,000
     * to +315,576,000,000 inclusive.
     */
    public var seconds: Int64
    /**
     * Signed fractions of a second at nanosecond resolution of the span
     * of time. Durations less than one second are represented with a 0
     * `seconds` field and a positive or negative `nanos` field. For durations
     * of one second or more, a non-zero value for the `nanos` field must be
     * of the same sign as the `seconds` field. Must be from -999,999,999
     * to +999,999,999 inclusive.
     */
    public var nanos: Int32
    public var unknownFields: Data = .init()

    public init(seconds: Int64, nanos: Int32) {
        self.seconds = seconds
        self.nanos = nanos
    }

}

#if !WIRE_REMOVE_EQUATABLE
extension Duration : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension Duration : Hashable {
}
#endif

#if swift(>=5.5)
extension Duration : Sendable {
}
#endif

extension Duration : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/google.protobuf.Duration"
    }
}

extension Duration : Proto3Codable {
    public init(from reader: ProtoReader) throws {
        var seconds: Int64? = nil
        var nanos: Int32? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: seconds = try reader.decode(Int64.self)
            case 2: nanos = try reader.decode(Int32.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.seconds = try Duration.checkIfMissing(seconds, "seconds")
        self.nanos = try Duration.checkIfMissing(nanos, "nanos")
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.seconds)
        try writer.encode(tag: 2, value: self.nanos)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension Duration : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: Duration.CodingKeys.self)
        self.seconds = try container.decode(StringEncoded<Int64>.self, forKey: .seconds).wrappedValue
        self.nanos = try container.decode(Int32.self, forKey: .nanos)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: Duration.CodingKeys.self)
        try container.encode(StringEncoded(wrappedValue: self.seconds), forKey: .seconds)
        try container.encode(self.nanos, forKey: .nanos)
    }

    public enum CodingKeys : String, CodingKey {

        case seconds
        case nanos

    }
}
#endif
