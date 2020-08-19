/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation

public final class ProtoReader {

    // MARK: -

    /** States in the message-reading state machine */
    private enum State : Equatable {
        /// Currently reading a varint field
        case varint

        /// Currently reading a fixed64 field
        case fixed64

        /// Currently reading a length-delimited field which ends at the given offset
        case lengthDelimited(length: Int)

        /// Currently reading a fixed32 field
        case fixed32

        /// Currently reading the tag for a new field
        case tag

        /// Currently reading a value in a length-delimited chunk of packed repeated values.
        case packedValue
    }

    // MARK: -

    private struct MessageFrame {
        let isProto3: Bool
        let messageEnd: UnsafePointer<UInt8>
        var unknownFields: WriteBuffer? = nil
    }

    // MARK: - Public Properties

    static let empty = ProtoReader(buffer: .init())

    // MARK: - Private Properties

    /** The number of slots to preallocate in a collection when we encounter the first value. */
    private static let collectionPreallocationSlotCount = 5

    /**
     The maximum allowable value size for which we'll do collection preallocation.
     If values in the collection are larger than this value then we won't preallocate.
     */
    private static let collectionPreallocationMaxValueSize = 128

    private let buffer: ReadBuffer

    /**
     A stack of frames where each frame represents a level of message nesting.
     */
    private var messageStack: UnsafeMutablePointer<MessageFrame>
    private var messageStackIndex: Int = -1
    private var messageStackCapacity: Int = 5

    /**
     Is the current message a proto3 message (or a proto2 message if false).
     This is determined prior to decoding the message based on the type's protocol
     so we store it here temporarily and then copy it into the message frame in `beginMessage`.
     */
    private var isProto3Message: Bool = false

    /** The encoding of the next value to be read. */
    private var nextFieldWireType: FieldWireType? = nil

    /** How to interpret the next read call. */
    private var state: State

    // MARK: - Private Properties - Constants

    /** The standard number of levels of message nesting to allow. */
    private static let recursionLimit: Int = 65

    // MARK: - Initialization

    init(buffer: ReadBuffer) {
        self.buffer = buffer
        self.state = .lengthDelimited(length: buffer.count)

        self.messageStack = .allocate(capacity: messageStackCapacity)
    }

    deinit {
        messageStack.deallocate()
    }

    // MARK: - Public Methods - Decoding - Generated Message Body

    /**
     Begin a nested message. A call to this method will restrict the reader so that `nextTag`
     returns nil when the message is complete.
     */
    public func beginMessage() throws -> Int {
        guard case let .lengthDelimited(length) = state else {
            fatalError("Unexpected call to beginMessage()")
        }
        if length == 0 {
            // Indicate that this is an empty message.
            return -1
        }

        if (messageStackIndex + 1) > ProtoReader.recursionLimit {
            throw ProtoDecoder.Error.recursionLimitExceeded
        }

        state = .tag

        messageStackIndex += 1
        if messageStackIndex >= messageStackCapacity {
            expandMessageStack()
        }

        let frame = MessageFrame(
            isProto3: isProto3Message,
            messageEnd: buffer.pointer.advanced(by: length)
        )
        messageStack.advanced(by: messageStackIndex).initialize(to: frame)

        return messageStackIndex
    }

    /**
     Reads and returns the next tag of the message, or `nil` if there are no further tags.
     This method should be paired with calls to `beginMessage` and `endMessage`.
     This silently skips groups.
     */
    public func nextTag(token: Int) throws -> UInt32? {
        guard token != -1 else {
            // This is an empty message, so bail out.
            return nil
        }

        guard state == .tag else {
            // After reading the previous value the state should have been set to `.tag`
            fatalError("Unexpected call to nextTag. State is \(state).")
        }

        let messageEnd = messageStack[token].messageEnd
        while buffer.pointer < messageEnd && buffer.isDataRemaining {
            let (tag, wireType) = try readFieldKey()
            nextFieldWireType = wireType

            switch wireType {
            case .startGroup:
                try addUnknownField { writer in
                    try skipGroup(expectedEndTag: tag, unknownFieldsWriter: writer)
                }

            case .endGroup:
                throw ProtoDecoder.Error.unexpectedEndGroupFieldNumber(expected: nil, found: tag)

            case .lengthDelimited:
                let length = try buffer.readVarint32()
                state = .lengthDelimited(length: Int(length))
                return tag

            case .fixed32:
                state = .fixed32
                return tag

            case .fixed64:
                state = .fixed64
                return tag

            case .varint:
                state = .varint
                return tag
            }
        }

        return nil
    }

