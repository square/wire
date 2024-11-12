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

/**
 A class responsible for turning bytes from a serialized protocol buffer message
 into an in-memory struct representing that message.

 General usage will look something like:
 ```
 let decoder = ProtoDecoder()
 let decodedMessage = try decoder.decode(GeneratedMessageType.self, from: data)
 ```
 */
public final class ProtoDecoder {

    // MARK: - Public enums

    /// Determines the way in which Wire's Swift runtime library handles unknown enum values when decoding.
    public enum UnknownEnumValueDecodingStrategy {
        /// Throws an error when encountering unknown enum values in single-value fields or collections.
        case throwError
        /// Defaults the unknown enum value to nil for single-value fields.
        /// With this option, unknown values in collections are removed from the collection in which they originated.
        /// When decoding a Proto, unknown values are then added to a collection for the same tag in unknown fields.
        case returnNil
    }

    public enum Error: Swift.Error, LocalizedError {

        case boxedValueMissingField(type: Any.Type)
        case fieldKeyValueZero
        case invalidFieldWireType(_: UInt32)
        case invalidStructure(message: String)
        case invalidUTF8StringData(_: Data)
        case malformedVarint
        case mapEntryWithoutKey(value: Any?)
        case mapEntryWithoutValue(key: Any)
        case messageWithoutLength
        case missingEnumDefaultValue(type: Any.Type)
        case missingRequiredField(typeName: String, fieldName: String)
        case recursionLimitExceeded
        case unexpectedEndOfData
        case unexpectedEndGroupFieldNumber(expected: UInt32?, found: UInt32)
        case unexpectedFieldNumberInMap(_: UInt32)
        case unexpectedFieldNumberInBoxedValue(_: UInt32)
        case unparsableString(type: Any.Type, value: String)
        case unknownEnumCase(type: Any.Type, fieldNumber: Int32)
        case unknownEnumString(type: Any.Type, string: String)
        case unterminatedGroup(fieldNumber: UInt32)

        var localizedDescription: String {
            switch self {
            case .fieldKeyValueZero:
                return "Message field has a field number of zero, which is invalid."
            case let .invalidFieldWireType(value):
                return "Field has an invalid wire type of \(value)."
            case let .invalidStructure(message):
                return "Message structure is invalid: \(message)."
            case let .invalidUTF8StringData(data):
                return "String field of size \(data.count) is not valid UTF8 encoded data."
            case .malformedVarint:
                return "Encoded varint was not in the correct format."
            case let .mapEntryWithoutKey(value):
                return "Map entry with value \(value ?? "") did not include a key."
            case let .mapEntryWithoutValue(key):
                return "Map entry with \(key) did not include a value."
            case .messageWithoutLength:
                return "Attempting to decode a message without first decoding the length of that message."
            case let .missingEnumDefaultValue(type):
                return "Could not assign a default value of 0 for enum type \(String(describing: type))"
            case let .missingRequiredField(typeName, fieldName):
                return "Required field \(fieldName) for type \(typeName) is not included in the message data."
            case let .boxedValueMissingField(type):
                return "Boxed value for type \(type) is missing field 1 (the value field)."
            case .recursionLimitExceeded:
                return "Message nesting exceeds the maximum allowed depth."
            case .unexpectedEndOfData:
                return "A field indicates that its data extends beyond the end of the available message data."
            case let .unexpectedEndGroupFieldNumber(expected, found):
                if let expected = expected {
                    return "Found non-matching end-group field number. Expected to end group \(expected), but found end key for \(found)."
                } else {
                    return "Found end-group key with field number \(found) but no matching start-group key existed."
                }
            case let .unexpectedFieldNumberInBoxedValue(fieldNumber):
                return "Boxed value includes the field number \(fieldNumber), but only field 1 is allowed."
            case let .unparsableString(type, value):
                return "Unknown string encoded type with value \(value) for type \(String(describing: type))"
            case let .unexpectedFieldNumberInMap(fieldNumber):
                return "Map entry includes the field number \(fieldNumber), but only 1 and 2 are allowed."
            case let .unknownEnumCase(type, fieldNumber):
                return "Unknown case with value \(fieldNumber) found for enum of type \(String(describing: type))."
            case let .unknownEnumString(type, string):
                return "Unknown case with string value \(string) found for enum of type \(String(describing: type))."
            case let .unterminatedGroup(fieldNumber):
                return "The group with field number \(fieldNumber) has no matching end-group key."
            }
        }
    }

