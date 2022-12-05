//[wire-gradle-plugin](../../../index.md)/[com.squareup.wire.gradle](../index.md)/[WireExtension](index.md)/[protoPath](proto-path.md)

# protoPath

[jvm]\
fun [protoPath](proto-path.md)(vararg protoPaths: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Proto paths for local jars and directories, as well as remote binary dependencies

[jvm]\
fun [protoPath](proto-path.md)(action: Action&lt;[WireExtension.ProtoRootSet](-proto-root-set/index.md)&gt;)

Proto paths for local file trees, backed by a org.gradle.api.file.SourceDirectorySet Must provide at least a org.gradle.api.file.SourceDirectorySet.srcDir
