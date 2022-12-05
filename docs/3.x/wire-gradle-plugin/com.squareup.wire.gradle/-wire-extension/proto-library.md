//[wire-gradle-plugin](../../../index.md)/[com.squareup.wire.gradle](../index.md)/[WireExtension](index.md)/[protoLibrary](proto-library.md)

# protoLibrary

[jvm]\

@get:Input

@get:Optional

var [protoLibrary](proto-library.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false

True to emit .proto files into the output resources. Use this when your .jar file can be used as a library for other proto or Wire projects.

Note that only the .proto files used in the library will be included, and these files will have tree-shaking applied.
