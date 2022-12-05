//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[ProtoTarget](index.md)

# ProtoTarget

[jvm]\
data class [ProtoTarget](index.md)(outDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [Target](../-target/index.md)

## Functions

| Name | Summary |
|---|---|
| [copyTarget](copy-target.md) | [jvm]<br>open override fun [copyTarget](copy-target.md)(includes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, excludes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, exclusive: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), outDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Target](../-target/index.md)<br>Returns a new Target object that is a copy of this one, but with the given fields updated. |
| [newHandler](new-handler.md) | [jvm]<br>open override fun [newHandler](new-handler.md)(): SchemaHandler |

## Properties

| Name | Summary |
|---|---|
| [excludes](excludes.md) | [jvm]<br>open override val [excludes](excludes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Proto types to excluded generated sources for. Types listed here will not be generated for this target. |
| [exclusive](exclusive.md) | [jvm]<br>open override val [exclusive](exclusive.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false<br>True if types emitted for this target should not also be emitted for other targets. Use this to cause multiple outputs to be emitted for the same input type. |
| [includes](includes.md) | [jvm]<br>open override val [includes](includes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Proto types to include generated sources for. Types listed here will be generated for this target and not for subsequent targets in the task. |
| [outDirectory](out-directory.md) | [jvm]<br>open override val [outDirectory](out-directory.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Directory where this target will write its output. |
