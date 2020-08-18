//
//  Copyright © 2020 Square Inc. All rights reserved.
//

import Foundation
import XCTest
@testable import Wire

final class JsonStringTests: XCTestCase {
    struct SupportedTypes : Codable, Equatable {
        @JSONString
        var a: Int64
        @JSONString
        var b: UInt64
        @JSONString
        var c: [Int64]
        @JSONString
        var d: [UInt64]
    }

    func testSupportedTypes() throws {
        let expectedStruct = SupportedTypes(
            a: -12,
            b: 13,
            c: [-14],
            d: [15]
        )
        let expectedJson = """
        {\
        "a":"-12",\
        "b":"13",\
        "c":["-14"],\
        "d":["15"]\
        }
        """

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys // For deterministic output.

        let jsonData = try! encoder.encode(expectedStruct)
        let actualJson = String(data: jsonData, encoding: .utf8)!
        XCTAssertEqual(expectedJson, actualJson)

        let actualStruct = try! JSONDecoder().decode(SupportedTypes.self, from: jsonData)
        XCTAssertEqual(expectedStruct, actualStruct)
    }
}
