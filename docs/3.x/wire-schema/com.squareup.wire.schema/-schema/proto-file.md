//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Schema](index.md)/[protoFile](proto-file.md)

# protoFile

[common]\
fun [protoFile](proto-file.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ProtoFile](../-proto-file/index.md)?

fun [protoFile](proto-file.md)(path: Path): [ProtoFile](../-proto-file/index.md)?

Returns the proto file at [path](proto-file.md), or null if this schema has no such file.

[common]\
fun [protoFile](proto-file.md)(protoType: [ProtoType](../-proto-type/index.md)): [ProtoFile](../-proto-file/index.md)?

Returns the proto file containing this [protoType](proto-file.md), or null if there isn't such file.
