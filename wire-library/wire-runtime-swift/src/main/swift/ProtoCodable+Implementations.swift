//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

// MARK: -

extension Bool : ProtoCodable {

    // MARK: - ProtoDecodable

    public init(from reader: ProtoReader) throws {
        self = try reader.readVarint32() == 0 ? false : true
    }

    // MARK: - ProtoEncodable

    public static var protoFieldWireType: FieldWireType { .varint }

    /** Encode a `bool` field */
    public func encode(to writer: ProtoWriter) throws {
        writer.write(self ? UInt8(1) : UInt8(0))
    }

}

// MARK: -

extension Data : ProtoCodable {

    // MARK: - ProtoDecodable

    public init(from reader: ProtoReader) throws {
        self = try reader.readData()
    }

    // MARK: - ProtoEncodable

    /** Encode a `bytes` field */
    public func encode(to writer: ProtoWriter) throws {
        writer.write(self)
    }

}

// MARK: -

extension Double : ProtoCodable {

    // MARK: - ProtoDecodable

    public init(from reader: ProtoReader) throws {
        self = try Double(bitPattern: reader.readFixed64())
    }

    // MARK: - ProtoEncodable

    public static var protoFieldWireType: FieldWireType { .fixed64 }

    /** Encode a `double` field */
    public func encode(to writer: ProtoWriter) throws {
        writer.writeFixed64(self.bitPattern)
    }

}

// MARK: -

extension Float : ProtoCodable {

    // MARK: - ProtoDecodable

    public init(from reader: ProtoReader) throws {
        self = try Float(bitPattern: reader.readFixed32())
    }

    // MARK: - ProtoEncodable

    public static var protoFieldWireType: FieldWireType { .fixed32 }

    /** Encode a `float` field */
    public func encode(to writer: ProtoWriter) throws {
        writer.writeFixed32(self.bitPattern)
    }

}

// MARK: -

extension String : ProtoCodable {

    // MARK: - ProtoDecodable

    /** Reads a `string` field value from the stream. */
    public init(from reader: ProtoReader) throws {
        let data = try reader.readData()
        guard let string = String(data: data, encoding: .utf8) else {
            throw ProtoDecoder.Error.invalidUTF8StringData(data)
        }
        self = string
    }

    // MARK: - ProtoEncodable

    /** Encode a `string` field */
    public func encode(to writer: ProtoWriter) throws {
        guard let stringData = data(using: .utf8) else {
            throw ProtoEncoder.Error.stringNotConvertibleToUTF8(self)
        }
        writer.write(stringData)
    }

}
