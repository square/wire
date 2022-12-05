//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoAdapter](index.md)/[encode](encode.md)

# encode

[jvm]\
fun [encode](encode.md)(stream: [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html), value: [E](index.md))

abstract fun [encode](encode.md)(writer: ProtoWriter, value: [E](index.md))

open fun [encode](encode.md)(writer: ReverseProtoWriter, value: [E](index.md))

fun [encode](encode.md)(sink: BufferedSink, value: [E](index.md))

fun [encode](encode.md)(value: [E](index.md)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

[common, js, native]\
[common]\
abstract fun [encode](encode.md)(writer: [ProtoWriter](../-proto-writer/index.md), value: [E](index.md))

[js, native]\
abstract fun [encode](encode.md)(writer: ProtoWriter, value: [E](index.md))

[common]\
open fun [encode](encode.md)(writer: [ReverseProtoWriter](../-reverse-proto-writer/index.md), value: [E](index.md))

[js, native]\
open fun [encode](encode.md)(writer: ReverseProtoWriter, value: [E](index.md))

Write non-null value to writer.

[common, js]\
[common, js]\
fun [encode](encode.md)(sink: BufferedSink, value: [E](index.md))

Encode value and write it to stream.

[common, js, native]\
[common, js, native]\
fun [encode](encode.md)(value: [E](index.md)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

Encode value as a byte[].
