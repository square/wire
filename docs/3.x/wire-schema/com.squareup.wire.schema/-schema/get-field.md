//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Schema](index.md)/[getField](get-field.md)

# getField

[common]\
fun [getField](get-field.md)(protoMember: [ProtoMember](../-proto-member/index.md)): [Field](../-field/index.md)?

Returns the field for [protoMember](get-field.md), or null if this schema defines no such field.

[common]\
fun [getField](get-field.md)(typeName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), memberName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Field](../-field/index.md)?

Returns the field with the fully qualified [typeName](get-field.md) and [memberName](get-field.md), or null if this schema defines no such field.

[common]\
fun [getField](get-field.md)(protoType: [ProtoType](../-proto-type/index.md), memberName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Field](../-field/index.md)?

Returns the field for [protoType](get-field.md) and [memberName](get-field.md), or null if this schema defines no such field.