    public func endMessage(token: Int) throws -> Data {
        guard token != -1 else {
            // Special case the empty reader.
            // It's reused, so we don't want to mutate it.
            if self !== ProtoReader.empty {
                state = .tag
            }
            return Data()
        }

        let frame = messageStack[token]

        messageStackIndex -= 1

        if buffer.pointer != frame.messageEnd {
            throw ProtoDecoder.Error.invalidStructure(
                message: "Expected to end message at at \(frame.messageEnd - buffer.start), but was at \(buffer.position)"
            )
        }

        return frame.unknownFields.flatMap { Data($0, copyBytes: false) } ?? Data()
    }

    // MARK: - Public Methods - Decoding - Single Fields

    /**
     Decode enums. Note that the enums themselves do not need to be `ProtoDecodable`
     so long as they're RawRepresentable as `UInt32`
     */
    public func decode<T: RawRepresentable>(_ type: T.Type) throws -> T where T.RawValue == UInt32 {
        // Pop the enum int value and pass in to initializer
        let intValue = try readVarint32()
        guard let enumValue = T(rawValue: intValue) else {
            throw ProtoDecoder.Error.unknownEnumCase(type: T.self, fieldNumber: intValue)
        }
        return enumValue
    }

    /** Decode a `bool` field */
    public func decode(_ type: Bool.Type) throws -> Bool {
        return try Bool(from: self)
    }

    /** Decode a `bytes` field */
    public func decode(_ type: Data.Type) throws -> Data {
        return try Data(from: self)
    }

    /** Decode a `double` field */
    public func decode(_ type: Double.Type) throws -> Double {
        return try Double(from: self)
    }

    /** Decode a `float` field */
    public func decode(_ type: Float.Type) throws -> Float {
        return try Float(from: self)
    }

    /** Decode an `int32`, `sfixed32`, or `sint32` field */
    public func decode(_ type: Int32.Type, encoding: ProtoIntEncoding = .variable) throws -> Int32 {
        return try Int32(from: self, encoding: encoding)
    }

    /** Decode an `int64`, `sfixed64`, or `sint64` field */
    public func decode(_ type: Int64.Type, encoding: ProtoIntEncoding = .variable) throws -> Int64 {
        return try Int64(from: self, encoding: encoding)
    }

    /** Decode a `string` field */
    public func decode(_ type: String.Type) throws -> String {
        return try String(from: self)
    }

    /** Decode a `fixed32` or `uint32` field */
    public func decode(_ type: UInt32.Type, encoding: ProtoIntEncoding = .variable) throws -> UInt32 {
        return try UInt32(from: self, encoding: encoding)
    }

    /** Decode a `fixed64` or `uint64` field */
    public func decode(_ type: UInt64.Type, encoding: ProtoIntEncoding = .variable) throws -> UInt64 {
        return try UInt64(from: self, encoding: encoding)
    }

    /** Decode a message field */
    public func decode<T: ProtoDecodable>(_ type: T.Type) throws -> T {
        isProto3Message = T.self.protoSyntax == .proto3
        return try T(from: self)
    }

    // MARK: - Public Methods - Decoding - Proto3 Well-Known Types

