//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal](../index.md)/[NameFactory](index.md)

# NameFactory

[jvm]\
interface [NameFactory](index.md)&lt;[T](index.md)&gt;

NameFactory is an abstraction for creating language-specific (Java vs Kotlin) type names.

## Functions

| Name | Summary |
|---|---|
| [nestedName](nested-name.md) | [jvm]<br>abstract fun [nestedName](nested-name.md)(enclosing: [T](index.md), simpleName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [T](index.md) |
| [newName](new-name.md) | [jvm]<br>abstract fun [newName](new-name.md)(packageName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), simpleName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [T](index.md) |
