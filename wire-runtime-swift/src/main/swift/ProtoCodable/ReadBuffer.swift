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

    /// Create an empty buffer
    init() {
        var storage: UnsafePointer<UInt8>?
        Data().withUnsafeBytes {
            // You're not supposed to use the raw pointer outside of this block,
            // but (a) the empty `Data` object is a tagged pointer and won't change,
            // and (b) we're not actually ever reading from the pointer.
            storage = $0.baseAddress!.bindMemory(to: UInt8.self, capacity: 0)
        }
        self.start = storage!
        self.end = storage!
        self.pointer = storage!
    }

    init(storage: UnsafePointer<UInt8>, count: Int) {
        precondition(count >= 0)
        self.start = storage
        self.end = storage.advanced(by: count)
        self.pointer = storage
    }

    func verifyAdditional(count: Int) throws {
        _ = try endPointer(count: count)
    }

    func endPointer(count: Int) throws -> UnsafePointer<UInt8> {
        let remainingCount = end - pointer
        guard count >= 0, remainingCount >= 0, count <= remainingCount else {
            throw ProtoDecoder.Error.unexpectedEndOfData
        }
        return pointer.advanced(by: count)
    }

}

extension ReadBuffer {

    // MARK: - Reading

    func readBuffer(count: Int) throws -> UnsafeRawBufferPointer {
        let newPointer = try endPointer(count: count)
        let buffer = UnsafeRawBufferPointer(start: pointer, count: count)
        pointer = newPointer

        return buffer
    }

    func readData(count: Int) throws -> Data {
        let newPointer = try endPointer(count: count)
        let data = Data(bytes: pointer, count: count)
        pointer = newPointer

        return data
    }

    func readFixed32() throws -> UInt32 {
        let newPointer = try endPointer(count: 4)
        var value: UInt32 = 0
        withUnsafeMutableBytes(of: &value) { dest -> Void in
            dest.copyMemory(from: UnsafeRawBufferPointer(start: pointer, count: 4))
        }
        pointer = newPointer

        return UInt32(littleEndian: value)
    }

    func readFixed64() throws -> UInt64 {
        let newPointer = try endPointer(count: 8)
        var value: UInt64 = 0
        withUnsafeMutableBytes(of: &value) { dest -> Void in
            dest.copyMemory(from: UnsafeRawBufferPointer(start: pointer, count: 8))
        }
        pointer = newPointer

        return UInt64(littleEndian: value)
    }

    /** Reads a raw varint up to 64 bits in length from the stream.  */
    func readVarint() throws -> UInt64 {
        var shift = 0
        var result: UInt64 = 0

        while shift < 64 {
            try verifyAdditional(count: 1)
            let byte = pointer.pointee
            pointer = pointer.advanced(by: 1)

            result |= UInt64(byte & 0x7F) << shift
            if byte < 0x80 {
                return result
            }
            shift += 7
        }
        throw ProtoDecoder.Error.malformedVarint
    }

}
