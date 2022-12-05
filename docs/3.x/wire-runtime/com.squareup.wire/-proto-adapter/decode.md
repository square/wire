//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoAdapter](index.md)/[decode](decode.md)

# decode

[jvm]\
fun [decode](decode.md)(stream: [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)): [E](index.md)

abstract fun [decode](decode.md)(reader: ProtoReader): [E](index.md)

fun [decode](decode.md)(bytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [E](index.md)

fun [decode](decode.md)(bytes: ByteString): [E](index.md)

fun [decode](decode.md)(source: BufferedSource): [E](index.md)

[common, js, native]\
[common]\
abstract fun [decode](decode.md)(reader: [ProtoReader](../-proto-reader/index.md)): [E](index.md)

[js, native]\
abstract fun [decode](decode.md)(reader: ProtoReader): [E](index.md)

Read a non-null value from reader.

[common, js, native]\
[common, js, native]\
fun [decode](decode.md)(bytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [E](index.md)

[common, js]\
fun [decode](decode.md)(bytes: ByteString): [E](index.md)

Read an encoded message from bytes.

[common, js]\
[common, js]\
fun [decode](decode.md)(source: BufferedSource): [E](index.md)

Read an encoded message from source.
