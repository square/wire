//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

final class ReadBuffer {

    // MARK: - Public Properties

    /** The total number of bytes in the buffer. */
    var count: Int {
        return end - start
    }

    /** The offset from the start, in bytes, so far. */
    var position: Int {
        return pointer - start
    }

    /** Whether or not there is additional data to be read. */
    var isDataRemaining: Bool {
        return pointer < end
    }

    /** A pointer to the start of the buffer. */
    let start: UnsafePointer<UInt8>

    /** A pointer to the end of the buffer. */
    let end: UnsafePointer<UInt8>

    // MARK: - Private Properties

    fileprivate(set) var pointer: UnsafePointer<UInt8>

    // MARK: - Initialization

    init(storage: UnsafePointer<UInt8>, count: Int) {
        self.start = storage
        self.end = storage.advanced(by: count)
        self.pointer = storage
    }

    func verifyAdditional(count: Int) throws {
        guard pointer.advanced(by: count) <= end else {
            throw ProtoDecoder.Error.unexpectedEndOfData
        }
    }

}

extension ReadBuffer {

    // MARK: - Reading

    func readBuffer(count: Int) throws -> UnsafeRawBufferPointer {
        try verifyAdditional(count: count)
        let buffer = UnsafeRawBufferPointer(start: pointer, count: count)
        pointer = pointer.advanced(by: count)

        return buffer
    }

    func readData(count: Int) throws -> Data {
        try verifyAdditional(count: count)
        let data = Data(bytes: pointer, count: count)
        pointer = pointer.advanced(by: count)

        return data
    }

    func readFixed32() throws -> UInt32 {
        try verifyAdditional(count: 4)
        var value: UInt32 = 0
        withUnsafeMutableBytes(of: &value) { dest -> Void in
            dest.copyMemory(from: UnsafeRawBufferPointer(start: pointer, count: 4))
        }
        pointer = pointer.advanced(by: 4)

        return UInt32(littleEndian: value)
    }

    func readFixed64() throws -> UInt64 {
        try verifyAdditional(count: 8)
        var value: UInt64 = 0
        withUnsafeMutableBytes(of: &value) { dest -> Void in
            dest.copyMemory(from: UnsafeRawBufferPointer(start: pointer, count: 8))
        }
        pointer = pointer.advanced(by: 8)

        return UInt64(littleEndian: value)
    }

    /**
     * Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits.
     */
    func readVarint32() throws -> UInt32 {
        var shift = 0
        var result: UInt32 = 0
        while shift < 32 {
            let byte = pointer.pointee
            pointer = pointer.advanced(by: 1)

            result |= UInt32(byte & 0x7f) << shift
            if (byte & 0x80) == 0 {
                // If the high bit of the byte is unset then this is
                // the last byte in the value.
                return result
            }
            shift += 7

            try verifyAdditional(count: 1)
        }

        // Discard upper 32 bits.
        for _ in 0 ..< 4 {
            let byte = pointer.pointee
            pointer = pointer.advanced(by: 1)

            if (byte & 0x80) == 0 {
                return result
            }

            try verifyAdditional(count: 1)
        }

        throw ProtoDecoder.Error.malformedVarint
    }

    /** Reads a raw varint up to 64 bits in length from the stream.  */
    func readVarint64() throws -> UInt64 {
        var shift = 0
        var result: UInt64 = 0

        while shift < 64 {
            let byte = pointer.pointee
            pointer = pointer.advanced(by: 1)

            result |= UInt64(byte & 0x7F) << shift
            if byte & 0x80 == 0 {
                return result
            }
            shift += 7

            try verifyAdditional(count: 1)
        }
        throw ProtoDecoder.Error.malformedVarint
    }

}
