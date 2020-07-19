//
//  Copyright © 2020 Square Inc. All rights reserved.
//

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

    // MARK: -

    public enum Error: Swift.Error, LocalizedError {

        case emptyData
        case fieldKeyValueZero
        case invalidFieldWireType(_: UInt32)
        case invalidStructure(message: String)
        case invalidUTF8StringData(_: Data)
        case malformedVarint
        case mapEntryWithoutKey(value: Any?)
        case mapEntryWithoutValue(key: Any?)
        case missingRequiredField(typeName: String, fieldName: String)
        case recursionLimitExceeded
        case unexpectedEndOfData
        case unexpectedEndGroupFieldNumber(expected: UInt32?, found: UInt32)
        case unexpectedFieldNumberInMap(_: UInt32)
        case unknownEnumCase(type: Any.Type, fieldNumber: UInt32)
        case unterminatedGroup(fieldNumber: UInt32)

        var localizedDescription: String {
            switch self {
            case .emptyData:
                return "The data to deserialize is empty."
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
                return "Map entry with \(key ?? "") did not include a value."
            case let .missingRequiredField(typeName, fieldName):
                return "Required field \(fieldName) for type \(typeName) is not included in the message data."
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
            case let .unexpectedFieldNumberInMap(fieldNumber):
                return "Map entry includes the field number \(fieldNumber), but only 1 and 2 are allowed."
            case let .unknownEnumCase(type, fieldNumber):
                return "Unknown case with value \(fieldNumber) found for enum of type \(String(describing: type))."
            case let .unterminatedGroup(fieldNumber):
                return "The group with field number \(fieldNumber) has no matching end-group key."
            }
        }
    }

    // MARK: - Initialization

    public init() {}

    // MARK: - Public Methods

    public func decode<T: ProtoDecodable>(_ type: T.Type, from data: Data) throws -> T {
        var value: T?
        try data.withUnsafeBytes { buffer in
            guard let baseAddress = buffer.baseAddress, buffer.count > 0 else {
                throw Error.emptyData
            }

            let readBuffer = ReadBuffer(
                storage: baseAddress.bindMemory(to: UInt8.self, capacity: buffer.count),
                count: buffer.count
            )
            let reader = ProtoReader(buffer: readBuffer)
            _ = try reader.forEachTag { tag in
                switch tag {
                case 1: value = try reader.decode(type)
                default: throw Error.invalidStructure(message: "The data root has more than one field. Found field number \(tag).")
                }
            }
        }

        guard let unwrappedValue = value else {
            throw Error.invalidStructure(message: "The data root does not have any identifiable fields.")
        }
        return unwrappedValue
    }

}

