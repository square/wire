// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.kotlin.unknownfields.EnumVersionOne in unknown_fields.proto
import Foundation
import Wire

public enum EnumVersionOne : UInt32, CaseIterable, ProtoEnum {

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

#if swift(>=5.5)
extension EnumVersionOne : Sendable {
}
#endif
