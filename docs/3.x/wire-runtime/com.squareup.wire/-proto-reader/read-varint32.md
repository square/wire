//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoReader](index.md)/[readVarint32](read-varint32.md)

# readVarint32

[common]\
fun [readVarint32](read-varint32.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)

Reads a raw varint from the stream. If larger than 32 bits, discard the upper bits.
