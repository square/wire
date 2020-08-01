//
//  Copyright © 2020 Square Inc. All rights reserved.
//

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
            if RedactedKeys(rawValue: label) != nil {
                // This is a redacted field, but if it's nil then that's ok to print
                if "\(value)" != "nil" {
                    return "\(label): <redacted>"
                }
            }
            if value is String {
                return "\(label): \"\(value)\""
            }
            return "\(label): \(value)"
        }

        if fields.count > 0 {
            let allFields = fields.joined(separator: ", ")
            return "\(mirror.subjectType)(\(allFields))"
        } else {
            return "\(self)"
        }
    }

}
