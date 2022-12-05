//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Linker](index.md)

# Linker

[common]\
class [Linker](index.md)

Links local field types and option types to the corresponding declarations.

## Constructors

| | |
|---|---|
| [Linker](-linker.md) | [common]<br>fun [Linker](-linker.md)(loader: [Loader](../-loader/index.md), errors: [ErrorCollector](../-error-collector/index.md), permitPackageCycles: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), loadExhaustively: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [addType](add-type.md) | [common]<br>fun [addType](add-type.md)(protoType: [ProtoType](../-proto-type/index.md), type: [Type](../-type/index.md))<br>Adds [type](add-type.md). |
| [dereference](dereference.md) | [common]<br>fun [dereference](dereference.md)(protoType: [ProtoType](../-proto-type/index.md), field: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Field](../-field/index.md)?<br>Returns the field named [field](dereference.md) on the message type of [protoType](dereference.md). |
| [get](get.md) | [common]<br>fun [get](get.md)(protoType: [ProtoType](../-proto-type/index.md)): [Type](../-type/index.md)?<br>Returns the type or null if it doesn't exist. |
| [getForOptions](get-for-options.md) | [common]<br>fun [getForOptions](get-for-options.md)(protoType: [ProtoType](../-proto-type/index.md)): [Type](../-type/index.md)?<br>Returns the type or null if it doesn't exist. Before this returns it ensures members are linked so that options may dereference them. |
| [link](link.md) | [common]<br>fun [link](link.md)(sourceProtoFiles: [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[ProtoFile](../-proto-file/index.md)&gt;): [Schema](../-schema/index.md)<br>Link all features of all files in [sourceProtoFiles](link.md) to create a schema. This will also partially link any imported files necessary. |
| [request](request.md) | [common]<br>fun [request](request.md)(field: [Field](../-field/index.md))<br>Mark a field as used in an option so its file is retained in the schema. |
| [resolve](resolve.md) | [common]<br>fun &lt;[T](resolve.md)&gt; [resolve](resolve.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), map: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [T](resolve.md)&gt;): [T](resolve.md)? |
| [resolveContext](resolve-context.md) | [common]<br>fun [resolveContext](resolve-context.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [resolveMessageType](resolve-message-type.md) | [common]<br>fun [resolveMessageType](resolve-message-type.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ProtoType](../-proto-type/index.md)<br>Returns the type name for the relative or fully-qualified name [name](resolve-message-type.md). |
| [resolveType](resolve-type.md) | [common]<br>fun [resolveType](resolve-type.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ProtoType](../-proto-type/index.md)<br>Returns the type name for the scalar, relative or fully-qualified name [name](resolve-type.md). |
| [validateEnumConstantNameUniqueness](validate-enum-constant-name-uniqueness.md) | [common]<br>fun [validateEnumConstantNameUniqueness](validate-enum-constant-name-uniqueness.md)(nestedTypes: [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[Type](../-type/index.md)&gt;) |
| [validateFields](validate-fields.md) | [common]<br>fun [validateFields](validate-fields.md)(fields: [Iterable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterable/index.html)&lt;[Field](../-field/index.md)&gt;, reserveds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Reserved](../-reserved/index.md)&gt;, syntaxRules: [SyntaxRules](../-syntax-rules/index.md))<br>Validate that the tags of [fields](validate-fields.md) are unique and in range, that proto3 message cannot reference proto2 enums. |
| [validateImportForPath](validate-import-for-path.md) | [common]<br>fun [validateImportForPath](validate-import-for-path.md)(location: [Location](../-location/index.md), requiredImport: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [validateImportForType](validate-import-for-type.md) | [common]<br>fun [validateImportForType](validate-import-for-type.md)(location: [Location](../-location/index.md), type: [ProtoType](../-proto-type/index.md)) |
| [withContext](with-context.md) | [common]<br>fun [withContext](with-context.md)(context: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)): [Linker](index.md)<br>Returns a new linker that uses [context](with-context.md) to resolve type names and report errors. |

## Properties

| Name | Summary |
|---|---|
| [errors](errors.md) | [common]<br>val [errors](errors.md): [ErrorCollector](../-error-collector/index.md)<br>Errors accumulated by this load. |
| [loadExhaustively](load-exhaustively.md) | [common]<br>val [loadExhaustively](load-exhaustively.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
