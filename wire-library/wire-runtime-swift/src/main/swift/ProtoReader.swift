//
//  Copyright © 2020 Square Inc. All rights reserved.
//

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
        case lengthDelimited(endOffset: Int)

        /// Currently reading a fixed32 field
        case fixed32

        /// Currently reading the tag for a new field
        case tag

        /// Currently reading a value in a length-delimited chunk of packed repeated values.
        case packedValue
    }

    // MARK: - Private Properties

    private let data: Data

    /** The current position in the input source, starting at 0 and increasing monotonically. */
    private var pos: Int = 0

    /** Buffers for unknown fields as a stack corresponding to message nesting.. */
    private var unknownFieldsStack: [WriteBuffer?] = []

    /** The encoding of the next value to be read. */
    private var nextFieldWireType: FieldWireType? = nil

    /** How to interpret the next read call. */
    private var state: State

    // MARK: - Private Properties - Constants

    /** The standard number of levels of message nesting to allow. */
    private static let recursionLimit: UInt = 65

    // MARK: - Initialization

    init(data: Data) {
        self.data = data
        self.state = .lengthDelimited(endOffset: data.count)
        self.unknownFieldsStack.append(nil)
    }

    // MARK: - Public Methods - Iterating Tags

    /** Reads each tag within a message, handles it, and returns a byte string with the unknown fields. */
    public func forEachTag(_ block: (UInt32) throws -> Void) throws -> Data {
        return try decodeMessage { endOffset in
            while true {
                guard let tag = try nextTag(messageEndOffset: endOffset) else { break }
                try block(tag)
            }
        }
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

    /** Decode an `int32` field */
    public func decode(_ type: Int32.Type, encoding: ProtoIntEncoding = .variable) throws -> Int32 {
        return try Int32(from: self, encoding: encoding)
    }

    /** Decode an `int64` field */
    public func decode(_ type: Int64.Type, encoding: ProtoIntEncoding = .variable) throws -> Int64 {
        return try Int64(from: self, encoding: encoding)
    }

    /** Decode a `string` field */
    public func decode(_ type: String.Type) throws -> String {
        return try String(from: self)
    }

    /** Decode an `uint32` field */
    public func decode(_ type: UInt32.Type, encoding: ProtoIntEncoding = .variable) throws -> UInt32 {
        return try UInt32(from: self, encoding: encoding)
    }

    /** Decode an `uint64` field */
    public func decode(_ type: UInt64.Type, encoding: ProtoIntEncoding = .variable) throws -> UInt64 {
        return try UInt64(from: self, encoding: encoding)
    }

    /** Decode a message field */
    public func decode<T: ProtoDecodable>(_ type: T.Type) throws -> T {
        return try T(from: self)
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

    /** Decode a repeated `int32` field */
    public func decode(into array: inout [Int32], encoding: ProtoIntEncoding = .variable) throws {
        try decode(into: &array) {
            return try Int32(from: self, encoding: encoding)
        }
    }

    /** Decode a repeated `int64` field */
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

    /** Decode a repeated `uint32` field */
    public func decode(into array: inout [UInt32], encoding: ProtoIntEncoding = .variable) throws {
        try decode(into: &array) {
            return try UInt32(from: self, encoding: encoding)
        }
    }

    /** Decode a repeated `uint64` field */
    public func decode(into array: inout [UInt64], encoding: ProtoIntEncoding = .variable) throws {
        try decode(into: &array) {
            return try UInt64(from: self, encoding: encoding)
        }
    }

    /** Decode a repeated message field. */
    public func decode<T: ProtoDecodable>(into array: inout [T]) throws {
        // These types do not support packing, so no need to test for it.
        try array.append(T(from: self))
    }

    // MARK: - Public Methods - Decoding - Maps

    /**
     Decode a single key-value pair from a map of values keyed by a `string`.
     */
    public func decode<V: ProtoDecodable>(into dictionary: inout [String: V]) throws {
        let (key, value) = try decode(
            decodeKey: { try decode(String.self) },
            decodeValue: { try decode(V.self) }
        )
        dictionary[key] = value
    }

    /**
     Decode a single key-value pair from a map of values keyed by a `string` with an `enum` value type.

     If the given value was not known at the time of generating these protos then nothing will be added to the map.
     */
    public func decode<V: RawRepresentable>(into dictionary: inout [String: V]) throws where V.RawValue == UInt32 {
        let (key, value) = try decode(
            decodeKey: { try decode(String.self) },
            decodeValue: { try decode(V.self) }
        )
        dictionary[key] = value
    }

    /**
     Decode a single key-value pair from a map of values keyed by an integer type
     */
    public func decode<K: ProtoIntDecodable, V: ProtoDecodable>(
        into dictionary: inout [K: V], keyEncoding: ProtoIntEncoding = .variable
    ) throws {
        let (key, value) = try decode(
            decodeKey: { try decode(K.self, encoding: keyEncoding) },
            decodeValue: { try decode(V.self) }
        )
        dictionary[key] = value
    }

    /**
     Decode a single key-value pair from a map of values keyed by a `string` with an integer value type.
     */
    public func decode<V: ProtoIntDecodable>(
        into dictionary: inout [String: V], valueEncoding: ProtoIntEncoding = .variable
    ) throws {
        let (key, value) = try decode(
            decodeKey: { try decode(String.self) },
            decodeValue: { try decode(V.self, encoding: valueEncoding) }
        )
        dictionary[key] = value
    }

    /** Decode a single key-value pair from a map of two integer types and add it to the given dictionary */
    public func decode<K: ProtoIntDecodable, V: ProtoIntDecodable>(
        into dictionary: inout [K: V], keyEncoding: ProtoIntEncoding = .variable, valueEncoding: ProtoIntEncoding = .variable
    ) throws {
        let (key, value) = try decode(
            decodeKey: { try decode(K.self, encoding: keyEncoding) },
            decodeValue: { try decode(V.self, encoding: valueEncoding) }
        )
        dictionary[key] = value
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

    /** Reads a `bytes` field value from the stream. The length is read from the stream prior to the actual data. */
    func readData() throws -> Data {
        guard case let .lengthDelimited(endOffset) = state else {
            fatalError("Decoding field as length delimited when key was not LENGTH_DELIMITED")
        }

        guard endOffset <= data.count else {
            throw ProtoDecoder.Error.unexpectedEndOfData
        }

        let data = self.data.subdata(in: pos ..< endOffset)
        pos = endOffset
        state = .tag

        return data
    }

    /** Reads a 32-bit little-endian integer from the stream.  */
    func readFixed32() throws -> UInt32 {
        precondition(state == .fixed32 || state == .packedValue)

        let result = try data.readFixed32(at: pos)
        pos += 4
        state = .tag

        return result
    }

    /** Reads a 64-bit little-endian integer from the stream.  */
    func readFixed64() throws -> UInt64 {
        precondition(state == .fixed64 || state == .packedValue)

        let result = try data.readFixed64(at: pos)
        pos += 8
        state = .tag

        return result
    }

    /** Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits. */
    func readVarint32() throws -> UInt32 {
        precondition(state == .varint || state == .packedValue)

        let (result, size) = try data.readVarint32(at: pos)
        pos += size
        state = .tag

        return result
    }

    /** Reads a raw varint up to 64 bits in length from the stream.  */
    func readVarint64() throws -> UInt64 {
        precondition(state == .varint || state == .packedValue)

        let (result, size) = try data.readVarint64(at: pos)
        pos += size
        state = .tag

        return result
    }

    // MARK: - Private Methods - Groups

    /** Skips a section of the input delimited by START_GROUP/END_GROUP type markers. */
    private func skipGroup(expectedEndTag: UInt32, unknownFieldsWriter: ProtoWriter) throws {
        // Preserve the group data in the unknown fields.
        // Rewrite the .startGroup key.
        let groupStartKey = ProtoWriter.makeFieldKey(tag: expectedEndTag, wireType: .startGroup)
        unknownFieldsWriter.writeVarint(groupStartKey)

        // Read fields until we find the group's corresponding end tag.
        while pos < data.count {
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
                let (length, size) = try data.readVarint32(at: pos)
                pos += size
                state = .lengthDelimited(endOffset: pos + Int(length))
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

    /**
     Begin a nested message. A call to this method will restrict the reader so that [nextTag]
     returns nil when the message is complete.

     - parameter decode: A block which is called to actually decode the message. The parameter
                         to the block is the end offset of the message.
     - returns: Returns all unknown fields in the message, encoded sequentially.
     */
    private func decodeMessage(_ decode: (_ endOffset: Int) throws -> Void) throws -> Data {
        guard case let .lengthDelimited(endOffset) = state else {
            fatalError("Unexpected call to decodeMessage()")
        }

        if unknownFieldsStack.count > ProtoReader.recursionLimit {
            throw ProtoDecoder.Error.recursionLimitExceeded
        }

        state = .tag

        unknownFieldsStack.append(nil)
        try decode(endOffset)
        let unknownFieldsData = unknownFieldsStack.popLast()!

        if pos != endOffset {
            throw ProtoDecoder.Error.invalidStructure(
                message: "Expected to end message at \(endOffset), but was at \(pos)"
            )
        }

        return unknownFieldsData.flatMap { Data($0) } ?? Data()
    }

    /**
     Reads and returns the next tag of the message, or nil if there are no further tags. Use
     `nextFieldWireType` after calling this method to query its wire type. This silently skips groups.
     */
    private func nextTag(messageEndOffset: Int) throws -> UInt32? {
        if state != .tag {
            // After reading the previous value the state should have been set to `.tag`
            fatalError("Unexpected call to nextTag. State is \(state).")
        }

        while pos < messageEndOffset && pos < data.count {
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
                let (length, size) = try data.readVarint32(at: pos)
                pos += size
                state = .lengthDelimited(endOffset: pos + Int(length))
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

    private func readFieldKey() throws -> (UInt32, FieldWireType) {
        let (tagAndWireType, size) = try data.readVarint32(at: pos)
        pos += size
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
        case let .lengthDelimited(endOffset):
            // Preallocate space for the unpacked data.
            // It's allowable to have a packed field spread across multiple places
            // in the buffer, so add to the existing capacity.
            //
            // This is a rough estimate. For fixed-size values like `bool`s, `double`s,
            // `float`s, and fixed-size integers this will be accurate.
            // For variable-sized ints it will likely underestimate.
            let packedDataSize = endOffset - pos
            array.reserveCapacity(array.count + packedDataSize / MemoryLayout<T>.size)

            // This is a packed field, so keep decoding until we're out of bytes.
            while pos < endOffset {
                // Reading a scalar will set the state to `.tag` because we assume
                // we're reading a single value most of the time and are then done.
                // Since we're in a repeated field we'll keep reading values though.
                state = .packedValue

                array.append(try decode())
            }
        default:
            // This is a single entry in a regular repeated field
            array.append(try decode())
        }
    }

    private func decode<K, V>(decodeKey: () throws -> K, decodeValue: () throws -> V) throws -> (K, V) {
        var key: K?
        var value: V?

        _ = try forEachTag { tag in
            switch tag {
            case 1: key = try decodeKey()
            case 2: value = try decodeValue()
            default:
                throw ProtoDecoder.Error.unexpectedFieldNumberInMap(tag)
            }
        }

        guard let unwrappedKey = key else {
            throw ProtoDecoder.Error.mapEntryWithoutKey(value: value)
        }
        guard let unwrappedValue = value else {
            throw ProtoDecoder.Error.mapEntryWithoutValue(key: key)
        }
        return (unwrappedKey, unwrappedValue)
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
        let unknownFieldsWriter = ProtoWriter(data: unknownFieldsStack.last! ?? WriteBuffer())
        try block(unknownFieldsWriter)
        unknownFieldsStack[unknownFieldsStack.count - 1] = unknownFieldsWriter.buffer
    }

}
