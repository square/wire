//[wire-gradle-plugin](../../../index.md)/[com.squareup.wire.gradle](../index.md)/[WireExtension](index.md)/[sourcePath](source-path.md)

# sourcePath

[jvm]\
fun [sourcePath](source-path.md)(vararg sourcePaths: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Source paths for local jars and directories, as well as remote binary dependencies

[jvm]\
fun [sourcePath](source-path.md)(action: Action&lt;[WireExtension.ProtoRootSet](-proto-root-set/index.md)&gt;)

Source paths for local file trees, backed by a org.gradle.api.file.SourceDirectorySet Must provide at least a org.gradle.api.file.SourceDirectorySet.srcDir
