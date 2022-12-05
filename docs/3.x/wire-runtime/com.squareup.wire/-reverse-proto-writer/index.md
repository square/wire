//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ReverseProtoWriter](index.md)

# ReverseProtoWriter

[common]\
class [ReverseProtoWriter](index.md)

Encodes protocol buffer message fields from back-to-front for efficiency. Callers should write data in the opposite order that the data will be read.

One significant benefit of writing messages in reverse order is that length prefixes can be computed in constant time. Get the length of a message by subtracting the [byteCount](byte-count.md) before writing it from [byteCount](byte-count.md) after writing it.

Utilities for encoding and writing protocol message fields.

## Constructors

| | |
|---|---|
| [ReverseProtoWriter](-reverse-proto-writer.md) | [common]<br>fun [ReverseProtoWriter](-reverse-proto-writer.md)() |

## Functions

| Name | Summary |
|---|---|
| [writeBytes](write-bytes.md) | [common]<br>fun [writeBytes](write-bytes.md)(value: ByteString) |
| [writeFixed32](write-fixed32.md) | [common]<br>fun [writeFixed32](write-fixed32.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))<br>Write a little-endian 32-bit integer. |
| [writeFixed64](write-fixed64.md) | [common]<br>fun [writeFixed64](write-fixed64.md)(value: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html))<br>Write a little-endian 64-bit integer. |
| [writeString](write-string.md) | [common]<br>fun [writeString](write-string.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [writeTag](write-tag.md) | [common]<br>fun [writeTag](write-tag.md)(fieldNumber: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), fieldEncoding: [FieldEncoding](../-field-encoding/index.md))<br>Encode and write a tag. |
| [writeTo](write-to.md) | [common]<br>fun [writeTo](write-to.md)(sink: BufferedSink) |
| [writeVarint32](write-varint32.md) | [common]<br>fun [writeVarint32](write-varint32.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))<br>Encode and write a varint. value is treated as unsigned, so it won't be sign-extended if negative. |
| [writeVarint64](write-varint64.md) | [common]<br>fun [writeVarint64](write-varint64.md)(value: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html))<br>Encode and write a varint. |

## Properties

| Name | Summary |
|---|---|
| [byteCount](byte-count.md) | [common]<br>val [byteCount](byte-count.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>The total number of bytes emitted thus far. |
