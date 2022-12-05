//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[SchemaHandler](index.md)

# SchemaHandler

[common]\
abstract class [SchemaHandler](index.md)

A [SchemaHandler](handle.md)s [Schema](../-schema/index.md)!

## Constructors

| | |
|---|---|
| [SchemaHandler](-schema-handler.md) | [common]<br>fun [SchemaHandler](-schema-handler.md)() |

## Types

| Name | Summary |
|---|---|
| [Context](-context/index.md) | [common]<br>data class [Context](-context/index.md)(fileSystem: FileSystem, outDirectory: Path, logger: [WireLogger](../../com.squareup.wire/-wire-logger/index.md), errorCollector: [ErrorCollector](../-error-collector/index.md), emittingRules: [EmittingRules](../-emitting-rules/index.md), claimedDefinitions: [ClaimedDefinitions](../-claimed-definitions/index.md)?, claimedPaths: [ClaimedPaths](../-claimed-paths/index.md), module: [SchemaHandler.Module](-module/index.md)?, sourcePathPaths: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;?, profileLoader: [ProfileLoader](../-profile-loader/index.md)?)<br>A [Context](-context/index.md) holds the information necessary for a [SchemaHandler](index.md) to do its job. It contains both helping objects such as [logger](-context/logger.md), and constraining objects such as [emittingRules](-context/emitting-rules.md). |
| [Factory](-factory/index.md) | [common]<br>interface [Factory](-factory/index.md) : Serializable<br>Implementations of this interface must have a no-arguments public constructor. |
| [Module](-module/index.md) | [common]<br>data class [Module](-module/index.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), types: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../-proto-type/index.md)&gt;, upstreamTypes: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[ProtoType](../-proto-type/index.md), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;)<br>A [Module](-module/index.md) dictates how the loaded types are to be partitioned and handled. |

## Functions

| Name | Summary |
|---|---|
| [handle](handle.md) | [common]<br>open fun [handle](handle.md)(schema: [Schema](../-schema/index.md), context: [SchemaHandler.Context](-context/index.md))<br>This will handle all [ProtoFile](../-proto-file/index.md)s which are part of the sourcePath. If a [Module](-module/index.md) is set in the [context](handle.md), it will handle only [Type](../-type/index.md)s and [Service](../-service/index.md)s the module defines respecting the [context](handle.md) rules. Override this method if you have specific needs the default implementation doesn't address.<br>[common]<br>abstract fun [handle](handle.md)(service: [Service](../-service/index.md), context: [SchemaHandler.Context](-context/index.md)): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Path&gt;<br>Returns the Paths of the files which [service](handle.md) will have been generated into. Null if nothing has been generated.<br>[common]<br>abstract fun [handle](handle.md)(type: [Type](../-type/index.md), context: [SchemaHandler.Context](-context/index.md)): Path?<br>Returns the Path of the file which [type](handle.md) will have been generated into. Null if nothing has been generated.<br>[common]<br>abstract fun [handle](handle.md)(extend: [Extend](../-extend/index.md), field: [Field](../-field/index.md), context: [SchemaHandler.Context](-context/index.md)): Path?<br>Returns the Path of the files which [field](handle.md) will have been generated into. Null if nothing has been generated. |
