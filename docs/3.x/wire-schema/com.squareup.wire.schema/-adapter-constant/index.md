//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[AdapterConstant](index.md)

# AdapterConstant

[jvm]\
data class [AdapterConstant](index.md)(javaClassName: ClassName, kotlinClassName: ClassName, memberName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

A constant field that identifies a ProtoAdapter. This should be a string like like com.squareup.dinosaurs.Dinosaur#ADAPTER with a fully qualified class name, a #, and a field name.

## Constructors

| | |
|---|---|
| [AdapterConstant](-adapter-constant.md) | [jvm]<br>fun [AdapterConstant](-adapter-constant.md)(javaClassName: ClassName, kotlinClassName: ClassName, memberName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [javaClassName](java-class-name.md) | [jvm]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [javaClassName](java-class-name.md): ClassName |
| [kotlinClassName](kotlin-class-name.md) | [jvm]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [kotlinClassName](kotlin-class-name.md): ClassName |
| [memberName](member-name.md) | [jvm]<br>@[JvmField](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/index.html)<br>val [memberName](member-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
