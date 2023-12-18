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

/**
 When applied to field a projected value is provided for accessing the real value
 or the default value set.
 */
@propertyWrapper
public struct CustomDefaulted<Value> {

    public var wrappedValue: Value?
    let defaultValue: Value

    public init(defaultValue: Value) {
        self.defaultValue = defaultValue
    }

    public var projectedValue: Value {
        get  {
            wrappedValue ?? defaultValue
        }
        set {
            wrappedValue = newValue
        }
    }
}

extension CustomDefaulted : Equatable where Value : Equatable {
}

extension CustomDefaulted : Hashable where Value : Hashable {
}

extension CustomDefaulted : Sendable where Value : Sendable {
}
