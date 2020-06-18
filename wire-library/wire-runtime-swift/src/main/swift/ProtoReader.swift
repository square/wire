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

        /// Currently reading a packed tag
        case packedTag

        /// Whether or not this is the length-delimited state.
        /// This is convenient for comparison since that state has an associated value.
        var isLengthDelimited: Bool {
            if case .lengthDelimited(_) = self {
                return true
            } else {
                return false
            }
        }
    }

    // MARK: - Private Properties

    private let data: Data

    /** The current position in the input source, starting at 0 and increasing monotonically. */
    private var pos: Int = 0

    /** The encoding of the next value to be read. */
    private var nextFieldWireType: FieldWireType? = nil

    /** How to interpret the next read call. */
    private var state: State

    // MARK: - Initialization

    init(data: Data) {
        self.data = data
        self.state = .lengthDelimited(endOffset: data.count)
    }

    // MARK: - Public Methods - Iterating Tags

    /** Reads each tag within a message and handles it. */
    public func forEachTag(_ block: (UInt32) throws -> Void) throws {
        try decodeMessage { endOffset in
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

    public func decode<T: ProtoDecodable>(_ type: T.Type) throws -> T {
        return try T(from: self)
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
        precondition(state == .fixed32 || state.isLengthDelimited)

        let result = try data.readFixed32(at: Int(pos))
        pos += 4
        state = .tag

        return result
    }

    /** Reads a 64-bit little-endian integer from the stream.  */
    func readFixed64() throws -> UInt64 {
        precondition(state == .fixed64 || state.isLengthDelimited)

        let result = try data.readFixed64(at: Int(pos))
        pos += 8
        state = .tag

        return result
    }

    /**
     * Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits.
     */
    func readVarint32() throws -> UInt32 {
        precondition(state == .varint || state.isLengthDelimited)

        let (result, size) = try data.readVarint32(at: Int(pos))
        pos += size
        state = .tag

        return result
    }

    /** Reads a raw varint up to 64 bits in length from the stream.  */
    func readVarint64() throws -> UInt64 {
        precondition(state == .varint || state.isLengthDelimited)

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
     */
    private func decodeMessage(_ decode: (_ endOffset: Int) throws -> Void) throws {
        guard case let .lengthDelimited(endOffset) = state else {
            fatalError("Unexpected call to decodeMessage()")
        }

        state = .tag

        try decode(endOffset)

        if pos != endOffset {
            throw ProtoDecoder.Error.invalidStructure(
                message: "Expected to end message at \(endOffset), but was at \(pos)"
            )
        }
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

}
