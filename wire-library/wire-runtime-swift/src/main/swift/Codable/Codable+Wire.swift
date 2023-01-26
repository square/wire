/*
 * Copyright 2023 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation

private func prefixedSequence<Value>(
    initialValue: Value,
    additionalValues: [Value]
) -> UnfoldSequence<Value, Int> {
    return sequence(state: -1) { index in
        defer {
            index += 1
        }
        switch index {
        case -1:
            return initialValue

        case 0..<additionalValues.count:
            return additionalValues[index]

        default:
            return nil
        }
    }
}

extension KeyedDecodingContainer {
    public func decodeFirstIfPresent<T>(
        _ type: T.Type,
        forKeys firstKey: Key,
        _ additionalKeys: Key...
    ) throws -> T? where T : Decodable {
        let seq = prefixedSequence(initialValue: firstKey, additionalValues: additionalKeys)

        return try seq.lazy.compactMap { key in
            try decodeIfPresent(type, forKey: key)
        }.first
    }

    public func decodeFirst<T>(
        _ type: T.Type,
        forKeys firstKey: Key,
        _ additionalKeys: Key...
    ) throws -> T where T : Decodable {
        let seq = prefixedSequence(initialValue: firstKey, additionalValues: additionalKeys)

        let value = try seq.lazy.compactMap { key in
            try decodeIfPresent(type, forKey: key)
        }.first

        guard let value else {
            throw DecodingError.keyNotFound(
                firstKey,
                DecodingError.Context(
                    codingPath: codingPath,
                    debugDescription: "decodeFirst() could find a valid key"
                )
            )
        }
        return value
    }
}
