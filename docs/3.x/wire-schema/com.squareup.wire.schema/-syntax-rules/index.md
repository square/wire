//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[SyntaxRules](index.md)

# SyntaxRules

[common]\
interface [SyntaxRules](index.md)

A set of rules which defines schema requirements for a specific Syntax.

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [getEncodeMode](get-encode-mode.md) | [common]<br>abstract fun [getEncodeMode](get-encode-mode.md)(protoType: [ProtoType](../-proto-type/index.md), label: [Field.Label](../-field/-label/index.md)?, isPacked: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), isOneOf: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [Field.EncodeMode](../-field/-encode-mode/index.md) |
| [isPackedByDefault](is-packed-by-default.md) | [common]<br>abstract fun [isPackedByDefault](is-packed-by-default.md)(type: [ProtoType](../-proto-type/index.md), label: [Field.Label](../-field/-label/index.md)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [jsonName](json-name.md) | [common]<br>abstract fun [jsonName](json-name.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), declaredJsonName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [validateDefaultValue](validate-default-value.md) | [common]<br>abstract fun [validateDefaultValue](validate-default-value.md)(hasDefaultValue: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), errors: [ErrorCollector](../-error-collector/index.md)) |
| [validateEnumConstants](validate-enum-constants.md) | [common]<br>abstract fun [validateEnumConstants](validate-enum-constants.md)(constants: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EnumConstant](../-enum-constant/index.md)&gt;, errors: [ErrorCollector](../-error-collector/index.md)) |
| [validateExtension](validate-extension.md) | [common]<br>abstract fun [validateExtension](validate-extension.md)(protoType: [ProtoType](../-proto-type/index.md), errors: [ErrorCollector](../-error-collector/index.md)) |
| [validateTypeReference](validate-type-reference.md) | [common]<br>abstract fun [validateTypeReference](validate-type-reference.md)(type: [Type](../-type/index.md)?, errors: [ErrorCollector](../-error-collector/index.md)) |
