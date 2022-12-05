//[wire-schema](../../../../index.md)/[com.squareup.wire.schema](../../index.md)/[SchemaHandler](../index.md)/[Context](index.md)

# Context

[common]\
data class [Context](index.md)(fileSystem: FileSystem, outDirectory: Path, logger: [WireLogger](../../../com.squareup.wire/-wire-logger/index.md), errorCollector: [ErrorCollector](../../-error-collector/index.md), emittingRules: [EmittingRules](../../-emitting-rules/index.md), claimedDefinitions: [ClaimedDefinitions](../../-claimed-definitions/index.md)?, claimedPaths: [ClaimedPaths](../../-claimed-paths/index.md), module: [SchemaHandler.Module](../-module/index.md)?, sourcePathPaths: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;?, profileLoader: [ProfileLoader](../../-profile-loader/index.md)?)

A [Context](index.md) holds the information necessary for a [SchemaHandler](../index.md) to do its job. It contains both helping objects such as [logger](logger.md), and constraining objects such as [emittingRules](emitting-rules.md).

## Constructors

| | |
|---|---|
| [Context](-context.md) | [common]<br>fun [Context](-context.md)(fileSystem: FileSystem, outDirectory: Path, logger: [WireLogger](../../../com.squareup.wire/-wire-logger/index.md), errorCollector: [ErrorCollector](../../-error-collector/index.md) = ErrorCollector(), emittingRules: [EmittingRules](../../-emitting-rules/index.md) = EmittingRules(), claimedDefinitions: [ClaimedDefinitions](../../-claimed-definitions/index.md)? = null, claimedPaths: [ClaimedPaths](../../-claimed-paths/index.md) = ClaimedPaths(), module: [SchemaHandler.Module](../-module/index.md)? = null, sourcePathPaths: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null, profileLoader: [ProfileLoader](../../-profile-loader/index.md)? = null) |

## Functions

| Name | Summary |
|---|---|
| [inSourcePath](in-source-path.md) | [common]<br>fun [inSourcePath](in-source-path.md)(location: [Location](../../-location/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>True if this [location](in-source-path.md) ia part of a sourcePath root.<br>[common]<br>fun [inSourcePath](in-source-path.md)(protoFile: [ProtoFile](../../-proto-file/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>True if this [protoFile](in-source-path.md) ia part of a sourcePath root. |

## Properties

| Name | Summary |
|---|---|
| [claimedDefinitions](claimed-definitions.md) | [common]<br>val [claimedDefinitions](claimed-definitions.md): [ClaimedDefinitions](../../-claimed-definitions/index.md)? = null<br>If set, the [SchemaHandler](../index.md) is to handle only types which are not claimed yet, and claim itself types it has handled. If null, the [SchemaHandler](../index.md) is to handle all types. |
| [claimedPaths](claimed-paths.md) | [common]<br>val [claimedPaths](claimed-paths.md): [ClaimedPaths](../../-claimed-paths/index.md)<br>If the [SchemaHandler](../index.md) writes files, it is to claim Paths of files it created. |
| [emittingRules](emitting-rules.md) | [common]<br>val [emittingRules](emitting-rules.md): [EmittingRules](../../-emitting-rules/index.md)<br>Set of rules letting the [SchemaHandler](../index.md) know what [ProtoType](../../-proto-type/index.md) to include or exclude in its logic. This object represents the includes and excludes values which were associated with its [Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html). |
| [errorCollector](error-collector.md) | [common]<br>val [errorCollector](error-collector.md): [ErrorCollector](../../-error-collector/index.md)<br>Object to be used by the [SchemaHandler](../index.md) to store errors. After all [SchemaHandler](../index.md)s are finished, Wire will throw an exception if any error are present inside the collector. |
| [fileSystem](file-system.md) | [common]<br>val [fileSystem](file-system.md): FileSystem<br>To be used by the [SchemaHandler](../index.md) for reading/writing operations on disk. |
| [logger](logger.md) | [common]<br>val [logger](logger.md): [WireLogger](../../../com.squareup.wire/-wire-logger/index.md)<br>Event-listener like logger with which [SchemaHandler](../index.md) can notify handled artifacts. |
| [module](module.md) | [common]<br>val [module](module.md): [SchemaHandler.Module](../-module/index.md)? = null<br>A [Module](../-module/index.md) dictates how the loaded types are partitioned and how they are to be handled. If null, there are no partition and all types are to be handled. |
| [outDirectory](out-directory.md) | [common]<br>val [outDirectory](out-directory.md): Path<br>Location on [fileSystem](file-system.md) where the [SchemaHandler](../index.md) is to write files, if it needs to. |
| [profileLoader](profile-loader.md) | [common]<br>val [profileLoader](profile-loader.md): [ProfileLoader](../../-profile-loader/index.md)? = null<br>To be used by the [SchemaHandler](../index.md) if it supports [Profile](../../-profile/index.md) files. Please note that this API is unstable and can change at anytime. |
| [sourcePathPaths](source-path-paths.md) | [common]<br>val [sourcePathPaths](source-path-paths.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null<br>Contains [Location.path](../../-location/path.md) values of all sourcePath roots. The [SchemaHandler](../index.md) is to ignore [ProtoFile](../../-proto-file/index.md)s not part of this set; this verification can be executed via the [inSourcePath](in-source-path.md) method. |
