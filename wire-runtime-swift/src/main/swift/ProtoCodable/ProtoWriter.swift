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

    // MARK: -

    private struct MessageFrame {
        let isProto3: Bool
    }

    // MARK: - Properties

    private(set) var buffer: WriteBuffer

    /**
     The syntax used by the root message, which can be different than child messages, which will individually set `isProto3`.
     */
    private let rootIsProto3: Bool

    /**
     A stack of frames where each frame represents a level of message nesting.
     */
    private var messageStack: UnsafeMutablePointer<MessageFrame>
    private var messageStackIndex: Int = -1
    private var messageStackCapacity: Int = 5

    /**
     Whether or not the currently being written message uses proto3 syntax.
     This is also available in the message stack, but accessing it here is faster.
     */
    private var isProto3: Bool = false

    var outputFormatting: ProtoEncoder.OutputFormatting

    // MARK: - Life Cycle

    init(
        data: WriteBuffer = .init(),
        outputFormatting: ProtoEncoder.OutputFormatting = [],
        rootMessageProtoSyntax: ProtoSyntax = .proto2
    ) {
        self.buffer = data
        self.outputFormatting = outputFormatting
        self.rootIsProto3 = (rootMessageProtoSyntax == .proto3)

        // set the initial value so that primitive types are handled correctly outside of a `messageStack` message
        self.isProto3 = rootIsProto3

        self.messageStack = .allocate(capacity: messageStackCapacity)
    }

    deinit {
        messageStack.deallocate()
    }

    // MARK: - Public Methods - Encoding - Single Fields

    /** Encode an optional `bytes` field */
    public func encode(tag: UInt32, value: Data) throws {
        if value.isEmpty && isProto3 { return }
        try encode(tag: tag, value: value as Data?)
    }

    /** Encode an optional `bytes` field */
    public func encode(tag: UInt32, value: Data?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)

        writeVarint(key)
        let startOffset = beginLengthDelimitedEncode(syntax: nil)
        try value.encode(to: self)
        endLengthDelimitedEncode(startOffset: startOffset, isMessage: false)
    }

    /** Encode a required `double` field */
    public func encode(tag: UInt32, value: Double) throws {
        if value == 0 && isProto3 { return }
        try encode(tag: tag, value: value as Double?)
    }

    /** Encode an optional `double` field */
    public func encode(tag: UInt32, value: Double?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .fixed64)
        writeVarint(key)
        try value.encode(to: self)
    }

    /** Encode a required `enum` field */
    public func encode<T: ProtoEnum>(tag: UInt32, value: T) throws where T: RawRepresentable<Int32> {
        if value.rawValue == 0 && isProto3 { return }
        try encode(tag: tag, value: value as T?)
    }

    /** Encode an optional `enum` field */
    public func encode<T: ProtoEnum>(tag: UInt32, value: T?) throws where T: RawRepresentable<Int32> {
        guard let value = value else { return }
        encode(tag: tag, wireType: .varint, value: value) {
            let uintValue = UInt32(bitPattern: $0.rawValue)
            writeVarint(uintValue)
        }
    }

    /** Encode a required `float` field */
    public func encode(tag: UInt32, value: Float) throws {
        if value == 0 && isProto3 { return }
        try encode(tag: tag, value: value as Float?)
    }

    /** Encode an optional `float` field */
    public func encode(tag: UInt32, value: Float?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .fixed32)
        writeVarint(key)
        try value.encode(to: self)
    }

    /** Encode a required `bool` field */
    public func encode(tag: UInt32, value: Bool) throws {
        if value == false && isProto3 { return }
        try encode(tag: tag, value: value as Bool?)
    }

    /** Encode an optional `bool` field */
    public func encode(tag: UInt32, value: Bool?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .varint)
        writeVarint(key)
        try value.encode(to: self)
    }

    /** Encode a required `int32`, `sfixed32`, or `sint32` field */
    public func encode(tag: UInt32, value: Int32, encoding: ProtoIntEncoding = .variable) throws {
        // Don't encode default values if using proto3 syntax.
        if value == 0 && isProto3 { return }
        try encode(tag: tag, value: value as Int32?, encoding: encoding)
    }

    /** Encode an optional `int32`, `sfixed32`, or `sint32` field */
    public func encode(tag: UInt32, value: Int32?, encoding: ProtoIntEncoding = .variable) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed32 : .varint
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)
        writeVarint(key)
        try value.encode(to: self, encoding: encoding)
    }

    /** Encode a required `int64`, `sfixed64`, or `sint64` field */
    public func encode(tag: UInt32, value: Int64, encoding: ProtoIntEncoding = .variable) throws {
        // Don't encode default values if using proto3 syntax.
        if value == 0 && isProto3 { return }
        try encode(tag: tag, value: value as Int64?, encoding: encoding)
    }

    /** Encode an optional `int64`, `sfixed64`, or `sint64` field */
    public func encode(tag: UInt32, value: Int64?, encoding: ProtoIntEncoding = .variable) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed64 : .varint
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)
        writeVarint(key)
        try value.encode(to: self, encoding: encoding)
    }

    /** Encode a required `fixed32` or `uint32` field */
    public func encode(tag: UInt32, value: UInt32, encoding: ProtoIntEncoding = .variable) throws {
        // Don't encode default values if using proto3 syntax.
        if value == 0 && isProto3 { return }
        try encode(tag: tag, value: value as UInt32?, encoding: encoding)
    }

    /** Encode an optional `fixed32` or `uint32` field */
    public func encode(tag: UInt32, value: UInt32?, encoding: ProtoIntEncoding = .variable) throws {
        guard let value = value else { return }
        let wireType: FieldWireType = encoding == .fixed ? .fixed32 : .varint
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)
        writeVarint(key)
        try value.encode(to: self, encoding: encoding)
    }

    /** Encode a required `fixed64` or `uint64` field */
    public func encode(tag: UInt32, value: UInt64, encoding: ProtoIntEncoding = .variable) throws {
        // Don't encode default values if using proto3 syntax.
        if value == 0 && isProto3 { return }
        try encode(tag: tag, value: value as UInt64?, encoding: encoding)
    }

    /** Encode an optional `fixed64` or `uint64` field */
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

        let valueType = type(of: value)
        let wireType = valueType.protoFieldWireType
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: wireType)

        writeVarint(key)
        if wireType == .lengthDelimited {
            let syntax = valueType.protoSyntax
            let startOffset = beginLengthDelimitedEncode(syntax: syntax)
            try value.encode(to: self)
            endLengthDelimitedEncode(startOffset: startOffset, isMessage: syntax != nil)
        } else {
            try value.encode(to: self)
        }
    }

    /** Encode a required `string` field */
    public func encode(tag: UInt32, value: String) throws {
        if value.isEmpty && isProto3 { return }
        try encode(tag: tag, value: value as String?)
    }

    /** Encode an optional `string` field */
    public func encode(tag: UInt32, value: String?) throws {
        guard let value = value else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)

        writeVarint(key)
        let startOffset = beginLengthDelimitedEncode(syntax: nil)
        try value.encode(to: self)
        endLengthDelimitedEncode(startOffset: startOffset, isMessage: false)
    }

    // MARK: - Public Methods - Encoding - Proto3 Well-Known Types

    /**
     Encode a `BoolValue` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func encode(tag: UInt32, value: Bool?, boxed: Bool) throws {
        guard let value = value else { return }
        try encodeBoxed(tag: tag) { try encode(tag: 1, value: value) }
    }

    /**
     Encode a `BytesValue` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func encode(tag: UInt32, value: Data?, boxed: Bool) throws {
        guard let value = value else { return }
        try encodeBoxed(tag: tag) { try encode(tag: 1, value: value) }
    }

    /**
     Encode a `DoubleValue` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func encode(tag: UInt32, value: Double?, boxed: Bool) throws {
        guard let value = value else { return }
        try encodeBoxed(tag: tag) { try encode(tag: 1, value: value) }
    }

    /**
     Encode a `FloatValue` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func encode(tag: UInt32, value: Float?, boxed: Bool) throws {
        guard let value = value else { return }
        try encodeBoxed(tag: tag) { try encode(tag: 1, value: value) }
    }

    /**
     Encode a `Int32Value` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func encode(tag: UInt32, value: Int32?, boxed: Bool) throws {
        guard let value = value else { return }
        try encodeBoxed(tag: tag) { try encode(tag: 1, value: value, encoding: .variable) }
    }

    /**
     Encode a `Int64Value` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func encode(tag: UInt32, value: Int64?, boxed: Bool) throws {
        guard let value = value else { return }
        try encodeBoxed(tag: tag) { try encode(tag: 1, value: value, encoding: .variable) }
    }

    /**
     Encode a `StringValue` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func encode(tag: UInt32, value: String?, boxed: Bool) throws {
        guard let value = value else { return }
        try encodeBoxed(tag: tag) { try encode(tag: 1, value: value) }
    }

    /**
     Encode a `UInt32Value` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func encode(tag: UInt32, value: UInt32?, boxed: Bool) throws {
        guard let value = value else { return }
        try encodeBoxed(tag: tag) { try encode(tag: 1, value: value, encoding: .variable) }
    }

    /**
     Encode a `UInt64Value` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func encode(tag: UInt32, value: UInt64?, boxed: Bool) throws {
        guard let value = value else { return }
        try encodeBoxed(tag: tag) { try encode(tag: 1, value: value, encoding: .variable) }
    }

    // MARK: - Public Methods - Encoding - Repeated Fields

    /**
     Encode a repeated `bool` field.
     This method is distinct from the generic repeated `ProtoEncodable` one because bools can be packed.
     */
    public func encode(tag: UInt32, value: [Bool], packed: Bool? = nil) throws {
        guard !value.isEmpty else { return }

        try encode(tag: tag, wireType: .varint, value: value, packed: packed) { item in
            try item.encode(to: self)
        }
    }

    /** Encode a repeated `bytes` field */
    public func encode(tag: UInt32, value: [Data]) throws {
        guard !value.isEmpty else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)
        for item in value {
            writeVarint(key)
            let startOffset = beginLengthDelimitedEncode(syntax: nil)
            try item.encode(to: self)
            endLengthDelimitedEncode(startOffset: startOffset, isMessage: false)
        }
    }

    /**
     Encode a repeated `double` field.
     This method is distinct from the generic repeated `ProtoEncodable` one because doubles can be packed.
     */
    public func encode(tag: UInt32, value: [Double], packed: Bool? = nil) throws {
        guard !value.isEmpty else { return }

        try encode(tag: tag, wireType: .fixed64, value: value, packed: packed) { item in
            try item.encode(to: self)
        }
    }

    /** Encoded a repeated `enum` field */
    public func encode<T: ProtoEnum>(tag: UInt32, value: [T], packed: Bool? = nil) throws where T: RawRepresentable<Int32> {
        guard !value.isEmpty else { return }

        encode(tag: tag, wireType: .varint, value: value, packed: packed) {
            let uintValue = UInt32(bitPattern: $0.rawValue)
            writeVarint(uintValue)
        }
    }

    /**
     Encode a repeated `float` field.
     This method is distinct from the generic repeated `ProtoEncodable` one because floats can be packed.
     */
    public func encode(tag: UInt32, value: [Float], packed: Bool? = nil) throws {
        guard !value.isEmpty else { return }

        try encode(tag: tag, wireType: .fixed32, value: value, packed: packed) { item in
            try item.encode(to: self)
        }
    }

    /** Encode a repeated `int32`, `sfixed32`, or `sint32` field */
    public func encode(tag: UInt32, value: [Int32], encoding: ProtoIntEncoding = .variable, packed: Bool? = nil) throws {
        guard !value.isEmpty else { return }

        let wireType: FieldWireType = encoding == .fixed ? .fixed32 : .varint
        try encode(tag: tag, wireType: wireType, value: value, packed: packed) {
            try $0.encode(to: self, encoding: encoding)
        }
    }

    /** Encode a repeated `int64`, `sfixed64`, or `sint64` field */
    public func encode(tag: UInt32, value: [Int64], encoding: ProtoIntEncoding = .variable, packed: Bool? = nil) throws {
        guard !value.isEmpty else { return }

        let wireType: FieldWireType = encoding == .fixed ? .fixed64 : .varint
        try encode(tag: tag, wireType: wireType, value: value, packed: packed) {
            try $0.encode(to: self, encoding: encoding)
        }
    }

    /** Encode a repeated `string` field */
    public func encode(tag: UInt32, value: [String]) throws {
        guard !value.isEmpty else { return }

        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)
        for item in value {
            writeVarint(key)
            let startOffset = beginLengthDelimitedEncode(syntax: nil)
            try item.encode(to: self)
            endLengthDelimitedEncode(startOffset: startOffset, isMessage: false)
        }
    }

    /** Encode a repeated  `fixed32` or `uint32` field */
    public func encode(tag: UInt32, value: [UInt32], encoding: ProtoIntEncoding = .variable, packed: Bool? = nil) throws {
        guard !value.isEmpty else { return }

        let wireType: FieldWireType = encoding == .fixed ? .fixed32 : .varint
        try encode(tag: tag, wireType: wireType, value: value, packed: packed) {
            try $0.encode(to: self, encoding: encoding)
        }
    }

    /** Encode a repeated `fixed64` or `uint64` field */
    public func encode(tag: UInt32, value: [UInt64], encoding: ProtoIntEncoding = .variable, packed: Bool? = nil) throws {
        let wireType: FieldWireType = encoding == .fixed ? .fixed64 : .varint
        try encode(tag: tag, wireType: wireType, value: value, packed: packed) {
            try $0.encode(to: self, encoding: encoding)
        }
    }

    /** Encode a repeated field which has a single encoding mechanism, like messages, strings, and bytes. */
    public func encode<T: ProtoEncodable>(tag: UInt32, value: [T]) throws {
        guard !value.isEmpty else { return }

        // We can assume length-delimited here because `bool`, `double` and `float` have their
        // own overloads and all other types use wire types of length-delimited.
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)
        let syntax = T.protoSyntax
        for item in value {
            writeVarint(key)
            let startOffset = beginLengthDelimitedEncode(syntax: syntax)
            try item.encode(to: self)
            endLengthDelimitedEncode(startOffset: startOffset, isMessage: syntax != nil)
        }
    }

    // MARK: - Public Methods - Encoding - Maps

    public func encode<V: ProtoEncodable>(tag: UInt32, value: [String: V]) throws {
        guard !value.isEmpty else { return }

        try encode(tag: tag, value: value) { key, item in
            try encode(tag: 1, value: key)
            try encode(tag: 2, value: item)
        }
    }

    public func encode<V: ProtoEnum>(tag: UInt32, value: [String: V]) throws where V: RawRepresentable<Int32> {
        guard !value.isEmpty else { return }

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
        guard !value.isEmpty else { return }

        try encode(tag: tag, value: value) { key, item in
            try encode(tag: 1, value: key, encoding: keyEncoding)
            try encode(tag: 2, value: item)
        }
    }

    public func encode<K: ProtoIntEncodable, V: ProtoEnum>(
        tag: UInt32,
        value: [K: V],
        keyEncoding: ProtoIntEncoding = .variable
    ) throws where V: RawRepresentable<Int32> {
        guard !value.isEmpty else { return }

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
        guard !value.isEmpty else { return }

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
        guard !value.isEmpty else { return }

        try encode(tag: tag, value: value) { key, item in
            try encode(tag: 1, value: key, encoding: keyEncoding)
            try encode(tag: 2, value: item, encoding: valueEncoding)
        }
    }

    // MARK: - Public Methods - Unknown Fields

    /** Append unknown fields data to the output */
    public func writeUnknownFields(_ fields: UnknownFields) throws {
        fields.values.forEach {
            self.buffer.append($0)
        }
    }

    /** Append unknown fields data to the output */
    public func writeUnknownFields(_ fields: ExtensibleUnknownFields) throws {
        try writeUnknownFields(fields.rawData)
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
    private func encode<T>(tag: UInt32, wireType: FieldWireType, value: [T], packed: Bool?, encode: (T) throws -> Void) rethrows {
        let packed: Bool = packed ?? isProto3
        if packed {
            let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)
            writeVarint(key)
            let startOffset = beginLengthDelimitedEncode(syntax: nil)
            for item in value {
                try encode(item)
            }
            endLengthDelimitedEncode(startOffset: startOffset, isMessage: false)
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

        let syntax = (V.self as? ProtoEncodable.Type)?.protoSyntax
        if outputFormatting.contains(.sortedKeys) {
            // Sort the keys to get a deterministic binary output
            // This is mostly useful for testing purposes.
            let sortedKeys = value.keys.sorted()
            for key in sortedKeys {
                writeVarint(fieldKey)
                let startOffset = beginLengthDelimitedEncode(syntax: syntax)
                try encode(key, value[key]!)
                endLengthDelimitedEncode(startOffset: startOffset, isMessage: syntax != nil)
            }
        } else {
            for entry in value {
                writeVarint(fieldKey)
                let startOffset = beginLengthDelimitedEncode(syntax: syntax)
                try encode(entry.key, entry.value)
                endLengthDelimitedEncode(startOffset: startOffset, isMessage: syntax != nil)
            }
        }
    }

    private func encodeBoxed(tag: UInt32, _ encode: () throws -> Void) rethrows {
        let key = ProtoWriter.makeFieldKey(tag: tag, wireType: .lengthDelimited)
        writeVarint(key)

        let startOffset = beginLengthDelimitedEncode(syntax: nil)
        try encode()
        endLengthDelimitedEncode(startOffset: startOffset, isMessage: false)
    }

    /**
     Encodes a length-delimited field by:
     - Call `beginLengthDelimitedEncode` to reserve some space to write the length out
     - Writing the data out in the calling method
     - Call `endLengthDelimitedEncode` to write the size of the data into the reserved space,
       expanding or contracting the space as needed based on the amount of data written.
     */
    private func beginLengthDelimitedEncode(syntax: ProtoSyntax?) -> Int {
        let isProto3: Bool?
        if let syntax = syntax {
            isProto3 = syntax == .proto3
        } else {
            // No need for a stack frame as there won't be additional recursion.
            // This is a simple string, bytes, or other well-known type.
            isProto3 = nil
        }

        if let isProto3 = isProto3 {
            if messageStackIndex + 1 >= messageStackCapacity {
                expandMessageStack()
            }
            let frame = MessageFrame(
                isProto3: isProto3
            )
            messageStackIndex += 1
            messageStack.advanced(by: messageStackIndex).initialize(to: frame)

            // Cache this value as an ivar for quick access.
            self.isProto3 = isProto3
        }

        // Reserve some space for the encoded size of the field.
        // If this guess ends up being wrong we'll adjust it after the encode.
        let startOffset = buffer.count
        let reservedSize = ProtoWriter.lengthDelimitedInitialReservedLengthSize
        for _ in 0 ..< reservedSize {
            buffer.append(0)
        }
        return startOffset
    }

    private func endLengthDelimitedEncode(startOffset: Int, isMessage: Bool) {
        let reservedSize = ProtoWriter.lengthDelimitedInitialReservedLengthSize
        let writtenCount = UInt32(buffer.count - startOffset - reservedSize)

        let sizeSize = Int(writtenCount.varintSize)
        if sizeSize < reservedSize {
            buffer.remove(count: reservedSize - sizeSize, at: startOffset)
        } else if sizeSize > reservedSize {
            buffer.insert(count: sizeSize - reservedSize, at: startOffset + reservedSize)
        }

        buffer.writeVarint(writtenCount, at: startOffset)

        if isMessage {
            messageStackIndex -= 1

            // We popped the stack, so update the quick-access ivar.
            // If this is the last item in the stack, use the root value
            if messageStackIndex >= 0 {
                isProto3 = messageStack[messageStackIndex].isProto3
            } else {
                isProto3 = rootIsProto3
            }
        }
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

    // MARK: - Private Methods - Message Stack

    private func expandMessageStack() {
        let newCapacity = messageStackCapacity * 2

        let newMessageStack = UnsafeMutablePointer<MessageFrame>.allocate(capacity: newCapacity)
        newMessageStack.moveInitialize(from: messageStack, count: messageStackIndex + 1)
        messageStack.deallocate()

        messageStack = newMessageStack
        messageStackCapacity = newCapacity
    }

}
