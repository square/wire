/*
 * Copyright (C) 2020 Square, Inc.
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

// MARK: -

extension Bool : ProtoCodable, ProtoDefaultedValue {

    // MARK: - ProtoDefaultedValue

    public static var defaultedValue: Bool {
        false
    }

    // MARK: - ProtoDecodable

    public static var protoSyntax: ProtoSyntax? { nil }

    public init(from reader: ProtoReader) throws {
        self = try reader.readVarint() == 0 ? false : true
    }

    // MARK: - ProtoEncodable

    public static var protoFieldWireType: FieldWireType { .varint }

    /** Encode a `bool` field */
    public func encode(to writer: ProtoWriter) throws {
        writer.write(self ? UInt8(1) : UInt8(0))
    }

}

// MARK: -

extension Data : ProtoCodable, ProtoDefaultedValue {

    // MARK: - ProtoDefaultedValue

    public static var defaultedValue: Data {
        Data()
    }

    // MARK: - ProtoDecodable

    public static var protoSyntax: ProtoSyntax? { nil }

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

extension Double : ProtoCodable, ProtoDefaultedValue {

    // MARK: - ProtoDecodable

    public static var protoSyntax: ProtoSyntax? { nil }

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

extension Float : ProtoCodable, ProtoDefaultedValue {

    // MARK: - ProtoDecodable

    public static var protoSyntax: ProtoSyntax? { nil }

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

extension String : ProtoCodable, ProtoDefaultedValue {

    // MARK: - ProtoDefaultedValue

    public static var defaultedValue: String {
        ""
    }

    // MARK: - ProtoDecodable

    public static var protoSyntax: ProtoSyntax? { nil }

    /** Reads a `string` field value from the stream. */
    public init(from reader: ProtoReader) throws {
        // Credit to the SwiftProtobuf project for this implementation.
        let buffer = try reader.readBuffer()
        var parser = Unicode.UTF8.ForwardParser()

        // Verify that the UTF-8 is valid.
        var iterator = buffer.makeIterator()
        loop: while true {
            switch parser.parseScalar(from: &iterator) {
            case .valid:
                break
            case .error:
                throw ProtoDecoder.Error.invalidUTF8StringData(Data(buffer))
            case .emptyInput:
                break loop
            }
        }

        // This initializer is fast but does not reject broken
        // UTF-8 (which is why we validate the UTF-8 above).
        self = String(decoding: buffer, as: Unicode.UTF8.self)
    }

    // MARK: - ProtoEncodable

    /** Encode a `string` field */
    public func encode(to writer: ProtoWriter) throws {
        var copy = self
        copy.withUTF8 {
            writer.write(UnsafeRawBufferPointer($0))
        }
    }

}
