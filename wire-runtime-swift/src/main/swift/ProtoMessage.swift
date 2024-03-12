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

/// This Data value should be a valid proto data blob with the tagged field number.
public typealias UnknownFields = [UInt32: Data]

/// Interface that every Protobuf `message` conforms to.
public protocol ProtoMessage {
    /// - returns: The type URL for this message.
    ///            Example: `type.googleapis.com/packagename.messagename`
    static func protoMessageTypeURL() -> String
}
