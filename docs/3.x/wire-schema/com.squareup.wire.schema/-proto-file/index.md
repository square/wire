//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[ProtoFile](index.md)

# ProtoFile

[common]\
data class [ProtoFile](index.md)(location: [Location](../-location/index.md), imports: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, publicImports: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, packageName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, types: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt;, services: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Service](../-service/index.md)&gt;, extendList: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extend](../-extend/index.md)&gt;, options: [Options](../-options/index.md), syntax: Syntax?)

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [javaPackage](java-package.md) | [common]<br>fun [javaPackage](java-package.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [linkOptions](link-options.md) | [common]<br>fun [linkOptions](link-options.md)(linker: [Linker](../-linker/index.md), validate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [name](name.md) | [common]<br>fun [name](name.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Returns the name of this proto file, like simple_message for squareup/protos/person/simple_message.proto. |
| [retainAll](retain-all.md) | [common]<br>fun [retainAll](retain-all.md)(schema: [Schema](../-schema/index.md), markSet: [MarkSet](../-mark-set/index.md)): [ProtoFile](index.md)<br>Returns a new proto file that omits types, services, extensions, and options not in pruningRules. |
| [retainImports](retain-imports.md) | [common]<br>fun [retainImports](retain-imports.md)(retained: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ProtoFile](index.md)&gt;): [ProtoFile](index.md)<br>Returns a new proto file that omits unnecessary imports. |
| [retainLinked](retain-linked.md) | [common]<br>fun [retainLinked](retain-linked.md)(linkedTypes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../-proto-type/index.md)&gt;, linkedFields: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Field](../-field/index.md)&gt;): [ProtoFile](index.md)<br>Return a copy of this file with only the marked types. |
| [toElement](to-element.md) | [common]<br>fun [toElement](to-element.md)(): [ProtoFileElement](../../com.squareup.wire.schema.internal.parser/-proto-file-element/index.md) |
| [toSchema](to-schema.md) | [common]<br>fun [toSchema](to-schema.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [toString](to-string.md) | [common]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [typesAndNestedTypes](types-and-nested-types.md) | [common]<br>fun [typesAndNestedTypes](types-and-nested-types.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt;<br>Returns all types and subtypes which are found in the proto file. |
| [wirePackage](wire-package.md) | [common]<br>fun [wirePackage](wire-package.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |

## Properties

| Name | Summary |
|---|---|
| [extendList](extend-list.md) | [common]<br>val [extendList](extend-list.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Extend](../-extend/index.md)&gt; |
| [imports](imports.md) | [common]<br>val [imports](imports.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [location](location.md) | [common]<br>val [location](location.md): [Location](../-location/index.md) |
| [options](options.md) | [common]<br>val [options](options.md): [Options](../-options/index.md) |
| [packageName](package-name.md) | [common]<br>val [packageName](package-name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [publicImports](public-imports.md) | [common]<br>val [publicImports](public-imports.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [services](services.md) | [common]<br>val [services](services.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Service](../-service/index.md)&gt; |
| [syntax](syntax.md) | [common]<br>val [syntax](syntax.md): Syntax? |
| [types](types.md) | [common]<br>val [types](types.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Type](../-type/index.md)&gt; |