    /**
     Decode a `BoolValue` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func decode(_ type: Bool.Type, boxed: Bool) throws -> Bool {
        assert(boxed, "For non-boxed values, use decode(_:)")
        return try decodeBoxed { try Bool(from: self) }
    }

    /**
     Decode a `BytesValue` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func decode(_ type: Data.Type, boxed: Bool) throws -> Data {
        assert(boxed, "For non-boxed values, use decode(_:)")
        return try decodeBoxed { try Data(from: self) }
    }

    /**
     Decode a `DoubleValue` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func decode(_ type: Double.Type, boxed: Bool) throws -> Double {
        assert(boxed, "For non-boxed values, use decode(_:)")
        return try decodeBoxed { try Double(from: self) }
    }

    /**
     Decode a `FloatValue` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func decode(_ type: Float.Type, boxed: Bool) throws -> Float {
        assert(boxed, "For non-boxed values, use decode(_:)")
        return try decodeBoxed { try Float(from: self) }
    }

    /**
     Decode a `Int32Value` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func decode(_ type: Int32.Type, boxed: Bool) throws -> Int32 {
        assert(boxed, "For non-boxed values, use decode(_:)")
        return try decodeBoxed { try Int32(from: self, encoding: .variable) }
    }

    /**
     Decode a `Int64Value` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func decode(_ type: Int64.Type, boxed: Bool) throws -> Int64 {
        assert(boxed, "For non-boxed values, use decode(_:)")
        return try decodeBoxed { try Int64(from: self, encoding: .variable) }
    }

    /**
     Decode a `StringValue` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func decode(_ type: String.Type, boxed: Bool) throws -> String {
        assert(boxed, "For non-boxed values, use decode(_:)")
        return try decodeBoxed { try String(from: self) }
    }

    /**
     Decode a `UInt32Value` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func decode(_ type: UInt32.Type, boxed: Bool) throws -> UInt32 {
        assert(boxed, "For non-boxed values, use decode(_:)")
        return try decodeBoxed { try UInt32(from: self, encoding: .variable) }
    }

    /**
     Decode a `UInt64Value` message field.
     The `boxed` argument is a placebo and just gets us a unique method signature that looks nice. It should always be `true`.
     */
    public func decode(_ type: UInt64.Type, boxed: Bool) throws -> UInt64 {
        assert(boxed, "For non-boxed values, use decode(_:)")
        return try decodeBoxed { try UInt64(from: self, encoding: .variable) }
    }

    // MARK: - Public Methods - Decoding - Repeated Fields

    /**
     Decode a repeated `bool` field.
     This method is distinct from the generic repeated `ProtoEncodable` one because bools can be packed.
     */
    public func decode(into array: inout [Bool]) throws {
        try decode(into: &array) {
            return try Bool(from: self)
        }
    }

    /** Decode a repeated `bytes` field */
    public func decode(into array: inout [Data]) throws {
        // Data fields do not support packing, so no need to test for it.
        try array.append(Data(from: self))
    }

    /**
     Decode a repeated `double` field.
     This method is distinct from the generic repeated `ProtoEncodable` one because doubles can be packed.
     */
    public func decode(into array: inout [Double]) throws {
        try decode(into: &array) {
            return try Double(from: self)
        }
    }

    /**
     Decode a repeated `float` field.
     This method is distinct from the generic repeated `ProtoEncodable` one because floats can be packed.
     */
    public func decode(into array: inout [Float]) throws {
        try decode(into: &array) {
            return try Float(from: self)
        }
    }

    /** Decode a repeated `enum` field. */
    public func decode<T: RawRepresentable>(into array: inout [T]) throws where T.RawValue == UInt32 {
        try decode(into: &array) {
            let intValue = try readVarint32()
            guard let enumValue = T(rawValue: intValue) else {
                throw ProtoDecoder.Error.unknownEnumCase(type: T.self, fieldNumber: intValue)
            }
            return enumValue
        }
    }

    /** Decode a repeated `int32`, `sfixed32`, or `sint32` field */
    public func decode(into array: inout [Int32], encoding: ProtoIntEncoding = .variable) throws {
        try decode(into: &array) {
            return try Int32(from: self, encoding: encoding)
        }
    }

    /** Decode a repeated `int64`, `sfixed64`, or `sint64` field */
    public func decode(into array: inout [Int64], encoding: ProtoIntEncoding = .variable) throws {
        try decode(into: &array) {
            return try Int64(from: self, encoding: encoding)
        }
    }

    /** Decode a repeated `string` field */
    public func decode(into array: inout [String]) throws {
        // String fields do not support packing, so no need to test for it.
        try array.append(String(from: self))
    }

    /** Decode a repeated `fixed32` or `uint32` field */
    public func decode(into array: inout [UInt32], encoding: ProtoIntEncoding = .variable) throws {
        try decode(into: &array) {
            return try UInt32(from: self, encoding: encoding)
        }
    }

