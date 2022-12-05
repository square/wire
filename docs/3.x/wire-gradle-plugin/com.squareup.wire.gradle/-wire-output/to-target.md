//[wire-gradle-plugin](../../../index.md)/[com.squareup.wire.gradle](../index.md)/[WireOutput](index.md)/[toTarget](to-target.md)

# toTarget

[jvm]\
abstract fun [toTarget](to-target.md)(outputDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): Target

Transforms this [WireOutput](index.md) into a Target for which Wire will generate code. The Target should use [outputDirectory](to-target.md) instead of [WireOutput.out](--out--.md) in all cases for its output directory.
