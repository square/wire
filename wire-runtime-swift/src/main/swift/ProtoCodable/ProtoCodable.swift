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

/**
 A protocol which defines a type as being deserializable from protocol buffer data.
 */
public protocol ProtoDecodable {

    static var protoSyntax: ProtoSyntax? { get }

    init(from reader: ProtoReader) throws

}

// MARK: -

/**
 A protocol which defines a type as being serializable into protocol buffer data.
 */
public protocol ProtoEncodable {

    /** The wire type to use in the key for this field */
    static var protoFieldWireType: FieldWireType { get }

    static var protoSyntax: ProtoSyntax? { get }

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
public typealias ProtoCodable = ProtoDecodable & ProtoEncodable

// MARK: -

/**
 A marker protocol indicating that a given struct was generated from a .proto file
 that was using the Proto2 specification
 */
public protocol Proto2Codable: ProtoCodable {}

extension Proto2Codable {

    public static var protoSyntax: ProtoSyntax? { .proto2 }

}

/**
 A marker protocol indicating that a given struct was generated from a .proto file
 that was using the Proto3 specification
 */
public protocol Proto3Codable: ProtoCodable {}

extension Proto3Codable {

    public static var protoSyntax: ProtoSyntax? { .proto3 }

}

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

extension ProtoEnum where Self : RawRepresentable, RawValue == Int32 {

    /**
     A convenience function used with enum fields that throws an error if the field is null
     and its default value can't be used instead.
     */
    public static func defaultIfMissing(_ value: Self?) throws -> Self {
        guard let value = value ?? Self(rawValue: 0) else {
            throw ProtoDecoder.Error.missingEnumDefaultValue(type: Self.self)
        }
        return value
    }

}