    /** Decode a repeated `fixed64`, or `uint64` field */
    public func decode(into array: inout [UInt64], encoding: ProtoIntEncoding = .variable) throws {
        try decode(into: &array) {
            return try UInt64(from: self, encoding: encoding)
        }
    }

    /** Decode a repeated message field. */
    public func decode<T: ProtoDecodable>(into array: inout [T]) throws {
        // These types do not support packing, so no need to test for it.
        isProto3Message = T.self is Proto3Codable.Type
        try array.append(T(from: self))
    }

    // MARK: - Public Methods - Decoding - Maps

    /**
     Decode a single key-value pair from a map of values keyed by a `string`.
     */
    public func decode<V: ProtoDecodable>(into dictionary: inout [String: V]) throws {
        try decode(
            into: &dictionary,
            decodeKey: { try decode(String.self) },
            decodeValue: { try decode(V.self) }
        )
    }

    /**
     Decode a single key-value pair from a map of values keyed by a `string` with an `enum` value type.

     If the given value was not known at the time of generating these protos then nothing will be added to the map.
     */
    public func decode<V: RawRepresentable>(into dictionary: inout [String: V]) throws where V.RawValue == UInt32 {
        try decode(
            into: &dictionary,
            decodeKey: { try decode(String.self) },
            decodeValue: { try decode(V.self) }
        )
    }

    /**
     Decode a single key-value pair from a map of values keyed by an integer type
     */
    public func decode<K: ProtoIntDecodable, V: ProtoDecodable>(
        into dictionary: inout [K: V], keyEncoding: ProtoIntEncoding = .variable
    ) throws {
        try decode(
            into: &dictionary,
            decodeKey: { try decode(K.self, encoding: keyEncoding) },
            decodeValue: { try decode(V.self) }
        )
    }

    /**
     Decode a single key-value pair from a map of values keyed by a `string` with an integer value type.
     */
    public func decode<V: ProtoIntDecodable>(
        into dictionary: inout [String: V], valueEncoding: ProtoIntEncoding = .variable
    ) throws {
        try decode(
            into: &dictionary,
            decodeKey: { try decode(String.self) },
            decodeValue: { try decode(V.self, encoding: valueEncoding) }
        )
    }

    /** Decode a single key-value pair from a map of two integer types and add it to the given dictionary */
    public func decode<K: ProtoIntDecodable, V: ProtoIntDecodable>(
        into dictionary: inout [K: V], keyEncoding: ProtoIntEncoding = .variable, valueEncoding: ProtoIntEncoding = .variable
    ) throws {
        try decode(
            into: &dictionary,
            decodeKey: { try decode(K.self, encoding: keyEncoding) },
            decodeValue: { try decode(V.self, encoding: valueEncoding) }
        )
    }

    // MARK: - Public Methods - Unknown Fields

    /** Read an unknown field and store temporarily. The stored unknown fields will be returned from `decodeMessage` */
    public func readUnknownField(tag: UInt32) throws {
        guard let wireType = nextFieldWireType else {
            fatalError("Calling readUnknownField outside of parsing a message.")
        }
        switch wireType {
        case .fixed32:
            let value = try readFixed32()
            try addUnknownField(tag: tag, value: value, encoding: .fixed)
        case .fixed64:
            let value = try readFixed64()
            try addUnknownField(tag: tag, value: value, encoding: .fixed)
        case .lengthDelimited:
            // Treat this as bytes. There's no need to decode it fully.
            let value = try readData()
            try addUnknownField(tag: tag, value: value)
        case .varint:
            let value = try readVarint64()
            try addUnknownField(tag: tag, value: value, encoding: .variable)
        case .startGroup, .endGroup:
            fatalError("Groups are unsupported and shouldn't be our current wire type.")
        }
    }

    // MARK: - Internal Methods - Reading Primitives

    func readBuffer() throws -> UnsafeRawBufferPointer {
        guard case let .lengthDelimited(length) = state else {
            fatalError("Decoding field as length delimited when key was not LENGTH_DELIMITED")
        }
        state = .tag

        return try buffer.readBuffer(count: length)
    }

    /** Reads a `bytes` field value from the stream. The length is read from the stream prior to the actual data. */
    func readData() throws -> Data {
        guard case let .lengthDelimited(length) = state else {
            fatalError("Decoding field as length delimited when key was not LENGTH_DELIMITED")
        }
        state = .tag

        return try buffer.readData(count: length)
    }

