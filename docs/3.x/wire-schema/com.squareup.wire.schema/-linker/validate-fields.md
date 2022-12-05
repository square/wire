//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Linker](index.md)/[validateFields](validate-fields.md)

# validateFields

[common]\
fun [validateFields](validate-fields.md)(fields: [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[Field](../-field/index.md)&gt;, reserveds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Reserved](../-reserved/index.md)&gt;, syntaxRules: [SyntaxRules](../-syntax-rules/index.md))

Validate that the tags of [fields](validate-fields.md) are unique and in range, that proto3 message cannot reference proto2 enums.
