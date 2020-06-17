//
//  Copyright © 2020 Square Inc. All rights reserved.
//

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
public protocol ProtoIntEncodable {

    func encode(to writer: ProtoWriter, encoding: ProtoIntEncoding) throws

}

// MARK: -

public protocol ProtoIntCodable: ProtoIntDecodable & ProtoIntEncodable {}

// MARK: -

/**
 Possible ways to encode an integer of any variety (signed, unsigned, 32-, or 64-bit).
 */
public enum ProtoIntEncoding {
    case fixed
    case signed
    case variable
}
