//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[WireEnumConstant](index.md)

# WireEnumConstant

[common]\
@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.FIELD](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-f-i-e-l-d/index.html)])

annotation class [WireEnumConstant](index.md)(declaredName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Annotates generated [WireEnum](../-wire-enum/index.md) fields with metadata for serialization and deserialization.

## Constructors

| | |
|---|---|
| [WireEnumConstant](-wire-enum-constant.md) | [common]<br>fun [WireEnumConstant](-wire-enum-constant.md)(declaredName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "") |

## Properties

| Name | Summary |
|---|---|
| [declaredName](declared-name.md) | [common]<br>val [declaredName](declared-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Name of this constant as declared in the proto schema. This value is set to a non-empty string only when the declared name differs from the generated one; for instance, a proto constant named final generated in Java will be renamed to final_. |
