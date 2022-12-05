//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoReader](index.md)

# ProtoReader

[common]\
class [ProtoReader](index.md)(source: BufferedSource)

Reads and decodes protocol message fields.

## Constructors

| | |
|---|---|
| [ProtoReader](-proto-reader.md) | [common]<br>fun [ProtoReader](-proto-reader.md)(source: BufferedSource) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [addUnknownField](add-unknown-field.md) | [common]<br>fun [addUnknownField](add-unknown-field.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), fieldEncoding: [FieldEncoding](../-field-encoding/index.md), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?)<br>Store an already read field temporarily. Once the entire message is read, call [endMessageAndGetUnknownFields](end-message-and-get-unknown-fields.md) to retrieve unknown fields. |
| [beginMessage](begin-message.md) | [common]<br>fun [beginMessage](begin-message.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)<br>Begin a nested message. A call to this method will restrict the reader so that [nextTag](next-tag.md) returns -1 when the message is complete. An accompanying call to endMessage must then occur with the opaque token returned from this method. |
| [endMessageAndGetUnknownFields](end-message-and-get-unknown-fields.md) | [common]<br>fun [endMessageAndGetUnknownFields](end-message-and-get-unknown-fields.md)(token: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)): ByteString<br>End a length-delimited nested message. Calls to this method must be symmetric with calls to [beginMessage](begin-message.md). |
| [forEachTag](for-each-tag.md) | [common]<br>@[JvmName](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-name/index.html)(name = "-forEachTag")<br>inline fun [forEachTag](for-each-tag.md)(tagHandler: ([Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)): ByteString<br>Reads each tag, handles it, and returns a byte string with the unknown fields. |
| [nextTag](next-tag.md) | [common]<br>fun [nextTag](next-tag.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Reads and returns the next tag of the message, or -1 if there are no further tags. Use [peekFieldEncoding](peek-field-encoding.md) after calling this method to query its encoding. This silently skips groups. |
| [peekFieldEncoding](peek-field-encoding.md) | [common]<br>fun [peekFieldEncoding](peek-field-encoding.md)(): [FieldEncoding](../-field-encoding/index.md)?<br>Returns the encoding of the next field value. [nextTag](next-tag.md) must be called before this method. |
| [readBytes](read-bytes.md) | [common]<br>fun [readBytes](read-bytes.md)(): ByteString<br>Reads a bytes field value from the stream. The length is read from the stream prior to the actual data. |
| [readFixed32](read-fixed32.md) | [common]<br>fun [readFixed32](read-fixed32.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Reads a 32-bit little-endian integer from the stream. |
| [readFixed64](read-fixed64.md) | [common]<br>fun [readFixed64](read-fixed64.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)<br>Reads a 64-bit little-endian integer from the stream. |
| [readString](read-string.md) | [common]<br>fun [readString](read-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Reads a string field value from the stream. |
| [readUnknownField](read-unknown-field.md) | [common]<br>fun [readUnknownField](read-unknown-field.md)(tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))<br>Read an unknown field and store temporarily. Once the entire message is read, call [endMessageAndGetUnknownFields](end-message-and-get-unknown-fields.md) to retrieve unknown fields. |
| [readVarint32](read-varint32.md) | [common]<br>fun [readVarint32](read-varint32.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits. |
| [readVarint64](read-varint64.md) | [common]<br>fun [readVarint64](read-varint64.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)<br>Reads a raw varint up to 64 bits in length from the stream. |
| [skip](skip.md) | [common]<br>fun [skip](skip.md)()<br>Skips the current field's value. This is only safe to call immediately following a call to [nextTag](next-tag.md). |
