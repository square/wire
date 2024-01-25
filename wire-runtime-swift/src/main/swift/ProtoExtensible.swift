/*
 * Copyright (C) 2024 Square, Inc.
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

/// Protocol all messages conform to if they are extensible.
public protocol ProtoExtensible {
    var unknownFields: ExtensibleUnknownFields { get set }
}

public extension ProtoExtensible {
    // MARK: - ProtoCodable

    func parseUnknownField<T: ProtoDecodable>(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        type: T.Type
    ) -> T? {
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(T.self, from: data, withTag: fieldNumber)
        }
    }

    mutating func setUnknownField<T: ProtoEncodable>(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: T?
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value)
        }
    }

    // MARK: - ProtoEnum

    func parseUnknownField<T: ProtoEnum>(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        type: T.Type
    ) -> T? where T: RawRepresentable<Int32> {
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(T.self, from: data, withTag: fieldNumber)
        }
    }

    mutating func setUnknownField<T: ProtoEnum>(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: T?
    ) where T: RawRepresentable<Int32> {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value)
        }
    }

    // MARK: - ProtoIntCodable

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        type: Int32.Type,
        encoding: ProtoIntEncoding
    ) -> Int32? {
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(Int32.self, from: data, encoding: encoding, withTag: fieldNumber)
        }
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: Int32?,
        encoding: ProtoIntEncoding
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value, encoding: encoding)
        }
    }

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        type: Int64.Type,
        encoding: ProtoIntEncoding
    ) -> Int64? {
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(Int64.self, from: data, encoding: encoding, withTag: fieldNumber)
        }
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: Int64?,
        encoding: ProtoIntEncoding
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value, encoding: encoding)
        }
    }

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        type: UInt32.Type,
        encoding: ProtoIntEncoding
    ) -> UInt32? {
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(UInt32.self, from: data, encoding: encoding, withTag: fieldNumber)
        }
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: UInt32?,
        encoding: ProtoIntEncoding
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value, encoding: encoding)
        }
    }

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        type: UInt64.Type,
        encoding: ProtoIntEncoding
    ) -> UInt64? {
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(UInt64.self, from: data, encoding: encoding, withTag: fieldNumber)
        }
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: UInt64?,
        encoding: ProtoIntEncoding
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value, encoding: encoding)
        }
    }

    // MARK: - Arrays

    // MARK: - [UInt64]

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        encoding: ProtoIntEncoding
    ) -> [UInt64] {
        var result: [UInt64] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, encoding: encoding, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [UInt64],
        encoding: ProtoIntEncoding
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value, encoding: encoding)
        }
    }

    // MARK: - [UInt32]

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        encoding: ProtoIntEncoding
    ) -> [UInt32] {
        var result: [UInt32] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, encoding: encoding, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [UInt32],
        encoding: ProtoIntEncoding
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value, encoding: encoding)
        }
    }

    // MARK: - [Int64]

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        encoding: ProtoIntEncoding
    ) -> [Int64] {
        var result: [Int64] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, encoding: encoding, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [Int64],
        encoding: ProtoIntEncoding
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value, encoding: encoding)
        }
    }

    // MARK: - [Int32]

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        encoding: ProtoIntEncoding
    ) -> [Int32] {
        var result: [Int32] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, encoding: encoding, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [Int32],
        encoding: ProtoIntEncoding
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value, encoding: encoding)
        }
    }

    // MARK: - [Bool]

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32
    ) -> [Bool] {
        var result: [Bool] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [Bool]
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value)
        }
    }

    // MARK: - [Float]

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32
    ) -> [Float] {
        var result: [Float] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [Float]
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value)
        }
    }

    // MARK: - [Double]

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32
    ) -> [Double] {
        var result: [Double] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [Double]
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value)
        }
    }

    // MARK: - [String]

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32
    ) -> [String] {
        var result: [String] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [String]
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value)
        }
    }

    // MARK: - [Data]

    func parseUnknownField(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32
    ) -> [Data] {
        var result: [Data] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [Data]
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value)
        }
    }

    // MARK: - [ProtoCodable]

    func parseUnknownField<T: ProtoDecodable>(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32
    ) -> [T] {
        var result: [T] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField<T: ProtoEncodable>(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [T]
    ) {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value)
        }
    }

    // MARK: - [ProtoEnum]

    func parseUnknownField<T: ProtoEnum>(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32
    ) -> [T] where T: RawRepresentable<Int32> {
        var result: [T] = []
        try? unknownFields.getParsedField(fieldNumber: fieldNumber) { data in
            try protoDecoder.decode(into: &result, from: data, withTag: fieldNumber)
        }
        return result
    }

    mutating func setUnknownField<T: ProtoEnum>(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: [T]
    ) where T: RawRepresentable<Int32> {
        try? unknownFields.setParsedField(fieldNumber: fieldNumber, value: newValue) { value in
            try protoEncoder.encode(tag: fieldNumber, value: value)
        }
    }
}
