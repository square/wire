//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[AndroidMessage](index.md)

# AndroidMessage

[jvm]\
abstract class [AndroidMessage](index.md)&lt;[M](index.md) : [Message](../-message/index.md)&lt;[M](index.md), [B](index.md)&gt;, [B](index.md) : [Message.Builder](../-message/-builder/index.md)&lt;[M](index.md), [B](index.md)&gt;&gt; : [Message](../-message/index.md)&lt;[M](index.md), [B](index.md)&gt; , Parcelable

An Android-specific [Message](../-message/index.md) which adds support for Parcelable.

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [describeContents](describe-contents.md) | [jvm]<br>open override fun [describeContents](describe-contents.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [encode](../-message/encode.md) | [jvm]<br>fun [encode](../-message/encode.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Encode this message as a byte[].<br>[jvm]<br>fun [encode](../-message/encode.md)(stream: [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html))<br>fun [encode](../-message/encode.md)(sink: BufferedSink)<br>Encode this message and write it to stream. |
| [encodeByteString](../-message/encode-byte-string.md) | [jvm]<br>fun [encodeByteString](../-message/encode-byte-string.md)(): ByteString<br>Encode this message as a ByteString. |
| [newBuilder](../-message/new-builder.md) | [jvm]<br>abstract fun [newBuilder](../-message/new-builder.md)(): [B](index.md)<br>Returns a new builder initialized with the data in this message. |
| [toString](../-message/to-string.md) | [jvm]<br>open override fun [toString](../-message/to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [withoutUnknownFields](../-message/without-unknown-fields.md) | [jvm]<br>fun [withoutUnknownFields](../-message/without-unknown-fields.md)(): [M](index.md)<br>Returns this message with any unknown fields removed. |
| [writeToParcel](write-to-parcel.md) | [jvm]<br>open override fun [writeToParcel](write-to-parcel.md)(dest: Parcel, flags: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [adapter](../-message/adapter.md) | [jvm]<br>@[Transient](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-transient/index.html)<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "adapter")<br>val [adapter](../-message/adapter.md): [ProtoAdapter](../-proto-adapter/index.md)&lt;[M](index.md)&gt;<br>The [ProtoAdapter](../-proto-adapter/index.md) for encoding and decoding messages of this type. |
| [unknownFields](../-message/unknown-fields.md) | [jvm]<br>@[Transient](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-transient/index.html)<br>@get:[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "unknownFields")<br>val [unknownFields](../-message/unknown-fields.md): ByteString<br>Returns a byte string containing the proto encoding of this message's unknown fields. Returns an empty byte string if this message has no unknown fields. |
