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

extension Int32 {

    /**
     * Encode as a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that
     * can be efficiently encoded with varint. (Otherwise, negative values must be sign-extended to
     * 64 bits to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * - returns: An unsigned 32-bit integer
     */
    func zigZagEncoded() -> UInt32 {
        // Note: the right-shift must be arithmetic
        return UInt32(bitPattern: (self << 1) ^ (self >> 31))
    }

}

extension UInt32 {

    /**
     * Compute the number of bytes that would be needed to encode a varint.
     */
    var varintSize: UInt32 {
        if self & (~0 << 7) == 0 { return 1 }
        if self & (~0 << 14) == 0 { return 2 }
        if self & (~0 << 21) == 0 { return 3 }
        if self & (~0 << 28) == 0 { return 4 }
        return 5
    }

    /**
     * Decodes a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * - returns: A signed 32-bit integer.
     */
    func zigZagDecoded() -> Int32 {
        return Int32(bitPattern: (self >> 1)) ^ -(Int32(bitPattern: self) & 1)
    }

}

// MARK: -

extension Int64 {

    /**
     * Encode as a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that
     * can be efficiently encoded with varint. (Otherwise, negative values must be sign-extended to
     * 64 bits to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * - returns: An unsigned 64-bit integer
     */
    func zigZagEncoded() -> UInt64 {
        // Note: the right-shift must be arithmetic
        return UInt64(bitPattern: (self << 1) ^ (self >> 63))
    }

}

// MARK: -

extension UInt64 {

    /**
     * Encode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * - returns: An unsigned 64-bit integer, stored in a signed int because Java has no explicit
     * unsigned support.
     */
    func zigZagDecoded() -> Int64 {
        // Note: the right-shift must be arithmetic
        return Int64(bitPattern: (self >> 1)) ^ -(Int64(bitPattern: self) & 1)
    }

}
