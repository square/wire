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
        let result = try source.readFixed32(at: Int(pos))
        pos += 4

        return result
    }

    /** Reads a 64-bit little-endian integer from the stream.  */
    func readFixed64() throws -> UInt64 {
        let result = try source.readFixed64(at: Int(pos))
        pos += 8

        return result
    }

    /**
     * Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits.
     */
    func readVarint32() throws -> UInt32 {
        let (result, size) = try source.readVarint32(at: Int(pos))
        pos += size

        return result
    }

    /** Reads a raw varint up to 64 bits in length from the stream.  */
    func readVarint64() throws -> UInt64 {
        let (result, size) = try source.readVarint64(at: Int(pos))
        pos += size

        return result
    }

}
