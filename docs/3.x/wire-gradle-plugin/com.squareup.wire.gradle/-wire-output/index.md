//[wire-gradle-plugin](../../../index.md)/[com.squareup.wire.gradle](../index.md)/[WireOutput](index.md)

# WireOutput

[jvm]\
abstract class [WireOutput](index.md)

Specifies Wire's outputs (expressed as a list of Target objects) using Gradle's DSL (expressed as destination directories and configuration options). This includes registering output directories with the project so they can be compiled after they are generated.

## Constructors

| | |
|---|---|
| [WireOutput](-wire-output.md) | [jvm]<br>fun [WireOutput](-wire-output.md)() |

## Functions

| Name | Summary |
|---|---|
| [toTarget](to-target.md) | [jvm]<br>abstract fun [toTarget](to-target.md)(outputDirectory: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): Target<br>Transforms this [WireOutput](index.md) into a Target for which Wire will generate code. The Target should use [outputDirectory](to-target.md) instead of [WireOutput.out](--out--.md) in all cases for its output directory. |

## Properties

| Name | Summary |
|---|---|
| [out](--out--.md) | [jvm]<br>var [out](--out--.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null<br>Set this to override the default output directory for this [WireOutput](index.md). |

## Inheritors

| Name |
|---|
| [JavaOutput](../-java-output/index.md) |
| [KotlinOutput](../-kotlin-output/index.md) |
| [ProtoOutput](../-proto-output/index.md) |
| [CustomOutput](../-custom-output/index.md) |
