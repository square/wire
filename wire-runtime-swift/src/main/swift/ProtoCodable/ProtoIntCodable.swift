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
 Describes an integer type which can be decoded from protocol buffer data.
 These may be encoded using different encodings and the encoding used must
 be specified at the time of decoding.
 */
public protocol ProtoIntDecodable {

    init(from reader: ProtoReader, encoding: ProtoIntEncoding) throws

}

/**
 Describes an integer type which can be encoded in protocol buffer data
 using various encodings.
 */
public protocol ProtoIntEncodable: Comparable {

    func encode(to writer: ProtoWriter, encoding: ProtoIntEncoding) throws

}

// MARK: -

public typealias ProtoIntCodable = ProtoIntDecodable & ProtoIntEncodable

// MARK: -

/**
 Possible ways to encode an integer of any variety (signed, unsigned, 32-, or 64-bit).
 */
public enum ProtoIntEncoding {
    case fixed
    case signed
    case variable
}
