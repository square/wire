//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Target](index.md)/[copyTarget](copy-target.md)

# copyTarget

[jvm]\
abstract fun [copyTarget](copy-target.md)(includes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = this.includes, excludes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = this.excludes, exclusive: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = this.exclusive, outDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = this.outDirectory): [Target](index.md)

Returns a new Target object that is a copy of this one, but with the given fields updated.
