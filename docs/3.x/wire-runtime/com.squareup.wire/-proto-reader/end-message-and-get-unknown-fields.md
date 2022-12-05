//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoReader](index.md)/[endMessageAndGetUnknownFields](end-message-and-get-unknown-fields.md)

# endMessageAndGetUnknownFields

[common]\
fun [endMessageAndGetUnknownFields](end-message-and-get-unknown-fields.md)(token: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)): ByteString

End a length-delimited nested message. Calls to this method must be symmetric with calls to [beginMessage](begin-message.md).

## Parameters

common

| | |
|---|---|
| token | value returned from the corresponding call to [beginMessage](begin-message.md). |
