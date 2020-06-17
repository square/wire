//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

public final class ProtoReader {

    // MARK: - Private Properties

    private let source: Data

    /** The current position in the input source, starting at 0 and increasing monotonically. */
    private var pos: UInt32 = 0

    // MARK: - Initialization

    init(data: Data) {
        self.source = data
    }

    // MARK: - Public Methods - Decoding

    public func decode<T: ProtoIntDecodable>(_ type: T.Type, encoding: ProtoIntEncoding = .variable) throws -> T {
        return try T(from: self, encoding: encoding)
    }

    // MARK: - Internal Methods - Reading Primitives

    /** Reads a 32-bit little-endian integer from the stream.  */
    func readFixed32() throws -> UInt32 {
        let result = try source.readUInt32LE(at: Int(pos))
        pos += 4

        return result
    }

    /** Reads a 64-bit little-endian integer from the stream.  */
    func readFixed64() throws -> UInt64 {
        let result = try source.readUInt64LE(at: Int(pos))
        pos += 8

        return result
    }

    /**
     * Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits.
     */
    func readVarint32() throws -> UInt32 {
        var shift = 0
        var result: UInt32 = 0
        while shift < 32 {
            let byte = source[Int(pos)]
            pos += 1

            result |= UInt32(byte & 0x7f) << shift
            if (byte & 0x80) == 0 {
                // If the high bit of the byte is unset then this is
                // the last byte in the value.
                return result
            }

            shift += 7
        }

        // Discard upper 32 bits.
        for _ in 0 ..< 4 {
            let byte = source[Int(pos)]
            pos += 1
            if (byte & 0x80) == 0 {
                return result
            }
        }

        throw ProtoDecoder.Error.malformedVarint
    }

    /** Reads a raw varint up to 64 bits in length from the stream.  */
    func readVarint64() throws -> UInt64 {
        var shift = 0
        var result: UInt64 = 0
        while shift < 64 {
            let byte = source[Int(pos)]
            pos += 1

            result |= UInt64(byte & 0x7F) << shift
            if byte & 0x80 == 0 {
                return result
            }
            shift += 7
        }
        throw ProtoDecoder.Error.malformedVarint
    }

}
