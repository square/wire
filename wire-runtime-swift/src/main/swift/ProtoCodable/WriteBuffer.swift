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

/**
 A data buffer class similar to `Data`, but without some of the overhead
 or unexpected allocation characteristics of that class.
 */
final class WriteBuffer {

    // MARK: - Properties

    fileprivate var storage: UnsafeMutablePointer<UInt8>!
    private(set) var capacity: Int

    fileprivate var freeOnDeinit: Bool = true

    private(set) var count: Int = 0

    // MARK: - Initialization

    init(capacity: Int = 0) {
        self.capacity = 0

        if capacity > 0 {
            expand(to: capacity)
        }
    }

    deinit {
        if freeOnDeinit {
            free(storage)
        }
    }

    // MARK: - Public Methods

    func append(_ data: Data) {
        guard !data.isEmpty else { return }

        expandIfNeeded(adding: data.count)

        data.copyBytes(to: storage.advanced(by: count), count: data.count)
        count += data.count
    }

    func append(_ value: UInt8) {
        expandIfNeeded(adding: 1)

        storage[count] = value
        count += 1
    }

    func append(_ value: [UInt8]) {
        guard !value.isEmpty else { return }

        expandIfNeeded(adding: value.count)

        for byte in value {
            storage[count] = byte
            count += 1
        }
    }

    func append(_ value: WriteBuffer) {
        precondition(value !== self)
        guard value.count > 0 else { return }

        expandIfNeeded(adding: value.count)

        memcpy(storage.advanced(by: count), value.storage, value.count)
        count += value.count
    }

    func append(_ value: UnsafeRawBufferPointer) {
        guard value.count > 0 else { return }
        
        expandIfNeeded(adding: value.count)

        memcpy(storage.advanced(by: count), value.baseAddress, value.count)
        count += value.count
    }

    // Insert bytes into the buffer and move everything else down.
    // The contents of the newly inserted bytes is undefined and is
    // expected to be overwritten in a subsequent operation.
    func insert(count: Int, at offset: Int) {
        expandIfNeeded(adding: count)

        memmove(storage.advanced(by: offset + count), storage.advanced(by: offset), self.count - offset)
        self.count += count
    }

    func remove(count: Int, at offset: Int) {
        let moveCount = self.count - (offset + count)
        memmove(storage.advanced(by: offset), storage.advanced(by: offset + count), moveCount)
        self.count -= count
    }

    func set(_ value: UInt8, at index: Int) {
        if index == count {
            expandIfNeeded(adding: 1)
            count += 1
        }
        storage[index] = value
    }

    // MARK: - Private Methods

    private func expandIfNeeded(adding: Int) {
        if capacity - count >= adding {
            return
        }

        if capacity == 0 {
            expand(to: adding)
        } else if adding < capacity {
            expand(to: capacity * 2)
        } else if adding > 1024 {
            expand(to: capacity + adding + 1024)
        } else {
            expand(to: adding * 2)
        }
    }

    private func expand(to size: Int) {
        let memory: UnsafeMutableRawPointer
        if capacity == 0 {
            memory = malloc(size)
        } else {
            memory = realloc(storage, size)
        }
        storage = memory.bindMemory(to: UInt8.self, capacity: size)
        capacity = size
    }

}

// MARK: -

extension Data {

    init(_ buffer: WriteBuffer, copyBytes: Bool) {
        if buffer.count == 0 {
            self = Data()
        } else {
            if copyBytes {
                self = Data(bytes: buffer.storage, count: buffer.count)
            } else {
                buffer.freeOnDeinit = false
                self = Data(bytesNoCopy: buffer.storage, count: buffer.count, deallocator: .free)
            }
        }
    }

}
