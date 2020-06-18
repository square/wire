//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

/**
 Writes values into data using the protocol buffer format in a stream-like fashion.
 */
public final class ProtoWriter {

    // MARK: - Properties

    private(set) var data: Data

    // MARK: - Life Cycle

    init(data: Data = .init()) {
        self.data = data
    }

    // MARK: - Public Methods - Encoding

    /** Encode an integer field */
    public func encode<T: ProtoIntEncodable>(tag: UInt32, value: T?, encoding: ProtoIntEncoding = .variable) throws {
        guard let value = value else { return }
        let wireType = ProtoWriter.wireType(for: type(of: value), encoding: encoding)
        try encode(tag: tag, wireType: wireType, value: value) {
            try $0.encode(to: self, encoding: encoding)
        }
    }

    /**
     Encode a field which has a single encoding mechanism (unlike integers).
     This includes most fields types, such as messages, strings, bytes, and floating point numbers.
     */
     public func encode(tag: UInt32, value: ProtoEncodable?) throws {
         guard let value = value else { return }

         let wireType = type(of: value).protoFieldWireType
         let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)

         writeVarint(key)
         if wireType == .lengthDelimited {
             try encodeLengthDelimited() { try value.encode(to: self) }
         } else {
             try value.encode(to: self)
         }
     }

    // MARK: - Internal Methods - Writing Primitives

    /** Write arbitrary data */
    func write(_ data: Data) {
        self.data.append(data)
    }

    /** Write a single byte */
    func write(_ value: UInt8) {
        data.append(value)
    }

    /** Write a buffer of bytes */
    func write(_ buffer: UnsafeRawBufferPointer) {
        data.append(contentsOf: buffer)
    }

    /** Write a little-endian 32-bit integer.  */
    func writeFixed32(_ value: UInt32) {
        withUnsafeBytes(of: value.littleEndian) { data.append(contentsOf: $0) }
    }

    /** Write a little-endian 64-bit integer.  */
    func writeFixed64(_ value: UInt64) {
        withUnsafeBytes(of: value.littleEndian) { data.append(contentsOf: $0) }
    }

    func writeVarint(_ value: UInt32) {
        writeVarint(UInt64(value))
    }

    func writeVarint(_ value: UInt64) {
        writeVarint(value, at: data.count)
    }

    // MARK: - Private Methods - Field Keys

    /** Makes a tag value given a field number and wire type. */
    private static func makeFieldKey(tag: UInt32, wireType: FieldWireType) -> UInt32 {
        return (tag << Constants.tagFieldEncodingBits) | wireType.rawValue
    }

    private static func wireType<T: ProtoIntEncodable>(for valueType: T.Type, encoding: ProtoIntEncoding) -> FieldWireType {
        if encoding == .fixed {
            let size = MemoryLayout<T>.size
            if size == 4 {
                return .fixed32
            } else if size == 8 {
                return .fixed64
            } else {
                fatalError("Trying to encode integer of unexpected size \(size)")
            }
        } else {
            return .varint
        }
    }

    // MARK: - Private Methods - Field Encoder Helpers

    /** Encode a field */
    private func encode<T>(tag: UInt32, wireType: FieldWireType, value: T, encode: (T) throws -> Void) rethrows {
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)
        writeVarint(key)
        try encode(value)
    }

    private func encodeLengthDelimited(_ encode: () throws -> Void) rethrows {
        let startOffset = data.count
        let reservedSize = 2
        for _ in 0 ..< reservedSize {
            data.append(0)
        }

        try encode()

        let writtenCount = UInt32(data.count - startOffset - reservedSize)

        let sizeSize = Int(writtenCount.varintSize)
        if sizeSize < reservedSize {
            data.removeSubrange(startOffset + sizeSize ..< startOffset + reservedSize)
        } else if sizeSize != reservedSize {
            let zeros = [UInt8](repeating: 0, count: sizeSize - reservedSize)
            data.insert(contentsOf: zeros, at: startOffset + reservedSize)
        }

        writeVarint(writtenCount, at: startOffset)
    }

    // MARK: - Private Methods - Writing Primitives

    private func writeVarint(_ value: UInt32, at index: Int) {
        // Because an unsigned (positive) varint will only use as many bytes as it needs we can
        // safely up-cast a 32-bit value to a 64-bit one for encoding purposes.
        writeVarint(UInt64(value), at: index)
    }

    /**
     * Encode a UInt64 into writable varint representation data. `value` is treated  unsigned, so it won't be sign-extended
     * if negative.
     */
    private func writeVarint(_ value: UInt64, at index: Int) {
        var index = index
        var value = value

        while (value & ~0x7f) != 0 {
            let byte = UInt8((value & 0x7f) | 0x80)
            if index < data.count {
                data[index] = byte
            } else {
                data.append(byte)
            }
            index += 1
            value = value >> 7
        }
        let byte = UInt8(bitPattern: Int8(value))
        if index < data.count {
            data[index] = byte
        } else {
            data.append(byte)
        }
    }

}
