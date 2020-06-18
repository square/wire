//
//  Copyright © 2020 Square Inc. All rights reserved.
//

// MARK: -

/**
 A protocol which defines a type as being deserializable from protocol buffer data.
 */
public protocol ProtoDecodable {

    init(from reader: ProtoReader) throws

}

// MARK: -

/**
 A protocol which defines a type as being serializable into protocol buffer data.
 */
public protocol ProtoEncodable {

    /** The wire type to use in the key for this field */
    static var protoFieldWireType: FieldWireType { get }

    func encode(to writer: ProtoWriter) throws

}

public extension ProtoEncodable {

    /**
     The vast majority of fields, including all messages, use a
     length-delimited wire type, so make it the default.
     */
    static var protoFieldWireType: FieldWireType { .lengthDelimited }

}

// MARK: -

/**
 A convenience protocol which defines a type as being both encodable and decodable as protocol buffer data.
 */
public protocol ProtoCodable: ProtoDecodable & ProtoEncodable {}

// MARK: -

/**
 A marker protocol indicating that a given struct was generated from a .proto file
 that was using the Proto2 specification
 */
public protocol Proto2Codable: ProtoCodable {}

/**
 A marker protocol indicating that a given struct was generated from a .proto file
 that was using the Proto3 specification
 */
public protocol Proto3Codable: ProtoCodable {}

// MARK: -

extension ProtoDecodable {

    /** 
     A convenience function used with required fields that throws an error if the field is null,
     or unwraps the nullable value if not.
     */
    public static func checkIfMissing<T>(_ value: T?, _ fieldName: String) throws -> T {
        guard let value = value else {
            throw ProtoDecoder.Error.missingRequiredField(
                typeName: String(describing: self),
                fieldName: fieldName
            )
        }
        return value
    }

}
