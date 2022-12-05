//[wire-schema](../../../index.md)/[com.squareup.wire](../index.md)/[WireLogger](index.md)/[unusedExcludesInTarget](unused-excludes-in-target.md)

# unusedExcludesInTarget

[common]\
abstract fun [unusedExcludesInTarget](unused-excludes-in-target.md)(unusedExcludes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;)

This is called if some excludes values have not been used by the target they were defined in. Note that excludes should contain package names (suffixed with .*) and type names only. It should not contain member names, nor file paths. Unused excludes can happen if the referenced type or service isn't part of the parsed and pruned schema model, or has already been consumed by another preceding target.
