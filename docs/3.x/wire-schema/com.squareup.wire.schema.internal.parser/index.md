//[wire-schema](../../index.md)/[com.squareup.wire.schema.internal.parser](index.md)

# Package com.squareup.wire.schema.internal.parser

## Types

| Name | Summary |
|---|---|
| [EnumConstantElement](-enum-constant-element/index.md) | [common]<br>data class [EnumConstantElement](-enum-constant-element/index.md)(location: [Location](../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](-option-element/index.md)&gt;) |
| [EnumElement](-enum-element/index.md) | [common]<br>data class [EnumElement](-enum-element/index.md)(location: [Location](../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](-option-element/index.md)&gt;, constants: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[EnumConstantElement](-enum-constant-element/index.md)&gt;, reserveds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ReservedElement](-reserved-element/index.md)&gt;) : [TypeElement](-type-element/index.md) |
| [ExtendElement](-extend-element/index.md) | [common]<br>data class [ExtendElement](-extend-element/index.md)(location: [Location](../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), fields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[FieldElement](-field-element/index.md)&gt;) |
| [ExtensionsElement](-extensions-element/index.md) | [common]<br>data class [ExtensionsElement](-extensions-element/index.md)(location: [Location](../com.squareup.wire.schema/-location/index.md), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), values: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;) |
| [FieldElement](-field-element/index.md) | [common]<br>data class [FieldElement](-field-element/index.md)(location: [Location](../com.squareup.wire.schema/-location/index.md), label: [Field.Label](../com.squareup.wire.schema/-field/-label/index.md)?, type: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), defaultValue: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, jsonName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](-option-element/index.md)&gt;) |
| [GroupElement](-group-element/index.md) | [common]<br>data class [GroupElement](-group-element/index.md)(label: [Field.Label](../com.squareup.wire.schema/-field/-label/index.md)?, location: [Location](../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), tag: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), fields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[FieldElement](-field-element/index.md)&gt;) |
| [MessageElement](-message-element/index.md) | [common]<br>data class [MessageElement](-message-element/index.md)(location: [Location](../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), nestedTypes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](-type-element/index.md)&gt;, options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](-option-element/index.md)&gt;, reserveds: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ReservedElement](-reserved-element/index.md)&gt;, fields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[FieldElement](-field-element/index.md)&gt;, oneOfs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OneOfElement](-one-of-element/index.md)&gt;, extensions: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ExtensionsElement](-extensions-element/index.md)&gt;, groups: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[GroupElement](-group-element/index.md)&gt;, extendDeclarations: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ExtendElement](-extend-element/index.md)&gt;) : [TypeElement](-type-element/index.md) |
| [OneOfElement](-one-of-element/index.md) | [common]<br>data class [OneOfElement](-one-of-element/index.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), fields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[FieldElement](-field-element/index.md)&gt;, groups: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[GroupElement](-group-element/index.md)&gt;, options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](-option-element/index.md)&gt;) |
| [OptionElement](-option-element/index.md) | [common]<br>data class [OptionElement](-option-element/index.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), kind: [OptionElement.Kind](-option-element/-kind/index.md), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), isParenthesized: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [OptionReader](-option-reader/index.md) | [common]<br>class [OptionReader](-option-reader/index.md)(reader: [SyntaxReader](-syntax-reader/index.md)) |
| [ProtoFileElement](-proto-file-element/index.md) | [common]<br>data class [ProtoFileElement](-proto-file-element/index.md)(location: [Location](../com.squareup.wire.schema/-location/index.md), packageName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, syntax: Syntax?, imports: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, publicImports: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, types: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[TypeElement](-type-element/index.md)&gt;, services: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ServiceElement](-service-element/index.md)&gt;, extendDeclarations: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ExtendElement](-extend-element/index.md)&gt;, options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](-option-element/index.md)&gt;)<br>A single .proto file. |
| [ProtoParser](-proto-parser/index.md) | [common]<br>class [ProtoParser](-proto-parser/index.md)<br>Basic parser for .proto schema declarations. |
| [ReservedElement](-reserved-element/index.md) | [common]<br>data class [ReservedElement](-reserved-element/index.md)(location: [Location](../com.squareup.wire.schema/-location/index.md), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), values: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;) |
| [RpcElement](-rpc-element/index.md) | [common]<br>data class [RpcElement](-rpc-element/index.md)(location: [Location](../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), responseType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestStreaming: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), responseStreaming: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](-option-element/index.md)&gt;) |
| [ServiceElement](-service-element/index.md) | [common]<br>data class [ServiceElement](-service-element/index.md)(location: [Location](../com.squareup.wire.schema/-location/index.md), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), documentation: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), rpcs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[RpcElement](-rpc-element/index.md)&gt;, options: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](-option-element/index.md)&gt;) |
| [SyntaxReader](-syntax-reader/index.md) | [common]<br>class [SyntaxReader](-syntax-reader/index.md)(data: [CharArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-array/index.html), location: [Location](../com.squareup.wire.schema/-location/index.md))<br>A general purpose reader for formats like .proto. |
| [TypeElement](-type-element/index.md) | [common]<br>interface [TypeElement](-type-element/index.md)<br>A message type or enum type declaration. |