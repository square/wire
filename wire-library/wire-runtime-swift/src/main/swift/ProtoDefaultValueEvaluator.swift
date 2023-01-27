/*
 * Copyright 2023 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation

/// The default value for a file in the Proto syntax
public protocol ProtoDefaultValueEvaluator {
    var isDefaultProtoValue: Bool { get }
}

extension Numeric where Self : ProtoDefaultValueEvaluator {
    public var isDefaultProtoValue: Bool {
        self == .zero
    }
}

extension Int32 : ProtoDefaultValueEvaluator {}
extension UInt32 : ProtoDefaultValueEvaluator {}
extension Int64 : ProtoDefaultValueEvaluator {}
extension UInt64 : ProtoDefaultValueEvaluator {}
extension Float : ProtoDefaultValueEvaluator {}
extension Double : ProtoDefaultValueEvaluator {}

extension Bool : ProtoDefaultValueEvaluator {
    public var isDefaultProtoValue: Bool {
        !self
    }
}

extension Collection where Self : ProtoDefaultValueEvaluator {
    public var isDefaultProtoValue: Bool {
        isEmpty
    }
}

// In theory, these should all have conditional conformance...
// This has not been done for now since adding ProtoDefaultValueEvaluator to messages is an expensive recursive problem

extension Array : ProtoDefaultValueEvaluator {}
extension Dictionary : ProtoDefaultValueEvaluator {}

extension Optional : ProtoDefaultValueEvaluator {
    public var isDefaultProtoValue: Bool {
        // A proto3 field that is defined with the optional keyword supports field presence.
        // Fields that have a value set and that support field presence always include the field value in the JSON-encoded output, even if it is the default value.
        self == nil
    }
}


