//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Loader](index.md)

# Loader

[common]\
interface [Loader](index.md)

Loads other files as needed by their import path.

## Functions

| Name | Summary |
|---|---|
| [load](load.md) | [common]<br>abstract fun [load](load.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ProtoFile](../-proto-file/index.md) |
| [withErrors](with-errors.md) | [common]<br>abstract fun [withErrors](with-errors.md)(errors: [ErrorCollector](../-error-collector/index.md)): [Loader](index.md)<br>Returns a new loader that reports failures to [errors](with-errors.md). |

## Inheritors

| Name |
|---|
| [CoreLoader](../-core-loader/index.md) |
| [SchemaLoader](../-schema-loader/index.md) |
