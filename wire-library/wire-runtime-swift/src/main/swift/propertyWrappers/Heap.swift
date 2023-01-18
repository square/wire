/*
 * Copyright 2020 Square Inc.
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

/**
 When applied to field the corresponding value will always be heap-allocated.
 This happens because this wrapper is a class and classes are always heap-allocated.

 Use of this wrapper is required on large protobuf messages because large (and especially
 recursive) structs can overflow the Swift runtime stack.
 */
@propertyWrapper
public final class Heap<T> {

    public var wrappedValue: T

    public init(value: T) {
        self.wrappedValue = value
    }

}

// MARK: - Codable

extension Heap : Codable where T : Codable {
    convenience public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        self.init(value: try container.decode(T.self))
    }

    public func encode(to encoder: Encoder) throws {
        try wrappedValue.encode(to: encoder)
    }
}

// MARK: - Equatable

extension Heap : Equatable where T : Equatable {

    public static func == (lhs: Heap<T>, rhs: Heap<T>) -> Bool {
        return lhs.wrappedValue == rhs.wrappedValue
    }

}

// MARK: - Hashable

extension Heap : Hashable where T : Hashable {

    public func hash(into hasher: inout Hasher) {
        wrappedValue.hash(into: &hasher)
    }

}
