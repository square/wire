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

/// Interface that every extensible Protobuf `message` conforms to.
public protocol ProtoExtensible: ProtoMessage {
    var unknownFields: [UInt32: Data] { get }
}

public extension ProtoExtensible {
    func parseUnknownField<T: ProtoDecodable>(
        with protoDecoder: ProtoDecoder = .init(),
        fieldNumber: UInt32,
        type: T.self
      ) -> T? {
        guard let data = unknownFields[fieldNumber] else {
            return nil
        }
        return try protoDecoder.decode(T.self, from: data)
    }

    func setUnknownField<T: ProtoEncodable>(
        with protoEncoder: ProtoEncoder = .init(),
        fieldNumber: UInt32,
        type: T.self,
        newValue: T
      ) {
        unknownFields[fieldNumber] = protoEncoder.encode(newValue)
    }

    // TODO: Add support for ProtoIntCodable, ProtoEnum, possibly others?
}
