//[wire-gradle-plugin](../../../index.md)/[com.squareup.wire.gradle](../index.md)/[WireExtension](index.md)

# WireExtension

[jvm]\
open class [WireExtension](index.md)(project: Project)

## Types

| Name | Summary |
|---|---|
| [ProtoRootSet](-proto-root-set/index.md) | [jvm]<br>open class [ProtoRootSet](-proto-root-set/index.md) |

## Functions

| Name | Summary |
|---|---|
| [custom](custom.md) | [jvm]<br>fun [custom](custom.md)(action: Action&lt;[CustomOutput](../-custom-output/index.md)&gt;) |
| [getProtoJars](get-proto-jars.md) | [jvm]<br>@InputFiles<br>@Optional<br>fun [getProtoJars](get-proto-jars.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[WireExtension.ProtoRootSet](-proto-root-set/index.md)&gt; |
| [getProtoPaths](get-proto-paths.md) | [jvm]<br>@InputFiles<br>@Optional<br>fun [getProtoPaths](get-proto-paths.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [getProtoTrees](get-proto-trees.md) | [jvm]<br>@InputFiles<br>@Optional<br>fun [getProtoTrees](get-proto-trees.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;SourceDirectorySet&gt; |
| [getSourceJars](get-source-jars.md) | [jvm]<br>@InputFiles<br>@Optional<br>fun [getSourceJars](get-source-jars.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[WireExtension.ProtoRootSet](-proto-root-set/index.md)&gt; |
| [getSourcePaths](get-source-paths.md) | [jvm]<br>@InputFiles<br>@Optional<br>fun [getSourcePaths](get-source-paths.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [getSourceTrees](get-source-trees.md) | [jvm]<br>@InputFiles<br>@Optional<br>fun [getSourceTrees](get-source-trees.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;SourceDirectorySet&gt; |
| [java](java.md) | [jvm]<br>fun [java](java.md)(action: Action&lt;[JavaOutput](../-java-output/index.md)&gt;) |
| [kotlin](kotlin.md) | [jvm]<br>fun [kotlin](kotlin.md)(action: Action&lt;[KotlinOutput](../-kotlin-output/index.md)&gt;) |
| [move](move.md) | [jvm]<br>fun [move](move.md)(action: Action&lt;[Move](../-move/index.md)&gt;) |
| [onlyVersion](only-version.md) | [jvm]<br>@Input<br>@Optional<br>fun [onlyVersion](only-version.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>[jvm]<br>fun [onlyVersion](only-version.md)(onlyVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>See com.squareup.wire.schema.WireRun.onlyVersion. |
| [permitPackageCycles](permit-package-cycles.md) | [jvm]<br>@Input<br>fun [permitPackageCycles](permit-package-cycles.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>[jvm]<br>fun [permitPackageCycles](permit-package-cycles.md)(permitPackageCycles: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html))<br>See com.squareup.wire.schema.WireRun.permitPackageCycles |
| [proto](proto.md) | [jvm]<br>fun [proto](proto.md)(action: Action&lt;[ProtoOutput](../-proto-output/index.md)&gt;) |
| [protoPath](proto-path.md) | [jvm]<br>fun [protoPath](proto-path.md)(vararg protoPaths: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Proto paths for local jars and directories, as well as remote binary dependencies<br>[jvm]<br>fun [protoPath](proto-path.md)(action: Action&lt;[WireExtension.ProtoRootSet](-proto-root-set/index.md)&gt;)<br>Proto paths for local file trees, backed by a org.gradle.api.file.SourceDirectorySet Must provide at least a org.gradle.api.file.SourceDirectorySet.srcDir |
| [prune](prune.md) | [jvm]<br>fun [prune](prune.md)(vararg prunes: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>See com.squareup.wire.schema.WireRun.treeShakingRubbish |
| [prunes](prunes.md) | [jvm]<br>@Input<br>@Optional<br>fun [prunes](prunes.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [root](root.md) | [jvm]<br>fun [root](root.md)(vararg roots: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>See com.squareup.wire.schema.WireRun.treeShakingRoots |
| [roots](roots.md) | [jvm]<br>@Input<br>@Optional<br>fun [roots](roots.md)(): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [sinceVersion](since-version.md) | [jvm]<br>@Input<br>@Optional<br>fun [sinceVersion](since-version.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>[jvm]<br>fun [sinceVersion](since-version.md)(sinceVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>See com.squareup.wire.schema.WireRun.sinceVersion |
| [sourcePath](source-path.md) | [jvm]<br>fun [sourcePath](source-path.md)(vararg sourcePaths: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Source paths for local jars and directories, as well as remote binary dependencies<br>[jvm]<br>fun [sourcePath](source-path.md)(action: Action&lt;[WireExtension.ProtoRootSet](-proto-root-set/index.md)&gt;)<br>Source paths for local file trees, backed by a org.gradle.api.file.SourceDirectorySet Must provide at least a org.gradle.api.file.SourceDirectorySet.srcDir |
| [untilVersion](until-version.md) | [jvm]<br>@Input<br>@Optional<br>fun [untilVersion](until-version.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>[jvm]<br>fun [untilVersion](until-version.md)(untilVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>See com.squareup.wire.schema.WireRun.untilVersion |

## Properties

| Name | Summary |
|---|---|
| [outputs](outputs.md) | [jvm]<br>@get:Input<br>val [outputs](outputs.md): [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[WireOutput](../-wire-output/index.md)&gt;<br>Specified what types to output where. Maps to com.squareup.wire.schema.Target |
| [protoLibrary](proto-library.md) | [jvm]<br>@get:Input<br>@get:Optional<br>var [protoLibrary](proto-library.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false<br>True to emit .proto files into the output resources. Use this when your .jar file can be used as a library for other proto or Wire projects. |
| [rules](rules.md) | [jvm]<br>@get:Input<br>@get:Optional<br>var [rules](rules.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null<br>A user-provided file listing [roots](roots.md) and [prunes](prunes.md) |
