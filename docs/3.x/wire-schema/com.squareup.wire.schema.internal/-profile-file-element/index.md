//[wire-schema](../../../index.md)/[com.squareup.wire.schema.internal](../index.md)/[ProfileFileElement](index.md)

# ProfileFileElement

[common]\
data class [ProfileFileElement](index.md)(location: [Location](../../com.squareup.wire.schema/-location/index.md), packageName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, imports: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, typeConfigs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeConfigElement](../-type-config-element/index.md)&gt;)

A single .wire file. This file is structured similarly to a .proto file, but with different elements.

File Structure

 --------------

A project may have 0 or more .wire files. These files should be in the same directory as the .proto files so they may be automatically discovered by Wire.

Each file starts with a syntax declaration. The syntax must be "wire2". This is followed by an optional package declaration, which should match to the package declarations of the .proto files in the directory.

Profiles may import any number of proto files. Note that it is an error to import .wire files. These imports are used to resolve types specified later in the file.

Profiles may specify any number of type configurations. These specify a fully qualified type, its target Java type, and an adapter to do the encoding and decoding.

syntax = "wire2";\
package squareup.dinosaurs;\
\
import "squareup/geology/period.proto";\
\
// Roar!\
type squareup.dinosaurs.Dinosaur {\
target com.squareup.dino.Dinosaur using com.squareup.dino.Dinosaurs#DINO_ADAPTER;\
}

## Constructors

| | |
|---|---|
| [ProfileFileElement](-profile-file-element.md) | [common]<br>fun [ProfileFileElement](-profile-file-element.md)(location: [Location](../../com.squareup.wire.schema/-location/index.md), packageName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, imports: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = emptyList(), typeConfigs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeConfigElement](../-type-config-element/index.md)&gt; = emptyList()) |

## Functions

| Name | Summary |
|---|---|
| [toSchema](to-schema.md) | [common]<br>fun [toSchema](to-schema.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [imports](imports.md) | [common]<br>val [imports](imports.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [location](location.md) | [common]<br>val [location](location.md): [Location](../../com.squareup.wire.schema/-location/index.md) |
| [packageName](package-name.md) | [common]<br>val [packageName](package-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [typeConfigs](type-configs.md) | [common]<br>val [typeConfigs](type-configs.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeConfigElement](../-type-config-element/index.md)&gt; |