    /** Reads a 32-bit little-endian integer from the stream.  */
    func readFixed32() throws -> UInt32 {
        precondition(state == .fixed32 || state == .packedValue)
        state = .tag
        return try buffer.readFixed32()
    }

    /** Reads a 64-bit little-endian integer from the stream.  */
    func readFixed64() throws -> UInt64 {
        precondition(state == .fixed64 || state == .packedValue)
        state = .tag
        return try buffer.readFixed64()
    }

    /** Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits. */
    func readVarint32() throws -> UInt32 {
        precondition(state == .varint || state == .packedValue)
        state = .tag

        return try buffer.readVarint32()
    }

    /** Reads a raw varint up to 64 bits in length from the stream.  */
    func readVarint64() throws -> UInt64 {
        precondition(state == .varint || state == .packedValue)
        state = .tag

        return try buffer.readVarint64()
    }

    // MARK: - Private Methods - Groups

    /** Skips a section of the input delimited by START_GROUP/END_GROUP type markers. */
    private func skipGroup(expectedEndTag: UInt32, unknownFieldsWriter: ProtoWriter) throws {
        // Preserve the group data in the unknown fields.
        // Rewrite the .startGroup key.
        let groupStartKey = ProtoWriter.makeFieldKey(tag: expectedEndTag, wireType: .startGroup)
        unknownFieldsWriter.writeVarint(groupStartKey)

        // Read fields until we find the group's corresponding end tag.
        while buffer.isDataRemaining {
            let (tag, wireType) = try readFieldKey()
            switch wireType {
            case .startGroup:
                // Nested group
                try skipGroup(expectedEndTag: tag, unknownFieldsWriter: unknownFieldsWriter)

            case .endGroup:
                guard tag == expectedEndTag else {
                    throw ProtoDecoder.Error.unexpectedEndGroupFieldNumber(expected: expectedEndTag, found: tag)
                }
                // We found the corresponding end tag.
                // Rewrite it to the unknown data.
                let groupEndKey = ProtoWriter.makeFieldKey(tag: expectedEndTag, wireType: .endGroup)
                unknownFieldsWriter.writeVarint(groupEndKey)
                return

            case .lengthDelimited:
                let length = try buffer.readVarint32()
                state = .lengthDelimited(length: Int(length))
                let data = try readData()
                try unknownFieldsWriter.encode(tag: tag, value: data)

            case .fixed32:
                state = .fixed32
                try unknownFieldsWriter.encode(tag: tag, value: try readFixed32(), encoding: .fixed)

            case .fixed64:
                state = .fixed64
                try unknownFieldsWriter.encode(tag: tag, value: try readFixed64(), encoding: .fixed)

            case .varint:
                state = .varint
                try unknownFieldsWriter.encode(tag: tag, value: try readVarint64(), encoding: .variable)
            }
        }
        throw ProtoDecoder.Error.unterminatedGroup(fieldNumber: expectedEndTag)
    }

    // MARK: - Private Methods - Decoding - Single Fields

    /**
     Decode an integer field.
     This method is used in map decoding. For single and repeated integer field decoding
     there are distinct implementations to avoid the performance cost of virtual function resolution.
     */
    private func decode<T: ProtoIntDecodable>(_ type: T.Type, encoding: ProtoIntEncoding = .variable) throws -> T {
        return try T(from: self, encoding: encoding)
    }

    private func decodeBoxed<T>(_ decode: () throws -> T) throws -> T {
        var result: T?
        let token = try beginMessage()
        while let tag = try nextTag(token: token) {
            switch tag {
            case 1: result = try decode()
            default:
                throw ProtoDecoder.Error.unexpectedFieldNumberInBoxedValue(tag)
            }
        }
        _ = try endMessage(token: token)
        guard let unwrappedResult = result else {
            throw ProtoDecoder.Error.boxedValueMissingField(type: T.self)
        }
        return unwrappedResult
    }

