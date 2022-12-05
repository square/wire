//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Schema](index.md)

# Schema

[common]\
class [Schema](index.md)

A collection of .proto files that describe a set of messages. A schema is *linked*: each field's type name is resolved to the corresponding type definition.

Use [SchemaLoader](../-schema-loader/index.md) to load a schema from source files.

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [getField](get-field.md) | [common]<br>fun [getField](get-field.md)(protoMember: [ProtoMember](../-proto-member/index.md)): [Field](../-field/index.md)?<br>Returns the field for [protoMember](get-field.md), or null if this schema defines no such field.<br>[common]<br>fun [getField](get-field.md)(protoType: [ProtoType](../-proto-type/index.md), memberName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Field](../-field/index.md)?<br>Returns the field for [protoType](get-field.md) and [memberName](get-field.md), or null if this schema defines no such field.<br>[common]<br>fun [getField](get-field.md)(typeName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), memberName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Field](../-field/index.md)?<br>Returns the field with the fully qualified [typeName](get-field.md) and [memberName](get-field.md), or null if this schema defines no such field. |
| [getService](get-service.md) | [common]<br>fun [getService](get-service.md)(protoType: [ProtoType](../-proto-type/index.md)): [Service](../-service/index.md)?<br>Returns the service for [protoType](get-service.md), or null if this schema defines no such service.<br>[common]<br>fun [getService](get-service.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Service](../-service/index.md)?<br>Returns the service with the fully qualified name [name](get-service.md), or null if this schema defines no such service. |
| [getType](get-type.md) | [common]<br>fun [getType](get-type.md)(protoType: [ProtoType](../-proto-type/index.md)): [Type](../-type/index.md)?<br>Returns the type for [protoType](get-type.md), or null if this schema defines no such type.<br>[common]<br>fun [getType](get-type.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Type](../-type/index.md)?<br>Returns the type with the fully qualified name [name](get-type.md), or null if this schema defines no such type. |
| [isExtensionField](is-extension-field.md) | [common]<br>fun [isExtensionField](is-extension-field.md)(protoMember: [ProtoMember](../-proto-member/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [protoAdapter](proto-adapter.md) | [common]<br>fun [protoAdapter](proto-adapter.md)(typeName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), includeUnknown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): ProtoAdapter&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;<br>Returns a wire adapter for the message or enum type named [typeName](proto-adapter.md). The returned type adapter doesn't have model classes to encode and decode from, so instead it uses scalar types ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ByteString, Integer, etc.), [maps](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html), and [lists](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html). It can both encode and decode these objects. Map keys are field names. |
| [protoFile](proto-file.md) | [common]<br>fun [protoFile](proto-file.md)(protoType: [ProtoType](../-proto-type/index.md)): [ProtoFile](../-proto-file/index.md)?<br>Returns the proto file containing this [protoType](proto-file.md), or null if there isn't such file.<br>[common]<br>fun [protoFile](proto-file.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ProtoFile](../-proto-file/index.md)?<br>fun [protoFile](proto-file.md)(path: Path): [ProtoFile](../-proto-file/index.md)?<br>Returns the proto file at [path](proto-file.md), or null if this schema has no such file. |
| [prune](prune.md) | [common]<br>fun [prune](prune.md)(pruningRules: [PruningRules](../-pruning-rules/index.md)): [Schema](index.md)<br>Returns a copy of this schema that retains only the types and services selected by [pruningRules](prune.md), plus their transitive dependencies. |

## Properties

| Name | Summary |
|---|---|
| [protoFiles](proto-files.md) | [common]<br>val [protoFiles](proto-files.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ProtoFile](../-proto-file/index.md)&gt; |
| [types](types.md) | [common]<br>val [types](types.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../-proto-type/index.md)&gt; |

## Extensions

| Name | Summary |
|---|---|
| [withStubs](../../com.squareup.wire.schema.internal/with-stubs.md) | [common]<br>fun [Schema](index.md).[withStubs](../../com.squareup.wire.schema.internal/with-stubs.md)(typesToStub: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../-proto-type/index.md)&gt;): [Schema](index.md)<br>Replace types in this schema which are present in [typesToStub](../../com.squareup.wire.schema.internal/with-stubs.md) with empty shells that have no outward references. This has to be done in this module so that we can access the internal constructor to avoid re-linking. |
