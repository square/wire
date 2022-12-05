//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Schema](index.md)/[getType](get-type.md)

# getType

[common]\
fun [getType](get-type.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Type](../-type/index.md)?

Returns the type with the fully qualified name [name](get-type.md), or null if this schema defines no such type.

[common]\
fun [getType](get-type.md)(protoType: [ProtoType](../-proto-type/index.md)): [Type](../-type/index.md)?

Returns the type for [protoType](get-type.md), or null if this schema defines no such type.
