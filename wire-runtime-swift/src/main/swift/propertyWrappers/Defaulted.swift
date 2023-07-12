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
public struct Defaulted<T> {

    public var wrappedValue: T?
    let defaultValue: T

    public init(defaultValue: T) {
        self.defaultValue = defaultValue
    }

    public var projectedValue: T {
        wrappedValue ?? defaultValue
    }
}

extension Defaulted : Equatable where T : Equatable {
}

extension Defaulted : Hashable where T : Hashable {
}

#if swift(>=5.5)
extension Defaulted : Sendable where T : Sendable {
}
#endif
