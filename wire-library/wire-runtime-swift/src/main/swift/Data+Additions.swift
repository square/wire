//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

extension Data {

    // MARK: - Reading

    func readFixed32(at index: Int) throws -> UInt32 {
        guard count >= index + 4 else {
            throw ProtoDecoder.Error.unexpectedEndOfData
        }
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: 4)
        self.copyBytes(to: buffer, from: index ..< index + 4)
        let littleEndianValue = (buffer.withMemoryRebound(to: UInt32.self, capacity: 1) { $0 }).pointee
        let result = UInt32(littleEndian: littleEndianValue)
        buffer.deallocate()

        return result
    }

    func readFixed64(at index: Int) throws -> UInt64 {
        guard count >= index + 8 else {
            throw ProtoDecoder.Error.unexpectedEndOfData
        }
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: 8)
        self.copyBytes(to: buffer, from: index ..< index + 8)
        let littleEndianValue = (buffer.withMemoryRebound(to: UInt64.self, capacity: 1) { $0 }).pointee
        let result = UInt64(littleEndian: littleEndianValue)
        buffer.deallocate()

        return result
    }

    /**
     * Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits.
     */
    func readVarint32(at index: Int) throws -> (value: UInt32, size: Int) {
        var shift = 0
        var result: UInt32 = 0
        var size: Int = 0
        while shift < 32 {
            guard index + size <= count else {
                throw ProtoDecoder.Error.unexpectedEndOfData
            }

            let byte = self[index + size]
            size += 1

            result |= UInt32(byte & 0x7f) << shift
            if (byte & 0x80) == 0 {
                // If the high bit of the byte is unset then this is
                // the last byte in the value.
                return (result, size)
            }

            shift += 7
        }

        // Discard upper 32 bits.
        for _ in 0 ..< 4 {
            guard index + size <= count else {
                throw ProtoDecoder.Error.unexpectedEndOfData
            }

            let byte = self[index + size]
            size += 1
            if (byte & 0x80) == 0 {
                return (result, size)
            }
        }

        throw ProtoDecoder.Error.malformedVarint
    }

    /** Reads a raw varint up to 64 bits in length from the stream.  */
    func readVarint64(at index: Int) throws -> (value: UInt64, size: Int) {
        var shift = 0
        var result: UInt64 = 0
        var size: Int = 0
        while shift < 64 {
            let byte = self[index + Int(size)]
            size += 1

            result |= UInt64(byte & 0x7F) << shift
            if byte & 0x80 == 0 {
                return (result, size)
            }
            shift += 7
        }
        throw ProtoDecoder.Error.malformedVarint
    }

    // MARK: - Writing

    mutating func writeVarint(_ value: UInt32, at index: Int) {
        // Because an unsigned (positive) varint will only use as many bytes as it needs we can
        // safely up-cast a 32-bit value to a 64-bit one for encoding purposes.
        writeVarint(UInt64(value), at: index)
    }

    /**
     * Encode a UInt64 into writable varint representation data. `value` is treated  unsigned, so it
     won't be sign-extended if negative.
     */
    mutating func writeVarint(_ value: UInt64, at index: Int) {
        var index = index
        var value = value

        while (value & ~0x7f) != 0 {
            let byte = UInt8((value & 0x7f) | 0x80)
            if index < count {
                self[index] = byte
            } else {
                append(byte)
            }
            index += 1
            value = value >> 7
        }
        let byte = UInt8(bitPattern: Int8(value))
        if index < count {
            self[index] = byte
        } else {
            append(byte)
        }
    }

}
