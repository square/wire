//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

extension Data {

    func readUInt32LE(at index: Int) throws -> UInt32 {
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

    func readUInt64LE(at index: Int) throws -> UInt64 {
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

}
