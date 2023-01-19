// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.custom_options.FooBar in custom_options.proto
import Foundation
import Wire

public struct FooBar {

    public var foo: Int32?
    public var bar: String?
    public var baz: Nested?
    public var qux: UInt64?
    public var fred: [Float]
    public var daisy: Double?
    public var nested: [FooBar]
    public var ext: FooBarBazEnum?
    public var rep: [FooBarBazEnum]
    public var more_string: String?
    public var unknownFields: Data = .init()

    public init(
        foo: Int32? = nil,
        bar: String? = nil,
        baz: Nested? = nil,
        qux: UInt64? = nil,
        fred: [Float] = [],
        daisy: Double? = nil,
        nested: [FooBar] = [],
        ext: FooBarBazEnum? = nil,
        rep: [FooBarBazEnum] = [],
        more_string: String? = nil
    ) {
        self.foo = foo
        self.bar = bar
        self.baz = baz
        self.qux = qux
        self.fred = fred
        self.daisy = daisy
        self.nested = nested
        self.ext = ext
        self.rep = rep
        self.more_string = more_string
    }

    public struct Nested {

        public var value: FooBarBazEnum?
        public var unknownFields: Data = .init()

        public init(value: FooBarBazEnum? = nil) {
            self.value = value
        }

    }

    public struct More {

        public var serial: [Int32]
        public var unknownFields: Data = .init()

        public init(serial: [Int32] = []) {
            self.serial = serial
        }

    }

    public enum FooBarBazEnum : UInt32, CaseIterable, ProtoEnum {

        case FOO = 1
        case BAR = 2
        case BAZ = 3

        public var description: String {
            switch self {
            case .FOO: return "FOO"
            case .BAR: return "BAR"
            case .BAZ: return "BAZ"
            }
        }

    }

}

#if !WIRE_REMOVE_EQUATABLE
extension FooBar.Nested : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension FooBar.Nested : Hashable {
}
#endif

#if swift(>=5.5)
extension FooBar.Nested : Sendable {
}
#endif

extension FooBar.Nested : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.custom_options.FooBar.Nested"
    }
}

extension FooBar.Nested : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var value: FooBar.FooBarBazEnum? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: value = try reader.decode(FooBar.FooBarBazEnum.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.value = value
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.value)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension FooBar.Nested : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: FooBar.Nested.CodingKeys.self)
        self.value = try container.decodeIfPresent(FooBar.FooBarBazEnum.self, forKey: "value")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: FooBar.Nested.CodingKeys.self)
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.value != nil {
            try container.encode(self.value, forKey: "value")
        }
    }

    public struct CodingKeys : CodingKey, ExpressibleByStringLiteral {

        public let stringValue: String
        public let intValue: Int?

        public init(stringValue: String) {
            self.stringValue = stringValue
            self.intValue = nil
        }

        public init?(intValue: Int) {
            self.stringValue = intValue.description
            self.intValue = intValue
        }

        public init(stringLiteral: String) {
            self.stringValue = stringLiteral
            self.intValue = nil
        }

    }
}
#endif

#if !WIRE_REMOVE_EQUATABLE
extension FooBar.More : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension FooBar.More : Hashable {
}
#endif

#if swift(>=5.5)
extension FooBar.More : Sendable {
}
#endif

extension FooBar.More : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.custom_options.FooBar.More"
    }
}

extension FooBar.More : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var serial: [Int32] = []

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: try reader.decode(into: &serial)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.serial = serial
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.serial)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension FooBar.More : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: FooBar.More.CodingKeys.self)
        self.serial = try container.decodeIfPresent([Int32].self, forKey: "serial") ?? []
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: FooBar.More.CodingKeys.self)
        if encoder.protoDefaultValuesEncodingStrategy == .emit || !self.serial.isEmpty {
            try container.encode(self.serial, forKey: "serial")
        }
    }

    public struct CodingKeys : CodingKey, ExpressibleByStringLiteral {

        public let stringValue: String
        public let intValue: Int?

        public init(stringValue: String) {
            self.stringValue = stringValue
            self.intValue = nil
        }

        public init?(intValue: Int) {
            self.stringValue = intValue.description
            self.intValue = intValue
        }

        public init(stringLiteral: String) {
            self.stringValue = stringLiteral
            self.intValue = nil
        }

    }
}
#endif

#if swift(>=5.5)
extension FooBar.FooBarBazEnum : Sendable {
}
#endif

#if !WIRE_REMOVE_EQUATABLE
extension FooBar : Equatable {
}
#endif

#if !WIRE_REMOVE_HASHABLE
extension FooBar : Hashable {
}
#endif

#if swift(>=5.5)
extension FooBar : Sendable {
}
#endif

extension FooBar : ProtoMessage {
    public static func protoMessageTypeURL() -> String {
        return "type.googleapis.com/squareup.protos.custom_options.FooBar"
    }
}

