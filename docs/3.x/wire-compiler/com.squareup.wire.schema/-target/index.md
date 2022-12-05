//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Target](index.md)

# Target

[jvm]\
sealed class [Target](index.md) : [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)

## Functions

| Name | Summary |
|---|---|
| [copyTarget](copy-target.md) | [jvm]<br>abstract fun [copyTarget](copy-target.md)(includes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = this.includes, excludes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = this.excludes, exclusive: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = this.exclusive, outDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = this.outDirectory): [Target](index.md)<br>Returns a new Target object that is a copy of this one, but with the given fields updated. |
| [newHandler](new-handler.md) | [jvm]<br>abstract fun [newHandler](new-handler.md)(): SchemaHandler |

## Properties

| Name | Summary |
|---|---|
| [excludes](excludes.md) | [jvm]<br>abstract val [excludes](excludes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Proto types to excluded generated sources for. Types listed here will not be generated for this target. |
| [exclusive](exclusive.md) | [jvm]<br>abstract val [exclusive](exclusive.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>True if types emitted for this target should not also be emitted for other targets. Use this to cause multiple outputs to be emitted for the same input type. |
| [includes](includes.md) | [jvm]<br>abstract val [includes](includes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Proto types to include generated sources for. Types listed here will be generated for this target and not for subsequent targets in the task. |
| [outDirectory](out-directory.md) | [jvm]<br>abstract val [outDirectory](out-directory.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Directory where this target will write its output. |

## Inheritors

| Name |
|---|
| [JavaTarget](../-java-target/index.md) |
| [KotlinTarget](../-kotlin-target/index.md) |
| [SwiftTarget](../-swift-target/index.md) |
| [ProtoTarget](../-proto-target/index.md) |
| [CustomTarget](../-custom-target/index.md) |
