@propertyWrapper
public struct JSONString<T : Hashable> : Hashable, Codable {
    public var wrappedValue: T

    public init(wrappedValue: T) {
        self.wrappedValue = wrappedValue
    }

    public init(from decoder: Decoder) throws {
        switch (T.self) {
            case is Int64.Type:
                let container = try decoder.singleValueContainer()
                self.wrappedValue = Int64(try container.decode(String.self))! as! T
            case is UInt64.Type:
                let container = try decoder.singleValueContainer()
                self.wrappedValue = UInt64(try container.decode(String.self))! as! T
            case is [Int64].Type:
                var container = try decoder.unkeyedContainer()
                var array: [Int64] = []
                while (!container.isAtEnd) {
                    array.append(Int64(try container.decode(String.self))!)
                }
                self.wrappedValue = array as! T
            case is [UInt64].Type:
                var container = try decoder.unkeyedContainer()
                var array: [UInt64] = []
                while (!container.isAtEnd) {
                    array.append(UInt64(try container.decode(String.self))!)
                }
                self.wrappedValue = array as! T
            default:
                fatalError("Unsupported type \(T.self)")
        }
    }

    public func encode(to encoder: Encoder) throws {
        switch (wrappedValue) {
            case let value as Int64:
                try String(value).encode(to: encoder)
            case let value as UInt64:
                try String(value).encode(to: encoder)
            case let array as [Int64]:
                var container = encoder.unkeyedContainer()
                for value in array {
                    try container.encode(String(value))
                }
            case let array as [UInt64]:
                var container = encoder.unkeyedContainer()
                for value in array {
                    try container.encode(String(value))
                }
            default:
                fatalError("Unsupported type \(T.self)")
        }
    }
}
