//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[CoreLoader](index.md)

# CoreLoader

[common]\
object [CoreLoader](index.md) : [Loader](../-loader/index.md)

[js]\
object [CoreLoader](index.md) : Loader

[jvm]\
object [CoreLoader](index.md) : Loader

A loader that can only load built-in .proto files:

<ul><li>Google's protobuf descriptor, which defines standard options like default, deprecated, and     java_package.</li><li>Wire's extensions, which defines since and until options.</li></ul>

If the user has provided their own version of these protos, those are preferred.

## Functions

| Name | Summary |
|---|---|
| load | [js, jvm, common]<br>[js]<br>open override fun [load]([js]load.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): ProtoFile<br>[jvm]<br>open override fun [load]([jvm]load.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): ProtoFile<br>[common]<br>abstract fun [load](../-loader/load.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ProtoFile](../-proto-file/index.md) |
| withErrors | [js]<br>open override fun [withErrors]([js]with-errors.md)(errors: ErrorCollector): [CoreLoader](index.md)<br>Returns a new loader that reports failures to [errors]([js]with-errors.md).<br>[jvm]<br>open override fun [withErrors]([jvm]with-errors.md)(errors: ErrorCollector): [CoreLoader](index.md)<br>Returns a new loader that reports failures to [errors]([jvm]with-errors.md).<br>[common]<br>abstract fun [withErrors](../-loader/with-errors.md)(errors: [ErrorCollector](../-error-collector/index.md)): [Loader](../-loader/index.md)<br>Returns a new loader that reports failures to [errors](../-loader/with-errors.md). |
