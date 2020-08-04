//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

/**
 Writes values into data using the protocol buffer format in a stream-like fashion.
 */
public final class ProtoWriter {

    // MARK: - Private Properties

    /**
     The number of bytes to initially reserve when doing a length-delimited encode.
     This space will be used to store the length of the data. If the length of the data
     ends up needing more or less space then the data will be moved accordingly.
     */
    private static let lengthDelimitedInitialReservedLengthSize = 2

    // MARK: - Properties

    private(set) var buffer: WriteBuffer

    var outputFormatting: ProtoEncoder.OutputFormatting

    // MARK: - Life Cycle

    init(
        data: WriteBuffer = .init(),
        outputFormatting: ProtoEncoder.OutputFormatting = []
    ) {
        self.buffer = data
        self.outputFormatting = outputFormatting
    }

    // MARK: - Public Methods - Encoding - Single Fields

    /** Encode a `bytes` field */
    public func encode(tag: UInt32, value: Data?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)

        writeVarint(key)
        let startOffset = beginLengthDelimitedEncode()
        try value.encode(to: self)
        endLengthDelimitedEncode(startOffset: startOffset)
    }

    /** Encode a `double` field */
    public func encode(tag: UInt32, value: Double?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .fixed64)
        writeVarint(key)
        try value.encode(to: self)
    }

    /** Encode an `enum` field */
    public func encode<T: RawRepresentable>(tag: UInt32, value: T?) throws where T.RawValue == UInt32 {
        guard let value = value else { return }
        encode(tag: tag, wireType: .varint, value: value) {
            writeVarint($0.rawValue)
        }
    }

    /** Encode a `float` field */
    public func encode(tag: UInt32, value: Float?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .fixed32)
        writeVarint(key)
        try value.encode(to: self)
    }

    /** Encode an `int32`, `sfixed32`, or `sint32` field */
    public func encode(tag: UInt32, value: Int32?, encoding: ProtoIntEncoding = .variable) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed32 : .varint
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)
        writeVarint(key)
        try value.encode(to: self, encoding: encoding)
    }

    /** Encode an `int64`, `sfixed64`, or `sint64` field */
    public func encode(tag: UInt32, value: Int64?, encoding: ProtoIntEncoding = .variable) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed64 : .varint
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)
        writeVarint(key)
        try value.encode(to: self, encoding: encoding)
    }

    /** Encode a `fixed32` or `uint32` field */
    public func encode(tag: UInt32, value: UInt32?, encoding: ProtoIntEncoding = .variable) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed32 : .varint
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)
        writeVarint(key)
        try value.encode(to: self, encoding: encoding)
    }

    /** Encode a `fixed64` or `uint64` field */
    public func encode(tag: UInt32, value: UInt64?, encoding: ProtoIntEncoding = .variable) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed64 : .varint
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)
        writeVarint(key)
        try value.encode(to: self, encoding: encoding)
    }

    /**
     Encode a field which has a single encoding mechanism (unlike integers).
     This includes most fields types, such as messages, strings, bytes, and floating point numbers.
     */
    public func encode(tag: UInt32, value: ProtoEncodable?) throws {
        guard let value = value else { return }

        let wireType = type(of: value).protoFieldWireType
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)

        writeVarint(key)
        if wireType == .lengthDelimited {
            let startOffset = beginLengthDelimitedEncode()
            try value.encode(to: self)
            endLengthDelimitedEncode(startOffset: startOffset)
        } else {
            try value.encode(to: self)
        }
    }

    public func encode(tag: UInt32, value: String?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)

        writeVarint(key)
        let startOffset = beginLengthDelimitedEncode()
        try value.encode(to: self)
        endLengthDelimitedEncode(startOffset: startOffset)
    }

    // MARK: - Public Methods - Encoding - Repeated Fields

    /**
     Encode a repeated `bool` field.
     This method is distinct from the generic repeated `ProtoEncodable` one because bools can be packed.
     */
    public func encode(tag: UInt32, value: [Bool]?, packed: Bool = false) throws {
        guard let value = value else { return }

        try encode(tag: tag, wireType: .varint, value: value, packed: packed) { item in
            try item.encode(to: self)
        }
    }

    /** Encode a repeated `bytes` field */
    public func encode(tag: UInt32, value: [Data]?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)
        for item in value {
            writeVarint(key)
            let startOffset = beginLengthDelimitedEncode()
            try item.encode(to: self)
            endLengthDelimitedEncode(startOffset: startOffset)
        }
    }

    /**
     Encode a repeated `double` field.
     This method is distinct from the generic repeated `ProtoEncodable` one because doubles can be packed.
     */
    public func encode(tag: UInt32, value: [Double]?, packed: Bool = false) throws {
        guard let value = value else { return }

        try encode(tag: tag, wireType: .fixed64, value: value, packed: packed) { item in
            try item.encode(to: self)
        }
    }

    /** Encoded a repeated `enum` field */
    public func encode<T: RawRepresentable>(tag: UInt32, value: [T]?, packed: Bool = false) throws where T.RawValue == UInt32 {
        guard let value = value else { return }
        encode(tag: tag, wireType: .varint, value: value, packed: packed) {
            writeVarint($0.rawValue)
        }
    }

    /**
     Encode a repeated `float` field.
     This method is distinct from the generic repeated `ProtoEncodable` one because floats can be packed.
     */
    public func encode(tag: UInt32, value: [Float]?, packed: Bool = false) throws {
        guard let value = value else { return }

        try encode(tag: tag, wireType: .fixed32, value: value, packed: packed) { item in
            try item.encode(to: self)
        }
    }

    /** Encode a repeated `int32`, `sfixed32`, or `sint32` field */
    public func encode(tag: UInt32, value: [Int32]?, encoding: ProtoIntEncoding = .variable, packed: Bool = false) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed32 : .varint
        try encode(tag: tag, wireType: wireType, value: value, packed: packed) {
            try $0.encode(to: self, encoding: encoding)
        }
    }

    /** Encode a repeated `int64`, `sfixed64`, or `sint64` field */
    public func encode(tag: UInt32, value: [Int64]?, encoding: ProtoIntEncoding = .variable, packed: Bool = false) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed64 : .varint
        try encode(tag: tag, wireType: wireType, value: value, packed: packed) {
            try $0.encode(to: self, encoding: encoding)
        }
    }

    /** Encode a repeated `string` field */
    public func encode(tag: UInt32, value: [String]?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)
        for item in value {
            writeVarint(key)
            let startOffset = beginLengthDelimitedEncode()
            try item.encode(to: self)
            endLengthDelimitedEncode(startOffset: startOffset)
        }
    }

    /** Encode a repeated  `fixed32` or `uint32` field */
    public func encode(tag: UInt32, value: [UInt32]?, encoding: ProtoIntEncoding = .variable, packed: Bool = false) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed32 : .varint
        try encode(tag: tag, wireType: wireType, value: value, packed: packed) {
            try $0.encode(to: self, encoding: encoding)
        }
    }

    /** Encode a repeated `fixed64` or `uint64` field */
    public func encode(tag: UInt32, value: [UInt64]?, encoding: ProtoIntEncoding = .variable, packed: Bool = false) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed64 : .varint
        try encode(tag: tag, wireType: wireType, value: value, packed: packed) {
            try $0.encode(to: self, encoding: encoding)
        }
    }

    /** Encode a repeated field which has a single encoding mechanism, like messages, strings, and bytes. */
    public func encode<T: ProtoEncodable>(tag: UInt32, value: [T]?) throws {
        guard let value = value else { return }

        // We can assume length-delimited here because `bool`, `double` and `float` have their
        // own overloads and all other types use wire types of length-delimited.
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)
        for item in value {
            writeVarint(key)
            let startOffset = beginLengthDelimitedEncode()
            try item.encode(to: self)
            endLengthDelimitedEncode(startOffset: startOffset)
        }
    }

    // MARK: - Public Methods - Encoding - Maps

    public func encode<V: ProtoEncodable>(tag: UInt32, value: [String: V]) throws {
        try encode(tag: tag, value: value) { key, item in
            try encode(tag: 1, value: key)
            try encode(tag: 2, value: item)
        }
    }

    public func encode<V: RawRepresentable>(tag: UInt32, value: [String: V]) throws where V.RawValue == UInt32 {
        try encode(tag: tag, value: value) { key, item in
            try encode(tag: 1, value: key)
            try encode(tag: 2, value: item.rawValue, encoding: .variable)
        }
    }

    public func encode<K: ProtoIntEncodable, V: ProtoEncodable>(
        tag: UInt32,
        value: [K: V],
        keyEncoding: ProtoIntEncoding = .variable
    ) throws {
        try encode(tag: tag, value: value) { key, item in
            try encode(tag: 1, value: key, encoding: keyEncoding)
            try encode(tag: 2, value: item)
        }
    }

    public func encode<K: ProtoIntEncodable, V: RawRepresentable>(
        tag: UInt32,
        value: [K: V],
        keyEncoding: ProtoIntEncoding = .variable
    ) throws where V.RawValue == UInt32 {
        try encode(tag: tag, value: value) { key, item in
            try encode(tag: 1, value: key, encoding: keyEncoding)
            try encode(tag: 2, value: item.rawValue, encoding: .variable)
        }
    }

    public func encode<V: ProtoIntEncodable>(
        tag: UInt32,
        value: [String: V],
        valueEncoding: ProtoIntEncoding = .variable
    ) throws {
        try encode(tag: tag, value: value) { key, item in
            try encode(tag: 1, value: key)
            try encode(tag: 2, value: item, encoding: valueEncoding)
        }
    }

    public func encode<K: ProtoIntEncodable, V: ProtoIntEncodable>(
        tag: UInt32,
        value: [K: V],
        keyEncoding: ProtoIntEncoding = .variable,
        valueEncoding: ProtoIntEncoding = .variable
    ) throws {
        try encode(tag: tag, value: value) { key, item in
            try encode(tag: 1, value: key, encoding: keyEncoding)
            try encode(tag: 2, value: item, encoding: valueEncoding)
        }
    }

    // MARK: - Public Methods - Unknown Fields

    /** Append unknown fields data to the output */
    public func writeUnknownFields(_ data: Data) throws {
        self.buffer.append(data)
    }

    // MARK: - Internal Methods - Writing Primitives

    /** Write arbitrary data */
    func write(_ data: Data) {
        self.buffer.append(data)
    }

    /** Write a single byte */
    func write(_ value: UInt8) {
        buffer.append(value)
    }

    /** Write a buffer of bytes */
    func write(_ buffer: UnsafeRawBufferPointer) {
        self.buffer.append(buffer)
    }

    func writeFixed32(_ value: Int32) {
        withUnsafeBytes(of: value.littleEndian) { buffer.append($0) }
    }

    func writeFixed64(_ value: Int64) {
        withUnsafeBytes(of: value.littleEndian) { buffer.append($0) }
    }

    /** Write a little-endian 32-bit integer.  */
    func writeFixed32(_ value: UInt32) {
        withUnsafeBytes(of: value.littleEndian) { buffer.append($0) }
    }

    /** Write a little-endian 64-bit integer.  */
    func writeFixed64(_ value: UInt64) {
        withUnsafeBytes(of: value.littleEndian) { buffer.append($0) }
    }

    func writeVarint(_ value: UInt32) {
        buffer.writeVarint(value, at: buffer.count)
    }

    func writeVarint(_ value: UInt64) {
        buffer.writeVarint(value, at: buffer.count)
    }

    // MARK: - Internal Methods - Field Keys

    /** Makes a tag value given a field number and wire type. */
    static func makeFieldKey(tag: UInt32, wireType: FieldWireType) -> UInt32 {
        return (tag << Constants.tagFieldEncodingBits) | wireType.rawValue
    }

    // MARK: - Private Methods - Field Encoder Helpers

    /** Encode a field */
    private func encode<T>(tag: UInt32, wireType: FieldWireType, value: T, encode: (T) throws -> Void) rethrows {
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)
        writeVarint(key)
        try encode(value)
    }

    /**
     Encode an integer field.
     This generic version is used in map and repeated field encoding. For individual fields there are distinct overloads for performance reasons.
     This method is `internal` and not `private` because it's used by `ProtoReader` when unknown fields are encountered.
     */
    func encode<T: ProtoIntEncodable>(tag: UInt32, value: T?, encoding: ProtoIntEncoding = .variable) throws {
        guard let value = value else { return }
        let wireType = ProtoWriter.wireType(for: type(of: value), encoding: encoding)
        try encode(tag: tag, wireType: wireType, value: value) {
            try $0.encode(to: self, encoding: encoding)
        }
    }

    /**
     Encode a generic repeated field.
     The public repeated field encoding methods should call this method to handle
     */
    private func encode<T>(tag: UInt32, wireType: FieldWireType, value: [T], packed: Bool, encode: (T) throws -> Void) rethrows {
        if packed {
            let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)
            writeVarint(key)
            let startOffset = beginLengthDelimitedEncode()
            for item in value {
                try encode(item)
            }
            endLengthDelimitedEncode(startOffset: startOffset)
        } else {
            let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)
            for item in value {
                writeVarint(key)
                try encode(item)
            }
        }
    }

    private func encode<K: Comparable, V>(tag: UInt32, value: [K: V], encode: (K, V) throws -> Void) throws {
        let fieldKey = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)

        if outputFormatting.contains(.sortedKeys) {
            // Sort the keys to get a deterministic binary output
            // This is mostly useful for testing purposes.
            let sortedKeys = value.keys.sorted()
            for key in sortedKeys {
                writeVarint(fieldKey)
                let startOffset = beginLengthDelimitedEncode()
                try encode(key, value[key]!)
                endLengthDelimitedEncode(startOffset: startOffset)
            }
        } else {
            for entry in value {
                writeVarint(fieldKey)
                let startOffset = beginLengthDelimitedEncode()
                try encode(entry.key, entry.value)
                endLengthDelimitedEncode(startOffset: startOffset)
            }
        }
    }

    private func beginLengthDelimitedEncode() -> Int {
        let startOffset = buffer.count
        let reservedSize = ProtoWriter.lengthDelimitedInitialReservedLengthSize
        for _ in 0 ..< reservedSize {
            buffer.append(0)
        }
        return startOffset
    }

    private func endLengthDelimitedEncode(startOffset: Int) {
        let reservedSize = ProtoWriter.lengthDelimitedInitialReservedLengthSize
        let writtenCount = UInt32(buffer.count - startOffset - reservedSize)

        let sizeSize = Int(writtenCount.varintSize)
        if sizeSize < reservedSize {
            buffer.remove(count: reservedSize - sizeSize, at: startOffset)
        } else if sizeSize > reservedSize {
            buffer.insert(count: sizeSize - reservedSize, at: startOffset + reservedSize)
        }

        buffer.writeVarint(writtenCount, at: startOffset)
    }

    private static func wireType<T: ProtoIntEncodable>(for valueType: T.Type, encoding: ProtoIntEncoding) -> FieldWireType {
        if encoding == .fixed {
            let size = MemoryLayout<T>.size
            if size == 4 {
                return .fixed32
            } else if size == 8 {
                return .fixed64
            } else {
                fatalError("Trying to encode integer of unexpected size \(size)")
            }
        } else {
            return .varint
        }
    }

}
