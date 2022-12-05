//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Target](index.md)/[excludes](excludes.md)

# excludes

[jvm]\
abstract val [excludes](excludes.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;

Proto types to excluded generated sources for. Types listed here will not be generated for this target.

This list should contain package names (suffixed with .*) and type names only. It should not contain member names.
