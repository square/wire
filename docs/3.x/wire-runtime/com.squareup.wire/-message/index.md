//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[Message](index.md)

# Message

[common, js, native]\
abstract class [Message](index.md)&lt;[M](index.md) : [Message](index.md)&lt;[M](index.md), [B](index.md)&gt;, [B](index.md) : [Message.Builder](-builder/index.md)&lt;[M](index.md), [B](index.md)&gt;&gt;

A protocol buffer message.

[jvm]\
abstract class [Message](index.md)&lt;[M](index.md) : [Message](index.md)&lt;[M](index.md), [B](index.md)&gt;, [B](index.md) : [Message.Builder](-builder/index.md)&lt;[M](index.md), [B](index.md)&gt;&gt; : [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)

A protocol buffer message.

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [common, js, jvm, native]<br>[common, js, jvm, native]<br>abstract class [Builder](-builder/index.md)&lt;[M](-builder/index.md) : [Message](index.md)&lt;[M](-builder/index.md), [B](-builder/index.md)&gt;, [B](-builder/index.md) : [Message.Builder](-builder/index.md)&lt;[M](-builder/index.md), [B](-builder/index.md)&gt;&gt;<br>Superclass for protocol buffer message builders. |
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [encode](encode.md) | [common, js, jvm, native]<br>[common, js, jvm, native]<br>fun [encode](encode.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Encode this message as a byte[].<br>[jvm, common, js]<br>[jvm]<br>fun [encode](encode.md)(stream: [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html))<br>[common, js, jvm]<br>fun [encode](encode.md)(sink: BufferedSink)<br>Encode this message and write it to stream. |
| [encodeByteString](encode-byte-string.md) | [common, js, jvm, native]<br>[common, js, jvm, native]<br>fun [encodeByteString](encode-byte-string.md)(): ByteString<br>Encode this message as a ByteString. |
| [newBuilder](new-builder.md) | [common, js, jvm, native]<br>[common, js, jvm, native]<br>abstract fun [newBuilder](new-builder.md)(): [B](index.md)<br>Returns a new builder initialized with the data in this message. |
| [toString](to-string.md) | [jvm]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [withoutUnknownFields](without-unknown-fields.md) | [jvm]<br>fun [withoutUnknownFields](without-unknown-fields.md)(): [M](index.md)<br>Returns this message with any unknown fields removed. |

## Properties

| Name | Summary |
|---|---|
| [adapter](adapter.md) | [common, js, native]<br>val [adapter](adapter.md): [ProtoAdapter](../-proto-adapter/index.md)&lt;[M](index.md)&gt;<br>The [ProtoAdapter](../-proto-adapter/index.md) for encoding and decoding messages of this type.<br>[jvm]<br>@[Transient](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-transient/index.html)<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "adapter")<br>val [adapter](adapter.md): [ProtoAdapter](../-proto-adapter/index.md)&lt;[M](index.md)&gt;<br>The [ProtoAdapter](../-proto-adapter/index.md) for encoding and decoding messages of this type. |
| [unknownFields](unknown-fields.md) | [common, js, native]<br>val [unknownFields](unknown-fields.md): ByteString<br>Returns a byte string containing the proto encoding of this message's unknown fields. Returns an empty byte string if this message has no unknown fields.<br>[jvm]<br>@[Transient](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-transient/index.html)<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "unknownFields")<br>val [unknownFields](unknown-fields.md): ByteString<br>Returns a byte string containing the proto encoding of this message's unknown fields. Returns an empty byte string if this message has no unknown fields. |

## Inheritors

| Name |
|---|
| [AnyMessage](../-any-message/index.md) |
| [AndroidMessage](../-android-message/index.md) |
