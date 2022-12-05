//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[JavaTarget](index.md)

# JavaTarget

[jvm]\
data class [JavaTarget](index.md)(includes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, excludes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, exclusive: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), outDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), android: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), androidAnnotations: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), compact: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), emitDeclaredOptions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), emitAppliedOptions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), buildersOnly: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) : [Target](../-target/index.md)

Generate .java sources.

## Constructors

| | |
|---|---|
| [JavaTarget](-java-target.md) | [jvm]<br>fun [JavaTarget](-java-target.md)(includes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf("*"), excludes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(), exclusive: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, outDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), android: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, androidAnnotations: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, compact: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, emitDeclaredOptions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, emitAppliedOptions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, buildersOnly: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) |

## Functions

| Name | Summary |
|---|---|
| [copyTarget](copy-target.md) | [jvm]<br>open override fun [copyTarget](copy-target.md)(includes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, excludes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, exclusive: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), outDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Target](../-target/index.md)<br>Returns a new Target object that is a copy of this one, but with the given fields updated. |
| [newHandler](new-handler.md) | [jvm]<br>open override fun [newHandler](new-handler.md)(): SchemaHandler |

## Properties

| Name | Summary |
|---|---|
| [android](android.md) | [jvm]<br>val [android](android.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false<br>True for emitted types to implement android.os.Parcelable. |
| [androidAnnotations](android-annotations.md) | [jvm]<br>val [androidAnnotations](android-annotations.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false<br>True to enable the androidx.annotation.Nullable annotation where applicable. |
| [buildersOnly](builders-only.md) | [jvm]<br>val [buildersOnly](builders-only.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false<br>If true, the constructor of all generated types will be non-public. |
| [compact](compact.md) | [jvm]<br>val [compact](compact.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false<br>True to emit code that uses reflection for reading, writing, and toString methods which are normally implemented with generated code. |
| [emitAppliedOptions](emit-applied-options.md) | [jvm]<br>val [emitAppliedOptions](emit-applied-options.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true<br>True to emit annotations for options applied on messages, fields, etc. |
| [emitDeclaredOptions](emit-declared-options.md) | [jvm]<br>val [emitDeclaredOptions](emit-declared-options.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true<br>True to emit types for options declared on messages, fields, etc. |
| [excludes](excludes.md) | [jvm]<br>open override val [excludes](excludes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Proto types to excluded generated sources for. Types listed here will not be generated for this target. |
| [exclusive](exclusive.md) | [jvm]<br>open override val [exclusive](exclusive.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true<br>True if types emitted for this target should not also be emitted for other targets. Use this to cause multiple outputs to be emitted for the same input type. |
| [includes](includes.md) | [jvm]<br>open override val [includes](includes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Proto types to include generated sources for. Types listed here will be generated for this target and not for subsequent targets in the task. |
| [outDirectory](out-directory.md) | [jvm]<br>open override val [outDirectory](out-directory.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Directory where this target will write its output. |
