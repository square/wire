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
import Foundation

public protocol Redactable: CustomStringConvertible {

    associatedtype RedactedKeys: RedactedKey

}

public protocol RedactedKey {

    /**
     Returns a value if a given raw string represents a redacted key,
     and returns `nil` if a given string does not represent a redacted key.
     */
    init?(rawValue: String)

}

extension Redactable {

    /**
     Prints a redacted summary of the object.
     */
    public var description: String {
        let mirror = Mirror(reflecting: self)

        let fields = mirror.children.map {
            (label: String?, value: Any) -> String in
            guard let label = label else {
                return "\(value)"
            }
            var strippedLabel = label
            if (label.hasPrefix("_")) {
                strippedLabel = String(label.dropFirst(1))
            }
            if (strippedLabel.hasSuffix("_")) {
                strippedLabel = String(strippedLabel.dropLast(1))
            }
            if RedactedKeys(rawValue: strippedLabel) != nil {
                // This is a redacted field, but if it's nil then that's ok to print
                if "\(value)" != "nil" {
                    return "\(strippedLabel): <redacted>"
                }
            }
            if value is String {
                return "\(strippedLabel): \"\(value)\""
            }
            return "\(strippedLabel): \(value)"
        }

        if fields.count > 0 {
            var typeName = String(describing: mirror.subjectType)
            if typeName.hasPrefix("_") {
                typeName = String(typeName.dropFirst(1))
            }
            let allFields = fields.joined(separator: ", ")
            return "\(typeName)(\(allFields))"
        } else {
            return "\(self)"
        }
    }

}
