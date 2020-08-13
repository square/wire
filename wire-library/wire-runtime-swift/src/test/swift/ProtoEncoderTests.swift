//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Wire
import XCTest

final class ProtoEncoderTests: XCTestCase {

    func testEncodeEmptyMessage() throws {
        let object = SimpleOptional2()
        let encoder = ProtoEncoder()
        let data = try encoder.encode(object)

        XCTAssertEqual(data, Data())
    }

}
