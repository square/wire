//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[ProtoMember](index.md)

# ProtoMember

[common]\
class [ProtoMember](index.md)

Identifies a field, enum or RPC on a declaring type. Members are encoded as strings containing a type name, a hash, and a member name, like squareup.dinosaurs.Dinosaur#length_meters.

A member's name is typically a simple name like "length_meters" or "packed". If the member field is an extension to its type, that name is prefixed with its enclosing package. This yields a member name with two packages, like google.protobuf.FieldOptions#squareup.units.unit.

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [equals](equals.md) | [common]<br>open operator override fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [hashCode](hash-code.md) | [common]<br>open override fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [toString](to-string.md) | [common]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [member](member.md) | [common]<br>val [member](member.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [simpleName](simple-name.md) | [common]<br>val [simpleName](simple-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [type](type.md) | [common]<br>val [type](type.md): [ProtoType](../-proto-type/index.md) |
