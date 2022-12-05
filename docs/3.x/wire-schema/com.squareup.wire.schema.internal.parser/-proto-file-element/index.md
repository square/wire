//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal.parser](../index.md)/[ProtoFileElement](index.md)

# ProtoFileElement

[common]\
data class [ProtoFileElement](index.md)(location: [Location](../../com.squareup.wire.schema/-location/index.md), packageName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, syntax: Syntax?, imports: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, publicImports: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, types: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](../-type-element/index.md)&gt;, services: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ServiceElement](../-service-element/index.md)&gt;, extendDeclarations: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ExtendElement](../-extend-element/index.md)&gt;, options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt;)

A single .proto file.

## Constructors

| | |
|---|---|
| [ProtoFileElement](-proto-file-element.md) | [common]<br>fun [ProtoFileElement](-proto-file-element.md)(location: [Location](../../com.squareup.wire.schema/-location/index.md), packageName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, syntax: Syntax? = null, imports: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = emptyList(), publicImports: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = emptyList(), types: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](../-type-element/index.md)&gt; = emptyList(), services: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ServiceElement](../-service-element/index.md)&gt; = emptyList(), extendDeclarations: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ExtendElement](../-extend-element/index.md)&gt; = emptyList(), options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt; = emptyList()) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [toSchema](to-schema.md) | [common]<br>fun [toSchema](to-schema.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [extendDeclarations](extend-declarations.md) | [common]<br>val [extendDeclarations](extend-declarations.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ExtendElement](../-extend-element/index.md)&gt; |
| [imports](imports.md) | [common]<br>val [imports](imports.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [location](location.md) | [common]<br>val [location](location.md): [Location](../../com.squareup.wire.schema/-location/index.md) |
| [options](options.md) | [common]<br>val [options](options.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../-option-element/index.md)&gt; |
| [packageName](package-name.md) | [common]<br>val [packageName](package-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [publicImports](public-imports.md) | [common]<br>val [publicImports](public-imports.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [services](services.md) | [common]<br>val [services](services.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ServiceElement](../-service-element/index.md)&gt; |
| [syntax](syntax.md) | [common]<br>val [syntax](syntax.md): Syntax? = null |
| [types](types.md) | [common]<br>val [types](types.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](../-type-element/index.md)&gt; |
