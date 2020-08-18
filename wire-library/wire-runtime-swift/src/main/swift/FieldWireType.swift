//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation

/**
 The encoding type which is included as part of the key for each field.
 These values match those specified by the protocol buffer specification.
 https://developers.google.com/protocol-buffers/docs/encoding#structure
 */
public enum FieldWireType: UInt32 {
    case varint = 0
    case fixed64 = 1
    case lengthDelimited = 2
    case startGroup = 3
    case endGroup = 4
    case fixed32 = 5
}
