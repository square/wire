// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.unknownfields.EnumVersionOne in unknown_fields.proto
import Wire

@objc
public enum EnumVersionOne : Int32, CaseIterable, Proto2Enum {

    case SHREK_V1 = 1
    case DONKEY_V1 = 2
    case FIONA_V1 = 3

    public var description: String {
        switch self {
        case .SHREK_V1: return "SHREK_V1"
        case .DONKEY_V1: return "DONKEY_V1"
        case .FIONA_V1: return "FIONA_V1"
        }
    }

}

extension EnumVersionOne : Sendable {
}
