//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoWriter](index.md)

# ProtoWriter

[common]\
class [ProtoWriter](index.md)(sink: BufferedSink)

Utilities for encoding and writing protocol message fields.

## Constructors

| | |
|---|---|
| [ProtoWriter](-proto-writer.md) | [common]<br>fun [ProtoWriter](-proto-writer.md)(sink: BufferedSink) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [writeBytes](write-bytes.md) | [common]<br>fun [writeBytes](write-bytes.md)(value: ByteString) |
| [writeFixed32](write-fixed32.md) | [common]<br>fun [writeFixed32](write-fixed32.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))<br>Write a little-endian 32-bit integer. |
| [writeFixed64](write-fixed64.md) | [common]<br>fun [writeFixed64](write-fixed64.md)(value: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html))<br>Write a little-endian 64-bit integer. |
| [writeString](write-string.md) | [common]<br>fun [writeString](write-string.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [writeTag](write-tag.md) | [common]<br>fun [writeTag](write-tag.md)(fieldNumber: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), fieldEncoding: [FieldEncoding](../-field-encoding/index.md))<br>Encode and write a tag. |
| [writeVarint32](write-varint32.md) | [common]<br>fun [writeVarint32](write-varint32.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))<br>Encode and write a varint. value is treated as unsigned, so it won't be sign-extended if negative. |
| [writeVarint64](write-varint64.md) | [common]<br>fun [writeVarint64](write-varint64.md)(value: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html))<br>Encode and write a varint. |
