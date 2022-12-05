//[wire-schema](../../../../index.md)/[com.squareup.wire.schema](../../index.md)/[SchemaHandler](../index.md)/[Context](index.md)/[sourcePathPaths](source-path-paths.md)

# sourcePathPaths

[common]\
val [sourcePathPaths](source-path-paths.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null

Contains [Location.path](../../-location/path.md) values of all sourcePath roots. The [SchemaHandler](../index.md) is to ignore [ProtoFile](../../-proto-file/index.md)s not part of this set; this verification can be executed via the [inSourcePath](in-source-path.md) method.
