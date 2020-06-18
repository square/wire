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
    private var unknownFieldsStack: [Data?] = []

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

    // MARK: - Public Methods - Decoding

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

    public func decode<T: ProtoIntDecodable>(_ type: T.Type, encoding: ProtoIntEncoding = .variable) throws -> T {
        return try T(from: self, encoding: encoding)
    }

    /** Decode a field which has a single encoding mechanism, like messages, strings, and bytes. */
    public func decode<T: ProtoDecodable>(_ type: T.Type) throws -> T {
        return try T(from: self)
    }

    /** Decode a repeated field which has a single encoding mechanism, like messages, strings, and bytes. */
    public func decode<T: ProtoDecodable>(into array: inout [T]) throws {
        // These types do not support packing, so no need to test for it.
        try array.append(T(from: self))
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

    // MARK: - Public Methods - Unknown Fields

    /**
     * Read an unknown field and store temporarily. The stored unknown fields
     * will be returned from `decodeMessage`
     */
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

    /**
     * Reads a `bytes` field value from the stream. The length is read from the stream prior to the
     * actual data.
     */
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

        let result = try data.readFixed32(at: Int(pos))
        pos += 4
        state = .tag

        return result
    }

    /** Reads a 64-bit little-endian integer from the stream.  */
    func readFixed64() throws -> UInt64 {
        precondition(state == .fixed64 || state == .packedValue)

        let result = try data.readFixed64(at: Int(pos))
        pos += 8
        state = .tag

        return result
    }

    /**
     * Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits.
     */
    func readVarint32() throws -> UInt32 {
        precondition(state == .varint || state == .packedValue)

        let (result, size) = try data.readVarint32(at: Int(pos))
        pos += size
        state = .tag

        return result
    }

    /** Reads a raw varint up to 64 bits in length from the stream.  */
    func readVarint64() throws -> UInt64 {
        precondition(state == .varint || state == .packedValue)

        let (result, size) = try data.readVarint64(at: Int(pos))
        pos += size
        state = .tag

        return result
    }

    // MARK: - Private Methods - Message Decoding

    /**
     * Begin a nested message. A call to this method will restrict the reader so that [nextTag]
     * returns nil when the message is complete. An accompanying call to [endMessage] must then occur
     * with the opaque token returned from this method.
     *
     * - parameter decode: A block which is called to actually decode the message. The parameter
     *                     to the block is the end offset of the message.
     * - returns: Returns all unknown fields in the message, encoded sequentially.
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

        return unknownFieldsData ?? Data()
    }

    /**
     * Reads and returns the next tag of the message, or nil if there are no further tags. Use
     * [peekFieldEncoding] after calling this method to query its encoding. This silently skips
     * groups.
     */
    private func nextTag(messageEndOffset: Int) throws -> UInt32? {
        if state != .tag {
            // After reading the previous value the state should have been set to `.tag`
            fatalError("Unexpected call to nextTag. State is \(state).")
        }

        while pos < messageEndOffset && pos < data.count {
            let (tag, wireType) = try readFieldKey()
            switch wireType {
            case .startGroup:
                fatalError("Unimplemented")

            case .endGroup:
                fatalError("Unimplemented")

            case .lengthDelimited:
                nextFieldWireType = .lengthDelimited
                let (length, size) = try data.readVarint32(at: pos)
                pos += size
                state = .lengthDelimited(endOffset: pos + Int(length))
                return tag

            case .fixed32:
                nextFieldWireType = .fixed32
                state = .fixed32
                return tag

            case .fixed64:
                nextFieldWireType = .fixed64
                state = .fixed64
                return tag

            case .varint:
                nextFieldWireType = .varint
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

    // MARK: - Private Methods - Repeated Field Decoding

    private func decode<T>(into array: inout [T], decode: () throws -> T) throws {
        switch state {
        case let .lengthDelimited(endOffset):
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
        let unknownFieldsWriter = ProtoWriter(data: unknownFieldsStack.last! ?? Data())
        try block(unknownFieldsWriter)
        unknownFieldsStack[unknownFieldsStack.count - 1] = unknownFieldsWriter.data
    }

}
