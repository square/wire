//
//  Copyright © 2020 Square Inc. All rights reserved.
//

// MARK: -

public protocol ProtoDecodable {

}

// MARK: -

public protocol ProtoEncodable {

}

// MARK: -

public protocol ProtoCodable: ProtoDecodable & ProtoEncodable {}

// MARK: -

extension ProtoDecodable {

    /** A convenience function used with required fields that throws an error if the field is null, or unwraps the nullable value if not. */
    public static func checkIfMissing<T>(_ value: T?, _ fieldName: String) throws -> T {
        guard let value = value else {
            throw ProtoDecoder.Error.missingRequiredField(
                typeName: String(describing: self),
                fieldName: fieldName
            )
        }
        return value
    }

}
