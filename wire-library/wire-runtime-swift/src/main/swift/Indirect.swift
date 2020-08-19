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