    private func readFieldKey() throws -> (UInt32, FieldWireType) {
        let tagAndWireType = try buffer.readVarint32()
        if tagAndWireType == 0 {
            throw ProtoDecoder.Error.fieldKeyValueZero
        }
        let tag = tagAndWireType >> Constants.tagFieldEncodingBits
        let wireTypeValue = tagAndWireType & Constants.fieldEncodingMask
        guard let wireType = FieldWireType(rawValue: wireTypeValue) else {
            throw ProtoDecoder.Error.invalidFieldWireType(wireTypeValue)
        }

        return (tag, wireType)
    }

    // MARK: - Private Methods - Decoding - Repeated Field

    private func decode<T>(into array: inout [T], decode: () throws -> T) throws {
        switch state {
        case let .lengthDelimited(length):
            // Preallocate space for the unpacked data.
            // It's allowable to have a packed field spread across multiple places
            // in the buffer, so add to the existing capacity.
            //
            // This is a rough estimate. For fixed-size values like `bool`s, `double`s,
            // `float`s, and fixed-size integers this will be accurate.
            // For variable-sized ints it will likely underestimate.
            array.reserveCapacity(array.count + (length / MemoryLayout<T>.size))

            // This is a packed field, so keep decoding until we're out of bytes.
            let messageEnd = buffer.pointer.advanced(by: length)
            while buffer.pointer < messageEnd {
                // Reading a scalar will set the state to `.tag` because we assume
                // we're reading a single value most of the time and are then done.
                // Since we're in a repeated field we'll keep reading values though.
                state = .packedValue

                array.append(try decode())
            }
        default:
            // It's faster to allocate multiple slots in the array up front rather
            // than reallocate each time we add a new item. We'll use an arbitrary
            // (small) guess, and make sure we don't do this for large message
            // structs to avoid allocating too much extra memory.
            if array.isEmpty && MemoryLayout<T>.size <= ProtoReader.collectionPreallocationMaxValueSize {
                array.reserveCapacity(ProtoReader.collectionPreallocationSlotCount)
            }

            // This is a single entry in a regular repeated field
            array.append(try decode())
        }
    }

    private func decode<K, V>(
        into dictionary: inout [K: V],
        decodeKey: () throws -> K,
        decodeValue: () throws -> V
    ) throws {
        var key: K?
        var value: V?

        let token = try beginMessage()
        while let tag = try nextTag(token: token) {
            switch tag {
            case 1: key = try decodeKey()
            case 2: value = try decodeValue()
            default:
                throw ProtoDecoder.Error.unexpectedFieldNumberInMap(tag)
            }
        }
        _ = try endMessage(token: token)

        guard let unwrappedKey = key else {
            throw ProtoDecoder.Error.mapEntryWithoutKey(value: value)
        }
        guard let unwrappedValue = value else {
            throw ProtoDecoder.Error.mapEntryWithoutValue(key: key)
        }

        // Preallocate a few empty slots to avoid reallocations.
        if dictionary.isEmpty && MemoryLayout<V>.size <= ProtoReader.collectionPreallocationMaxValueSize {
            dictionary.reserveCapacity(ProtoReader.collectionPreallocationSlotCount)
        }

        dictionary[unwrappedKey] = unwrappedValue
    }

    // MARK: - Private Methods - Message Stack

    private func expandMessageStack() {
        let newCapacity = min(messageStackCapacity * 2, ProtoReader.recursionLimit)

        let newMessageStack = UnsafeMutablePointer<MessageFrame>.allocate(capacity: newCapacity)
        newMessageStack.moveInitialize(from: messageStack, count: messageStackIndex + 1)
        messageStack.deallocate()

        messageStack = newMessageStack
        messageStackCapacity = newCapacity
    }

    // MARK: - Private Methods - Unknown Fields

    private func addUnknownField<T: ProtoIntEncodable>(tag: UInt32, value: T, encoding: ProtoIntEncoding) throws {
        try addUnknownField { writer in
            try writer.encode(tag: tag, value: value, encoding: encoding)
        }
    }

    private func addUnknownField(tag: UInt32, value: Data) throws {
        try addUnknownField { try $0.encode(tag: tag, value: value) }
    }

    private func addUnknownField(_ block: (ProtoWriter) throws -> Void) rethrows {
        let buffer = messageStack[messageStackIndex].unknownFields ?? WriteBuffer()
        let unknownFieldsWriter = ProtoWriter(data: buffer)
        try block(unknownFieldsWriter)
        messageStack[messageStackIndex].unknownFields = buffer
    }

}
