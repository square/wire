/*
 * Copyright (C) 2022 Square, Inc.
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
import Wire
import XCTest

final class AnyMessageTests: XCTestCase {
    func testPackingAny() throws {
        let data = Data(json_data: "")
        let person = Person(name: "foo bar", id: 12345, data: data)
        let any = try AnyMessage.pack(person)
        XCTAssertEqual(any.typeURL, Person.protoMessageTypeURL())
        XCTAssertEqual(any.value, try ProtoEncoder().encode(person))
    }

    func testUnpackingAnyToCorrectType() throws {
        let data = Data(json_data: "")
        let person = Person(name: "foo bar", id: 12345, data: data)
        let any = try AnyMessage.pack(person)
        XCTAssertEqual(person, try any.unpack(Person.self))
    }

    func testUnpackingAnyToIncorrectTypeThrows() throws {
        let data = Data(json_data: "")
        let person = Person(name: "foo bar", id: 12345, data: data)
        let any = try AnyMessage.pack(person)
        XCTAssertThrowsError(try any.unpack(Parent.self)) { error in
            XCTAssertEqual(.typeURLMismatch, error as? AnyMessage.DecodingError)
        }
    }

    func testSerializingAndDeserializingAny() throws {
        let data = Data(json_data: "")
        let person = Person(name: "foo bar", id: 12345, data: data)
        let any = try AnyMessage.pack(person)
        let anyData = try ProtoEncoder().encode(any)
        XCTAssertEqual(any, try ProtoDecoder().decode(AnyMessage.self, from: anyData))
    }
}
