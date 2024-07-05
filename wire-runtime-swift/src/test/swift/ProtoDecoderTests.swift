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
import Wire
import XCTest

final class ProtoDecoderTests: XCTestCase {

    func testDecodeEmptyData() throws {
        let decoder = ProtoDecoder()
        let object = try decoder.decode(SimpleOptional2.self, from: Foundation.Data())

        XCTAssertEqual(object, SimpleOptional2())
    }

    func testDecodeEmptyDataTwice() throws {
        let decoder = ProtoDecoder()
        // The empty message case is optimized to reuse objects, so make sure
        // that no unexpected state changes persist from one decode to another.
        let object1 = try decoder.decode(SimpleOptional2.self, from: Foundation.Data())
        let object2 = try decoder.decode(SimpleOptional2.self, from: Foundation.Data())

        XCTAssertEqual(object1, SimpleOptional2())
        XCTAssertEqual(object2, SimpleOptional2())

        /*
         * Packed content for unpacked field:
         *
         * $> echo -n "UgQBAgME" | base64 -d | protoscope
         * 10: {`01020304`}
         */
        var data = Foundation.Data(base64Encoded: "UgQBAgME")!
        let object3 = try decoder.decode(SimpleOptional2.self, from: data)
        XCTAssertEqual(object3, SimpleOptional2 {
            $0.repeated_int32 = [1, 2, 3, 4]
        })

        /*
         * Also the opposite: non-packed content for packed field:
         *
         * $> echo -n "aAFoAmgDaAQ=" | base64 -d | protoscope
         * 13: 1
         * 13: 2
         * 13: 3
         * 13: 4
         */
        data = Foundation.Data(base64Encoded: "aAFoAmgDaAQ=")!
        let object4 = try decoder.decode(SimpleOptional2.self, from: data)
        print(object4.packed_int32)
        XCTAssertEqual(object4, SimpleOptional2 {
            $0.packed_int32 = [1, 2, 3, 4]
        })
    }
}
