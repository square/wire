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

// Key must be encoded as string
// Value should be one of:
//   * Codable
//   * Message
//   * StringCodable
