//[wire-schema](../../../../index.md)/[com.squareup.wire.schema](../../index.md)/[Type](../index.md)/[Companion](index.md)

# Companion

[common]\
object [Companion](index.md)

## Functions

| Name | Summary |
|---|---|
| [fromElements](from-elements.md) | [common]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [fromElements](from-elements.md)(packageName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, elements: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](../../../com.squareup.wire.schema.internal.parser/-type-element/index.md)&gt;, syntax: Syntax): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../index.md)&gt; |
| [get](get.md) | [common]<br>fun [get](get.md)(namespaces: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, protoType: [ProtoType](../../-proto-type/index.md), type: [TypeElement](../../../com.squareup.wire.schema.internal.parser/-type-element/index.md), syntax: Syntax): [Type](../index.md) |
| [toElements](to-elements.md) | [common]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [toElements](to-elements.md)(types: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../index.md)&gt;): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](../../../com.squareup.wire.schema.internal.parser/-type-element/index.md)&gt; |
