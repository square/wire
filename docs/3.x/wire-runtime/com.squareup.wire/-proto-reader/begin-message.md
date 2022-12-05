//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoReader](index.md)/[beginMessage](begin-message.md)

# beginMessage

[common]\
fun [beginMessage](begin-message.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)

Begin a nested message. A call to this method will restrict the reader so that [nextTag](next-tag.md) returns -1 when the message is complete. An accompanying call to endMessage must then occur with the opaque token returned from this method.
