/*
 * Copyright (C) 2024 Square, Inc.
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

/// Protocol all messages conform to if they are extensible.
public protocol ProtoExtensible {
    var unknownFields: UnknownFields { get set }
}

public extension ProtoExtensible {
    func parseUnknownField<T: ProtoDecodable>(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        type: T.Type
    ) -> T? {
        guard let data = unknownFields[fieldNumber] else {
            return nil
        }
        // We presumably need to parse out the fields first
        // Right now it's effectively a Tuple(fieldNumber, data)
        return try? protoDecoder.decode(T.self, from: data)
    }

    mutating func setUnknownField<T: ProtoEncodable>(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        newValue: T?
    ) {
        // We need to be encoding the field number here
        unknownFields[fieldNumber] = newValue.flatMap { try? protoEncoder.encode($0) }
    }

    // TODO: Add support for ProtoIntCodable, ProtoEnum, Array, Dictionary, possibly others?
}
