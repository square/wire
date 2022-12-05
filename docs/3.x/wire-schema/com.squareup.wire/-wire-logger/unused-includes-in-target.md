//[wire-schema](../../../index.md)/[com.squareup.wire](../index.md)/[WireLogger](index.md)/[unusedIncludesInTarget](unused-includes-in-target.md)

# unusedIncludesInTarget

[common]\
abstract fun [unusedIncludesInTarget](unused-includes-in-target.md)(unusedIncludes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;)

This is called if some includes values have not been used by the target they were defined in. Note that includes should contain package names (suffixed with .*) and type names only. It should not contain member names, nor file paths. Unused includes can happen if the referenced type or service isn't part of the parsed and pruned schema model, or has already been consumed by another preceding target.
