/// Interface that every Protobuf `message` conforms to.
public protocol ProtoMessage {
    /// - returns: The type URL for this message.
    ///            Example: `type.googleapis.com/packagename.messagename`
    static func protoMessageTypeURL() -> String
}
