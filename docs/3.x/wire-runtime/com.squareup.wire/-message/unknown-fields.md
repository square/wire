//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[Message](index.md)/[unknownFields](unknown-fields.md)

# unknownFields

[common, js, jvm, native]\
[common, js, native]\
val [unknownFields](unknown-fields.md): ByteString

[jvm]\

@[Transient](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-transient/index.html)

@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "unknownFields")

val [unknownFields](unknown-fields.md): ByteString

Returns a byte string containing the proto encoding of this message's unknown fields. Returns an empty byte string if this message has no unknown fields.
