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
public protocol ProtoDefaultedValue {
    static var defaultedValue: Self { get }
}

// MARK: - ProtoDefaultedValue

extension Numeric where Self: ProtoDefaultedValue {
    public static var defaultedValue: Self {
        .zero
    }
}

// MARK: - ProtoDefaulted

@propertyWrapper
public struct ProtoDefaulted<Value: ProtoDefaultedValue> {
    public var wrappedValue: Value?

    public init() {
    }

    public var projectedValue: Value {
        get  {
            wrappedValue ?? Value.defaultedValue
        }
        set {
            wrappedValue = newValue
        }
    }
}

extension ProtoDefaulted : Equatable where Value : Equatable {
}

extension ProtoDefaulted : Hashable where Value : Hashable {
}

extension ProtoDefaulted : Sendable where Value : Sendable {
}
