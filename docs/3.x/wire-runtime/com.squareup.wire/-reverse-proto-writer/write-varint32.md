//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ReverseProtoWriter](index.md)/[writeVarint32](write-varint32.md)

# writeVarint32

[common]\
fun [writeVarint32](write-varint32.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html))

Encode and write a varint. value is treated as unsigned, so it won't be sign-extended if negative.
