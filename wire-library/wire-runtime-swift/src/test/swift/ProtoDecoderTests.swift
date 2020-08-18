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

    func testDecodeEmptyDataTwice() throws {
        let decoder = ProtoDecoder()
        // The empty message case is optimized to reuse objects, so make sure
        // that no unexpected state changes persist from one decode to another.
        let object1 = try decoder.decode(SimpleOptional2.self, from: Data())
        let object2 = try decoder.decode(SimpleOptional2.self, from: Data())

        XCTAssertEqual(object1, SimpleOptional2())
        XCTAssertEqual(object2, SimpleOptional2())
    }

}
