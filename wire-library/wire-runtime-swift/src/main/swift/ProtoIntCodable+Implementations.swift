//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

// MARK: -

extension Int32: ProtoIntCodable {

    // MARK: - ProtoIntDecodable

    public init(from reader: ProtoReader, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // sfixed32 fields
            self = try Int32(bitPattern: reader.readFixed32())
        case .signed:
            // sint32 fields
            self = try reader.readVarint32().zigZagDecoded()
        case .variable:
            // int32 fields
            self = try Int32(bitPattern: reader.readVarint32())
        }
    }

    // MARK: - ProtoIntEncodable

    /** Encode an `int32`, `sfixed32`, or `sint32` field */
    public func encode(to writer: ProtoWriter, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // sfixed32 fields
            writer.writeFixed32(UInt32(bitPattern: self))
        case .signed:
            // sint32 fields
            writer.writeVarint(zigZagEncoded())
        case .variable:
            // int32 fields
            writer.writeVarint(UInt32(bitPattern: self))
        }
    }

}

// MARK: -

extension UInt32: ProtoIntCodable {

    // MARK: - ProtoIntDecodable

    public init(from reader: ProtoReader, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // fixed32 fields
            self = try reader.readFixed32()
        case .signed:
            fatalError("Unsupported")
        case .variable:
             // uint32 fields
            self = try reader.readVarint32()
        }
    }

    // MARK: - ProtoIntEncodable

    /** Encode a `uint32` or `fixed32` field */
    public func encode(to writer: ProtoWriter, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // fixed32 fields
            writer.writeFixed32(self)
        case .signed:
            fatalError("Unsupported")
        case .variable:
            // uint32 fields
            writer.writeVarint(self)
        }
    }

}

// MARK: -

extension Int64: ProtoIntCodable {

    // MARK: - ProtoIntDecodable

    public init(from reader: ProtoReader, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // sfixed64 fields
            self = try Int64(bitPattern: reader.readFixed64())
        case .signed:
            // sint64 fields
            self = try reader.readVarint64().zigZagDecoded()
        case .variable:
            // int64 fields
            self = try Int64(bitPattern: reader.readVarint64())
        }
    }

    // MARK: - ProtoIntEncodable

    /** Encode `int64`, `sint64`, or `sfixed64` field */
    public func encode(to writer: ProtoWriter, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // sfixed64 fields
            writer.writeFixed64(UInt64(bitPattern: self))
        case .signed:
            // sint64 fields
            writer.writeVarint(zigZagEncoded())
        case .variable:
            // int64 fields
            writer.writeVarint(UInt64(bitPattern: self))
        }
    }

}

// MARK: -

extension UInt64: ProtoIntCodable {

    // MARK: - ProtoIntDecodable

    public init(from reader: ProtoReader, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // fixed64 fields
            self = try reader.readFixed64()
        case .signed:
            fatalError("Unsupported")
        case .variable:
            // uint64 fields
            self = try reader.readVarint64()
        }
    }

    // MARK: - ProtoIntEncodable

    /** Encode a `uint64` or `fixed64` field */
    public func encode(to writer: ProtoWriter, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // fixed64 fields
            writer.writeFixed64(self)
        case .signed:
            fatalError("Unsupported")
        case .variable:
            // uint64 fields
            writer.writeVarint(self)
        }
    }

}
