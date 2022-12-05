//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal](../index.md)/[TypeConfigElement](index.md)

# TypeConfigElement

[common]\
data class [TypeConfigElement](index.md)(location: [Location](../../com.squareup.wire.schema/-location/index.md), type: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), with: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../../com.squareup.wire.schema.internal.parser/-option-element/index.md)&gt;, target: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, adapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?)

Configures how Wire will generate code for a specific type. This configuration belongs in a build.wire file that is in the same directory as the configured type.

## Constructors

| | |
|---|---|
| [TypeConfigElement](-type-config-element.md) | [common]<br>fun [TypeConfigElement](-type-config-element.md)(location: [Location](../../com.squareup.wire.schema/-location/index.md), type: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "", with: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../../com.squareup.wire.schema.internal.parser/-option-element/index.md)&gt; = emptyList(), target: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, adapter: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) |

## Functions

| Name | Summary |
|---|---|
| [toSchema](to-schema.md) | [common]<br>fun [toSchema](to-schema.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [adapter](adapter.md) | [common]<br>val [adapter](adapter.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [documentation](documentation.md) | [common]<br>val [documentation](documentation.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [location](location.md) | [common]<br>val [location](location.md): [Location](../../com.squareup.wire.schema/-location/index.md) |
| [target](target.md) | [common]<br>val [target](target.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [type](type.md) | [common]<br>val [type](type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [with](with.md) | [common]<br>val [with](with.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../../com.squareup.wire.schema.internal.parser/-option-element/index.md)&gt; |
