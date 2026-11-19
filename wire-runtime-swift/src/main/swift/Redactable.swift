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
     Returns true if a mirror label names a redacted field.

     The generator emits the proto field name in `RedactedKeys`.
     Mirror labels can differ from that name.
     Property wrappers prefix a `_`, and `description` is renamed to `description_`.
     Field names can also start or end with `_` themselves, so this checks every name the label can map to.
     */
    private func isRedacted(label: String) -> Bool {
        var names = [label]
        if label.hasPrefix("_") {
            names.append(String(label.dropFirst(1)))
        }
        for name in names {
            if RedactedKeys(rawValue: name) != nil {
                return true
            }
            if name == "description_" && RedactedKeys(rawValue: "description") != nil {
                return true
            }
        }
        return false
    }

    /**
     Returns a string in quotes, with quotes, backslashes, and control characters escaped.

     This keeps a string value from ending its field or starting a new log line.
     */
    private func quoted(_ string: String) -> String {
        var result = "\""
        for scalar in string.unicodeScalars {
            switch scalar {
            case "\"": result += "\\\""
            case "\\": result += "\\\\"
            default: result += escapedControl(scalar) ?? String(scalar)
            }
        }
        return result + "\""
    }

    /**
     Returns the escaped form of a control or line-breaking character, or nil for any other character.
     */
    private func escapedControl(_ scalar: Unicode.Scalar) -> String? {
        switch scalar {
        case "\n": return "\\n"
        case "\r": return "\\r"
        case "\t": return "\\t"
        case "\0": return "\\0"
        case "\u{0}"..."\u{1F}", "\u{7F}"..."\u{9F}", "\u{2028}", "\u{2029}":
            let hex = String(scalar.value, radix: 16, uppercase: true)
            return "\\u{" + (hex.count < 2 ? "0" : "") + hex + "}"
        default:
            return nil
        }
    }

    /**
     Escapes control and line-breaking characters that remain in a description.

     Values inside wrappers, collections, and nested messages print through Swift's own formatting, which leaves some of them raw.
     This does not touch quotes or backslashes, so it never escapes the same character twice.
     */
    private func escapingControls(_ text: String) -> String {
        var result = ""
        for scalar in text.unicodeScalars {
            if let escaped = escapedControl(scalar) {
                result += escaped
            } else {
                result.unicodeScalars.append(scalar)
            }
        }
        return result
    }

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
            if isRedacted(label: label) {
                // This is a redacted field, but if it's nil then that's ok to print
                if "\(value)" != "nil" {
                    return "\(strippedLabel): <redacted>"
                }
            }
            if let string = value as? String {
                return "\(strippedLabel): \(quoted(string))"
            }
            return "\(strippedLabel): \(value)"
        }

        if fields.count > 0 {
            var typeName = String(describing: mirror.subjectType)
            if typeName.hasPrefix("_") {
                typeName = String(typeName.dropFirst(1))
            }
            let allFields = fields.joined(separator: ", ")
            return escapingControls("\(typeName)(\(allFields))")
        } else {
            return escapingControls("\(self)")
        }
    }

}