    // MARK: - Private constants

    public private(set) var enumDecodingStrategy: UnknownEnumValueDecodingStrategy

    // MARK: - Initialization

    public init(enumDecodingStrategy: UnknownEnumValueDecodingStrategy = .throwError) {
        self.enumDecodingStrategy = enumDecodingStrategy
    }

    // MARK: - Public Methods

    /** Decode a `ProtoDecodable` field from raw data */
    public func decode<T: ProtoDecodable>(_ type: T.Type, from data: Data) throws -> T {
        try decodeWithReader(from: data, emptyValue: try T(from: .empty)) { reader in
            try reader.decode(type)
        }
    }

    // MARK: - Internal Methods

    /** Decode a tagged `ProtoDecodable` field from raw data */
    internal func decode<T: ProtoDecodable>(_ type: T.Type, from data: Data, withTag tag: UInt32) throws -> T {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(type, withTag: tag)
        }
    }

    /** Decode a tagged `ProtoEnum` field from raw data */
    internal func decode<T: ProtoEnum>(_ type: T.Type, from data: Data, withTag tag: UInt32) throws -> T where T: RawRepresentable<Int32> {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(type, withTag: tag)
        }
    }

    /** Decode a tagged `int32`, `sfixed32`, or `sint32` field from raw data */
    internal func decode(_ type: Int32.Type, from data: Data, encoding: ProtoIntEncoding, withTag tag: UInt32) throws -> Int32 {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(type, encoding: encoding, withTag: tag)
        }
    }

    /** Decode a tagged `int64`, `sfixed64`, or `sint64` field from raw data */
    internal func decode(_ type: Int64.Type, from data: Data, encoding: ProtoIntEncoding, withTag tag: UInt32) throws -> Int64 {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(type, encoding: encoding, withTag: tag)
        }
    }

    /** Decode a tagged `fixed32` or `uint32` field from raw data */
    internal func decode(_ type: UInt32.Type, from data: Data, encoding: ProtoIntEncoding, withTag tag: UInt32) throws -> UInt32 {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(type, encoding: encoding, withTag: tag)
        }
    }

    /** Decode a tagged `fixed64` or `uint64` field from raw data */
    internal func decode(_ type: UInt64.Type, from data: Data, encoding: ProtoIntEncoding, withTag tag: UInt32) throws -> UInt64 {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(type, encoding: encoding, withTag: tag)
        }
    }

    /** Decode a repeated tagged `fixed64` or `uint64` field from raw data */
    internal func decode(into array: inout [UInt64], from data: Data, encoding: ProtoIntEncoding, withTag tag: UInt32) throws {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, encoding: encoding, withTag: tag)
        }
    }

    /** Decode a repeated tagged `fixed32` or `uint32` field from raw data */
    internal func decode(into array: inout [UInt32], from data: Data, encoding: ProtoIntEncoding, withTag tag: UInt32) throws {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, encoding: encoding, withTag: tag)
        }
    }

    /** Decode a repeated tagged `fixed64` or `int64` field from raw data */
    internal func decode(into array: inout [Int64], from data: Data, encoding: ProtoIntEncoding, withTag tag: UInt32) throws {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, encoding: encoding, withTag: tag)
        }
    }

    /** Decode a repeated tagged `fixed32` or `int32` field from raw data */
    internal func decode(into array: inout [Int32], from data: Data, encoding: ProtoIntEncoding, withTag tag: UInt32) throws {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, encoding: encoding, withTag: tag)
        }
    }

    /** Decode a repeated tagged `bool` field from raw data */
    internal func decode(into array: inout [Bool], from data: Data, withTag tag: UInt32) throws {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, withTag: tag)
        }
    }

    /** Decode a repeated tagged `float` field from raw data */
    internal func decode(into array: inout [Float], from data: Data, withTag tag: UInt32) throws {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, withTag: tag)
        }
    }

    /** Decode a repeated tagged `double` field from raw data */
    internal func decode(into array: inout [Double], from data: Data, withTag tag: UInt32) throws {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, withTag: tag)
        }
    }

    /** Decode a repeated tagged `string` field from raw data */
    internal func decode(into array: inout [String], from data: Data, withTag tag: UInt32) throws {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, withTag: tag)
        }
    }

    /** Decode a repeated tagged `bytes` field from raw data */
    internal func decode(into array: inout [Data], from data: Data, withTag tag: UInt32) throws {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, withTag: tag)
        }
    }

    /** Decode a repeated tagged `ProtoDecodable` field from raw data */
    internal func decode<T: ProtoDecodable>(into array: inout [T], from data: Data, withTag tag: UInt32) throws {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, withTag: tag)
        }
    }

    /** Decode a repeated tagged `ProtoEnum` field from raw data */
    internal func decode<T: ProtoEnum>(into array: inout [T], from data: Data, withTag tag: UInt32) throws where T: RawRepresentable<Int32> {
        try decodeWithReader(from: data, emptyValue: nil) { reader in
            try reader.decode(into: &array, withTag: tag)
        }
    }

    // MARK: - Private Methods

    private func decodeWithReader<T>(
        from data: Data,
        emptyValue: @autoclosure () throws -> T?,
        decoder: (ProtoReader) throws -> T
    ) throws -> T {
        var value: T?
        try data.withUnsafeBytes { buffer in
            // Handle the empty-data case.
            guard let baseAddress = buffer.baseAddress, buffer.count > 0 else {
                value = try emptyValue()
                return
            }

            let readBuffer = ReadBuffer(
                storage: baseAddress.bindMemory(to: UInt8.self, capacity: buffer.count),
                count: buffer.count
            )
            let reader = ProtoReader(buffer: readBuffer, enumDecodingStrategy: enumDecodingStrategy)
            value = try decoder(reader)
        }
        guard let value else {
            throw Error.invalidStructure(message: "The data root does not have any identifiable fields.")
        }
        return value
    }

    /** Decodes the provided size-delimited data into instances of the requested type.
     *
     * A size-delimited collection of messages is a sequence of varint + message pairs
     * where the varint indicates the size of the subsequent message.
     *
     * - Parameters:
     *   - type: the type to decode
     *   - data: the serialized size-delimited data for the messages
     * - Returns: an array of the decoded messages
     */
    public func decodeSizeDelimited<T: ProtoDecodable>(_ type: T.Type, from data: Foundation.Data) throws -> [T] {
        var values: [T] = []

        try data.withUnsafeBytes { buffer in
            // Handle the empty-data case.
            guard let baseAddress = buffer.baseAddress, buffer.count > 0 else {
                return
            }

            let fullBuffer = ReadBuffer(
                storage: baseAddress.bindMemory(to: UInt8.self, capacity: buffer.count),
                count: buffer.count
            )

            while fullBuffer.isDataRemaining, let size = try? fullBuffer.readVarint() {
                if size == 0 { break }

                let messageBuffer = ReadBuffer(
                    storage: fullBuffer.pointer,
                    count: Int(size)
                )

                let reader = ProtoReader(
                    buffer: messageBuffer,
                    enumDecodingStrategy: enumDecodingStrategy
                )

                values.append(try reader.decode(type))

                // Advance the buffer before reading the next item in the stream.
                _ = try fullBuffer.readBuffer(count: Int(size))
            }
        }

        return values
    }

}
