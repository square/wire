//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[SchemaHandler](index.md)/[handle](handle.md)

# handle

[common]\
open fun [handle](handle.md)(schema: [Schema](../-schema/index.md), context: [SchemaHandler.Context](-context/index.md))

This will handle all [ProtoFile](../-proto-file/index.md)s which are part of the sourcePath. If a [Module](-module/index.md) is set in the [context](handle.md), it will handle only [Type](../-type/index.md)s and [Service](../-service/index.md)s the module defines respecting the [context](handle.md) rules. Override this method if you have specific needs the default implementation doesn't address.

[common]\
abstract fun [handle](handle.md)(type: [Type](../-type/index.md), context: [SchemaHandler.Context](-context/index.md)): Path?

Returns the Path of the file which [type](handle.md) will have been generated into. Null if nothing has been generated.

[common]\
abstract fun [handle](handle.md)(service: [Service](../-service/index.md), context: [SchemaHandler.Context](-context/index.md)): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Path&gt;

Returns the Paths of the files which [service](handle.md) will have been generated into. Null if nothing has been generated.

[common]\
abstract fun [handle](handle.md)(extend: [Extend](../-extend/index.md), field: [Field](../-field/index.md), context: [SchemaHandler.Context](-context/index.md)): Path?

Returns the Path of the files which [field](handle.md) will have been generated into. Null if nothing has been generated.
