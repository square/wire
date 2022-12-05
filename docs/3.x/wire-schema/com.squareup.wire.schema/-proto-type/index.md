//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[ProtoType](index.md)

# ProtoType

[common]\
class [ProtoType](index.md)

Names a protocol buffer message, enumerated type, service, map, or a scalar. This class models a fully-qualified name using the protocol buffer package.

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [equals](equals.md) | [common]<br>open operator override fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [hashCode](hash-code.md) | [common]<br>open override fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [nestedType](nested-type.md) | [common]<br>fun [nestedType](nested-type.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?): [ProtoType](index.md) |
| [toString](to-string.md) | [common]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [enclosingTypeOrPackage](enclosing-type-or-package.md) | [common]<br>val [enclosingTypeOrPackage](enclosing-type-or-package.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>Returns the enclosing type, or null if this type is not nested in another type. |
| [isMap](is-map.md) | [common]<br>val [isMap](is-map.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isScalar](is-scalar.md) | [common]<br>val [isScalar](is-scalar.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isWrapper](is-wrapper.md) | [common]<br>val [isWrapper](is-wrapper.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>True if this type is defined in google/protobuf/wrappers.proto. |
| [keyType](key-type.md) | [common]<br>val [keyType](key-type.md): [ProtoType](index.md)?<br>The type of the map's keys. Only present when [isMap](is-map.md) is true. |
| [simpleName](simple-name.md) | [common]<br>val [simpleName](simple-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [typeUrl](type-url.md) | [common]<br>val [typeUrl](type-url.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>Returns a string like "type.googleapis.com/packagename.messagename" or null if this type is a scalar or a map. Note that this returns a non-null string for enums because it doesn't know if the named type is a message or an enum. |
| [valueType](value-type.md) | [common]<br>val [valueType](value-type.md): [ProtoType](index.md)?<br>The type of the map's values. Only present when [isMap](is-map.md) is true. |
