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

@propertyWrapper
public enum Indirect<T : ProtoCodable> {
    // Dedicated .none case for nil means the runtime size of this case is equal to a single
    // pointer rather than the two for the .some case.
    case none

    indirect case some(T)

    public init(value: T?) {
        if let value = value {
          self = .some(value)
        } else {
          self = .none
        }
    }

    public var wrappedValue: T? {
        get {
            switch self {
            case .none: return nil
            case .some(let value): return value
            }
        }
        set {
            if let newValue = newValue {
                self = .some(newValue)
            } else {
                self = .none
            }
        }
    }
}

extension Indirect : Codable where T : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        self.init(value: try container.decode(T.self))
    }

    public func encode(to encoder: Encoder) throws {
        try wrappedValue.encode(to: encoder)
    }
}

extension Indirect : Equatable where T : Equatable {
}

extension Indirect : Hashable where T : Hashable {
}
