//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Wire
import XCTest

final class ProtoDecoderTests: XCTestCase {

    func testDecodeEmptyData() throws {
        let decoder = ProtoDecoder()
        let object = try decoder.decode(SimpleOptional2.self, from: Data())

        XCTAssertEqual(object, SimpleOptional2())
    }

}