extension FooBar : Proto2Codable {
    public init(from reader: ProtoReader) throws {
        var foo: Int32? = nil
        var bar: String? = nil
        var baz: FooBar.Nested? = nil
        var qux: UInt64? = nil
        var fred: [Float] = []
        var daisy: Double? = nil
        var nested: [FooBar] = []
        var ext: FooBar.FooBarBazEnum? = nil
        var rep: [FooBar.FooBarBazEnum] = []
        var more_string: String? = nil

        let token = try reader.beginMessage()
        while let tag = try reader.nextTag(token: token) {
            switch tag {
            case 1: foo = try reader.decode(Int32.self)
            case 2: bar = try reader.decode(String.self)
            case 3: baz = try reader.decode(FooBar.Nested.self)
            case 4: qux = try reader.decode(UInt64.self)
            case 5: try reader.decode(into: &fred)
            case 6: daisy = try reader.decode(Double.self)
            case 7: try reader.decode(into: &nested)
            case 101: ext = try reader.decode(FooBar.FooBarBazEnum.self)
            case 102: try reader.decode(into: &rep)
            case 150: more_string = try reader.decode(String.self)
            default: try reader.readUnknownField(tag: tag)
            }
        }
        self.unknownFields = try reader.endMessage(token: token)

        self.foo = foo
        self.bar = bar
        self.baz = baz
        self.qux = qux
        self.fred = fred
        self.daisy = daisy
        self.nested = nested
        self.ext = ext
        self.rep = rep
        self.more_string = more_string
    }

    public func encode(to writer: ProtoWriter) throws {
        try writer.encode(tag: 1, value: self.foo)
        try writer.encode(tag: 2, value: self.bar)
        try writer.encode(tag: 3, value: self.baz)
        try writer.encode(tag: 4, value: self.qux)
        try writer.encode(tag: 5, value: self.fred)
        try writer.encode(tag: 6, value: self.daisy)
        try writer.encode(tag: 7, value: self.nested)
        try writer.encode(tag: 101, value: self.ext)
        try writer.encode(tag: 102, value: self.rep)
        try writer.encode(tag: 150, value: self.more_string)
        try writer.writeUnknownFields(unknownFields)
    }
}

#if !WIRE_REMOVE_CODABLE
extension FooBar : Codable {
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: FooBar.CodingKeys.self)
        self.foo = try container.decodeIfPresent(Int32.self, forKey: "foo")
        self.bar = try container.decodeIfPresent(String.self, forKey: "bar")
        self.baz = try container.decodeIfPresent(FooBar.Nested.self, forKey: "baz")
        self.qux = try container.decodeIfPresent(StringEncoded<UInt64>.self, forKey: "qux")?.wrappedValue
        self.fred = try container.decodeIfPresent([Float].self, forKey: "fred") ?? []
        self.daisy = try container.decodeIfPresent(Double.self, forKey: "daisy")
        self.nested = try container.decodeIfPresent([FooBar].self, forKey: "nested") ?? []
        self.ext = try container.decodeIfPresent(FooBar.FooBarBazEnum.self, forKey: "ext")
        self.rep = try container.decodeIfPresent([FooBar.FooBarBazEnum].self, forKey: "rep") ?? []
        self.more_string = try container.decodeIfPresent(String.self, forKey: "moreString")
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: FooBar.CodingKeys.self)
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.foo != nil {
            try container.encode(self.foo, forKey: "foo")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.bar != nil {
            try container.encode(self.bar, forKey: "bar")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.baz != nil {
            try container.encode(self.baz, forKey: "baz")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.qux != nil {
            try container.encode(StringEncoded(wrappedValue: self.qux), forKey: "qux")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || !self.fred.isEmpty {
            try container.encode(self.fred, forKey: "fred")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.daisy != nil {
            try container.encode(self.daisy, forKey: "daisy")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || !self.nested.isEmpty {
            try container.encode(self.nested, forKey: "nested")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.ext != nil {
            try container.encode(self.ext, forKey: "ext")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || !self.rep.isEmpty {
            try container.encode(self.rep, forKey: "rep")
        }
        if encoder.protoDefaultValuesEncodingStrategy == .emit || self.more_string != nil {
            try container.encode(self.more_string, forKey: "moreString")
        }
    }

    public struct CodingKeys : CodingKey, ExpressibleByStringLiteral {

        public let stringValue: String
        public let intValue: Int?

        public init(stringValue: String) {
            self.stringValue = stringValue
            self.intValue = nil
        }

        public init?(intValue: Int) {
            self.stringValue = intValue.description
            self.intValue = intValue
        }

        public init(stringLiteral: String) {
            self.stringValue = stringLiteral
            self.intValue = nil
        }

    }
}
#endif

#if !WIRE_REMOVE_REDACTABLE
extension FooBar : Redactable {
    public enum RedactedKeys : String, RedactedKey {

        case nested

    }
}
#endif
