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

extension Int32: ProtoIntCodable, ProtoDefaultedValue {

    // MARK: - ProtoIntDecodable

    public init(from reader: ProtoReader, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // sfixed32 fields
            self = try Int32(bitPattern: reader.readFixed32())
        case .signed:
            // sint32 fields
            self = try UInt32(truncatingIfNeeded: reader.readVarint()).zigZagDecoded()
        case .variable:
            // int32 fields
            self = try Int32(truncatingIfNeeded: reader.readVarint())
        }
    }

    // MARK: - ProtoIntEncodable

    /** Encode an `int32`, `sfixed32`, or `sint32` field */
    public func encode(to writer: ProtoWriter, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // sfixed32 fields
            writer.writeFixed32(self)
        case .signed:
            // sint32 fields
            writer.writeVarint(zigZagEncoded())
        case .variable:
            // int32 fields
            if (self >= 0) {
                writer.writeVarint(UInt32(bitPattern: self))
            } else {
                // Must sign-extend.
                writer.writeVarint(UInt64(bitPattern: Int64(self)))
            }
        }
    }

}

// MARK: -

extension UInt32: ProtoIntCodable, ProtoDefaultedValue {

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
            self = try UInt32(truncatingIfNeeded: reader.readVarint())
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

extension Int64: ProtoIntCodable, ProtoDefaultedValue {

    // MARK: - ProtoIntDecodable

    public init(from reader: ProtoReader, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // sfixed64 fields
            self = try Int64(bitPattern: reader.readFixed64())
        case .signed:
            // sint64 fields
            self = try reader.readVarint().zigZagDecoded()
        case .variable:
            // int64 fields
            self = try Int64(bitPattern: reader.readVarint())
        }
    }

    // MARK: - ProtoIntEncodable

    /** Encode `int64`, `sint64`, or `sfixed64` field */
    public func encode(to writer: ProtoWriter, encoding: ProtoIntEncoding) throws {
        switch encoding {
        case .fixed:
            // sfixed64 fields
            writer.writeFixed64(self)
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

extension UInt64: ProtoIntCodable, ProtoDefaultedValue {

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
            self = try reader.readVarint()
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
