//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[SchemaLoader](index.md)

# SchemaLoader

[common]\
class [SchemaLoader](index.md) : [Loader](../-loader/index.md), [ProfileLoader](../-profile-loader/index.md)

Load proto files and their transitive dependencies and parse them. Keep track of which files were loaded from where so that we can use that information later when deciding what to generate.

[js, jvm]\
class [SchemaLoader](index.md) : Loader, ProfileLoader

## Constructors

| | |
|---|---|
| [SchemaLoader](-schema-loader.md) | [js]<br>fun [SchemaLoader](-schema-loader.md)(fileSystem: FileSystem) |
| [SchemaLoader](-schema-loader.md) | [jvm]<br>fun [SchemaLoader](-schema-loader.md)(fileSystem: FileSystem) |
| [SchemaLoader](-schema-loader.md) | [jvm]<br>fun [SchemaLoader](-schema-loader.md)(fileSystem: [FileSystem](https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html)) |

## Functions

| Name | Summary |
|---|---|
| [initRoots](init-roots.md) | [common, js, jvm]<br>[common]<br>fun [initRoots](init-roots.md)(sourcePath: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Location](../-location/index.md)&gt;, protoPath: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Location](../-location/index.md)&gt; = listOf())<br>[js, jvm]<br>fun [initRoots](init-roots.md)(sourcePath: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Location&gt;, protoPath: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Location&gt; = listOf())<br>Initialize the WireRun.sourcePath and WireRun.protoPath from which files are loaded. |
| load | [common, js, jvm]<br>[common]<br>abstract fun [load](../-loader/load.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ProtoFile](../-proto-file/index.md)<br>[js]<br>open override fun [load]([js]load.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): ProtoFile<br>[jvm]<br>open override fun [load]([jvm]load.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): ProtoFile |
| loadProfile | [common, js, jvm]<br>[common]<br>abstract fun [loadProfile](../-profile-loader/load-profile.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), schema: [Schema](../-schema/index.md)): [Profile](../-profile/index.md)<br>[js]<br>open override fun [loadProfile]([js]load-profile.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), schema: Schema): [Profile](../-profile/index.md)<br>[jvm]<br>open override fun [loadProfile]([jvm]load-profile.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), schema: Schema): [Profile](../-profile/index.md) |
| [loadSchema](load-schema.md) | [common, js, jvm]<br>[common]<br>fun [loadSchema](load-schema.md)(): [Schema](../-schema/index.md)<br>[js, jvm]<br>fun [loadSchema](load-schema.md)(): Schema |
| withErrors | [common]<br>abstract fun [withErrors](../-loader/with-errors.md)(errors: [ErrorCollector](../-error-collector/index.md)): [Loader](../-loader/index.md)<br>Returns a new loader that reports failures to [errors](../-loader/with-errors.md).<br>[js]<br>open override fun [withErrors]([js]with-errors.md)(errors: ErrorCollector): [SchemaLoader](index.md)<br>Returns a new loader that reports failures to [errors]([js]with-errors.md).<br>[jvm]<br>open override fun [withErrors]([jvm]with-errors.md)(errors: ErrorCollector): [SchemaLoader](index.md)<br>Returns a new loader that reports failures to [errors]([jvm]with-errors.md). |

## Properties

| Name | Summary |
|---|---|
| [loadExhaustively](load-exhaustively.md) | [common, js, jvm]<br>var [loadExhaustively](load-exhaustively.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>If true, the schema loader will load the whole graph, including files and types not used by anything in the source path. |
| [permitPackageCycles](permit-package-cycles.md) | [common, js, jvm]<br>var [permitPackageCycles](permit-package-cycles.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Strict by default. Note that golang cannot build protos with package cycles. |
| [sourcePathFiles](source-path-files.md) | [common]<br>val [sourcePathFiles](source-path-files.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ProtoFile](../-proto-file/index.md)&gt;<br>Subset of the schema that was loaded from the source path.<br>[js, jvm]<br>val [sourcePathFiles](source-path-files.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;ProtoFile&gt;<br>Subset of the schema that was loaded from the source path. |
