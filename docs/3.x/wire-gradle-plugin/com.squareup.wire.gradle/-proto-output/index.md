//[wire-gradle-plugin](../../../index.md)/[com.squareup.wire.gradle](../index.md)/[ProtoOutput](index.md)

# ProtoOutput

[jvm]\
open class [ProtoOutput](index.md)@Injectconstructor : [WireOutput](../-wire-output/index.md)

## Functions

| Name | Summary |
|---|---|
| [toTarget](to-target.md) | [jvm]<br>open override fun [toTarget](to-target.md)(outputDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): ProtoTarget<br>Transforms this [WireOutput](../-wire-output/index.md) into a Target for which Wire will generate code. The Target should use [outputDirectory](to-target.md) instead of [WireOutput.out](../-wire-output/--out--.md) in all cases for its output directory. |

## Properties

| Name | Summary |
|---|---|
| [out](../-wire-output/--out--.md) | [jvm]<br>var [out](../-wire-output/--out--.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null<br>Set this to override the default output directory for this [WireOutput](../-wire-output/index.md). |
