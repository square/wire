//
//  ProtoMap.swift
//
//  Created by Adam Lickel on 1/16/23.
//

import Foundation

@propertyWrapper
public struct ProtoMap<Key : Hashable & LosslessStringConvertible, Value> {
    public var wrappedValue: [Key: Value]

    public init(wrappedValue: [Key: Value]) {
        self.wrappedValue = wrappedValue
    }
}

extension ProtoMap : Encodable where Value : Encodable {
    public func encode(to encoder: Encoder) throws {
        #warning("IMPL")
    }
}

extension ProtoMap : Decodable where Value : Decodable {
    public init(from decoder: Decoder) throws {
        #warning("IMPL")
        wrappedValue = [:]
    }
}

extension ProtoMap : Equatable where Value : Equatable {

}

extension ProtoMap : Hashable where Value : Hashable {

}

extension ProtoMap : EmptyInitializable {
    public init() {
        self.init(wrappedValue: [:])
    }
}

// MARK: - ProtoMapStringEncodedValues

@propertyWrapper
public struct ProtoMapStringEncodedValues<Key : Hashable & LosslessStringConvertible, Value : StringCodable> {
    public var wrappedValue: [Key: Value]

    public init(wrappedValue: [Key: Value]) {
        self.wrappedValue = wrappedValue
    }
}

extension ProtoMapStringEncodedValues : Encodable {
    public func encode(to encoder: Encoder) throws {
        #warning("IMPL")
    }
}

extension ProtoMapStringEncodedValues : Decodable {
    public init(from decoder: Decoder) throws {
        #warning("IMPL")
        wrappedValue = [:]
    }
}

extension ProtoMapStringEncodedValues : Equatable where Value : Equatable {

}

extension ProtoMapStringEncodedValues : Hashable where Value : Hashable {

}

extension ProtoMapStringEncodedValues : EmptyInitializable {
    public init() {
        self.init(wrappedValue: [:])
    }
}
