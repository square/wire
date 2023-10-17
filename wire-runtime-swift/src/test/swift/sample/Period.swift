// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.geology.Period in squareup/geology/period.proto
import Foundation
import Wire

public enum Period : Int32, CaseIterable, ProtoEnum, ProtoDefaultedValue {

    /**
     * 145.5 million years ago — 66.0 million years ago.
     */
    case CRETACEOUS = 1
    /**
     * 201.3 million years ago — 145.0 million years ago.
     */
    case JURASSIC = 2
    /**
     * 252.17 million years ago — 201.3 million years ago.
     */
    case TRIASSIC = 3

    public static var defaultedValue: Period {
        Period.CRETACEOUS
    }
    public var description: String {
        switch self {
        case .CRETACEOUS: return "CRETACEOUS"
        case .JURASSIC: return "JURASSIC"
        case .TRIASSIC: return "TRIASSIC"
        }
    }

}

#if swift(>=5.5)
extension Period : Sendable {
}
#endif
