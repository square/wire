//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Schema](index.md)/[protoAdapter](proto-adapter.md)

# protoAdapter

[common]\
fun [protoAdapter](proto-adapter.md)(typeName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), includeUnknown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): ProtoAdapter&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

Returns a wire adapter for the message or enum type named [typeName](proto-adapter.md). The returned type adapter doesn't have model classes to encode and decode from, so instead it uses scalar types ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ByteString, Integer, etc.), [maps](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html), and [lists](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html). It can both encode and decode these objects. Map keys are field names.

## Parameters

common

| | |
|---|---|
| includeUnknown | true to include values for unknown tags in the returned model. Map keys for such values is the unknown value's tag name as a string. Unknown values are decoded to [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), Integer, or ByteString for VARINT, FIXED64, FIXED32, or LENGTH_DELIMITED, respectively. |
