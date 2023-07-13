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

extension WriteBuffer {

    // MARK: - Writing

    func writeVarint(_ value: UInt32, at index: Int) {
        // Because an unsigned (positive) varint will only use as many bytes as it needs we can
        // safely up-cast a 32-bit value to a 64-bit one for encoding purposes.
        writeVarint(UInt64(value), at: index)
    }

    /**
     * Encode a UInt64 into writable varint representation data. `value` is treated  unsigned, so it
     won't be sign-extended if negative.
     */
    func writeVarint(_ value: UInt64, at index: Int) {
        var index = index
        var value = value

        while value > 0x7f {
            let byte = UInt8((value & 0x7f) | 0x80)
            set(byte, at: index)
            index += 1
            value = value >> 7
        }
        set(UInt8(value), at: index)
    }

}
