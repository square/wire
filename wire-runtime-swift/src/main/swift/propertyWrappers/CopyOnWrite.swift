/*
 * Copyright (C) 2023 Square, Inc.
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

/// A property wrapper which allows easily implementing Copy-on-Write for value types.
///
/// When applied to a field, the corresponding value will always be heap-allocated.
// This happens because this wrapper is a class and classes are always heap-allocated.
///
/// Use of this wrapper is required on large value types
/// because they can overflow the Swift runtime stack.
@propertyWrapper
public struct CopyOnWrite<Value> {

    /// The underlying value represented by the property wrapper.
    ///
    /// When writing to the the property, the property wrapper will
    /// perform copy-on-write by utilizing `isKnownUniquelyReferenced`
    /// to make a copy of the underlying storage.
    public var wrappedValue: Value {
        get {
            storage.value
        }

        set {
            if isKnownUniquelyReferenced(&storage) {
                storage.value = newValue
            } else {
                storage = Storage(newValue)
            }
        }
    }

    /// The reference type which holds onto our data.
    private var storage: Storage

    /// Creates a new property wrapper with the provided value.
    public init(wrappedValue: Value) {
        storage = Storage(wrappedValue)
    }
}

// MARK: - Equatable

extension CopyOnWrite : Equatable where Value : Equatable {
    public static func == (lhs: CopyOnWrite, rhs: CopyOnWrite) -> Bool {
        lhs.wrappedValue == rhs.wrappedValue
    }
}

// MARK: - Hashable

extension CopyOnWrite : Hashable where Value : Hashable {
    public func hash(into hasher: inout Hasher) {
        wrappedValue.hash(into: &hasher)
    }
}

// MARK: - Sendable

extension CopyOnWrite : Sendable where Value : Sendable {
}

// MARK: - Storage

private extension CopyOnWrite {
    final class Storage {
        var value: Value

        init(_ value: Value) {
            self.value = value
        }
    }
}

extension CopyOnWrite.Storage : @unchecked Sendable where Value : Sendable {
}
