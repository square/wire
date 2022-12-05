//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[WireRun](index.md)/[modules](modules.md)

# modules

[jvm]\
val [modules](modules.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [WireRun.Module](-module/index.md)&gt;

A map from module dir to module info which dictates how the loaded types are partitioned and generated.

When empty everything is generated in the root output directory. If desired, multiple modules can be specified along with dependencies between them. Types which appear in dependencies will not be re-generated.
