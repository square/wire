//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

// MARK: -

extension Bool : ProtoEncodable {

    public static var protoFieldWireType: FieldWireType { .varint }

    /** Encode a `bool` field */
    public func encode(to writer: ProtoWriter) throws {
        writer.write(self ? UInt8(1) : UInt8(0))
    }

}

// MARK: -

extension Data : ProtoEncodable {

    /** Encode a `bytes` field */
    public func encode(to writer: ProtoWriter) throws {
        writer.write(self)
    }

}

// MARK: -

extension Double : ProtoEncodable {

    public static var protoFieldWireType: FieldWireType { .fixed64 }

    /** Encode a `double` field */
    public func encode(to writer: ProtoWriter) throws {
        writer.writeFixed64(self.bitPattern)
    }

}

// MARK: -

extension Float : ProtoEncodable {

    public static var protoFieldWireType: FieldWireType { .fixed32 }

    /** Encode a `float` field */
    public func encode(to writer: ProtoWriter) throws {
        writer.writeFixed32(self.bitPattern)
    }

}

// MARK: -

extension String : ProtoEncodable {

    /** Encode a `string` field */
    public func encode(to writer: ProtoWriter) throws {
        guard let stringData = data(using: .utf8) else {
            throw ProtoEncoder.Error.stringNotConvertibleToUTF8(self)
        }
        writer.write(stringData)
    }

}
