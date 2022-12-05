//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Options](index.md)

# Options

[common]\
class [Options](index.md)(optionType: [ProtoType](../-proto-type/index.md), optionElements: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../../com.squareup.wire.schema.internal.parser/-option-element/index.md)&gt;)

A set of options declared on a message declaration, field declaration, enum declaration, enum constant declaration, service declaration, RPC method declaration, or proto file declaration. Options values may be arbitrary protocol buffer messages, but must be valid protocol buffer messages.

## Constructors

| | |
|---|---|
| [Options](-options.md) | [common]<br>fun [Options](-options.md)(optionType: [ProtoType](../-proto-type/index.md), optionElements: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../../com.squareup.wire.schema.internal.parser/-option-element/index.md)&gt;) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [fields](fields.md) | [common]<br>fun [fields](fields.md)(): [Multimap](../-multimap/index.md)&lt;[ProtoType](../-proto-type/index.md), [ProtoMember](../-proto-member/index.md)&gt;<br>fun [fields](fields.md)(pruningRules: [PruningRules](../-pruning-rules/index.md)): [Multimap](../-multimap/index.md)&lt;[ProtoType](../-proto-type/index.md), [ProtoMember](../-proto-member/index.md)&gt; |
| [get](get.md) | [common]<br>fun [get](get.md)(protoMember: [ProtoMember](../-proto-member/index.md)): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)? |
| [link](link.md) | [common]<br>fun [link](link.md)(linker: [Linker](../-linker/index.md), location: [Location](../-location/index.md), validate: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [optionMatches](option-matches.md) | [common]<br>fun [optionMatches](option-matches.md)(namePattern: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), valuePattern: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if any of the options in entries matches both of the regular expressions provided: its name matches the option's name and its value matches the option's value. |
| [retainAll](retain-all.md) | [common]<br>fun [retainAll](retain-all.md)(schema: [Schema](../-schema/index.md), markSet: [MarkSet](../-mark-set/index.md)): [Options](index.md) |
| [retainLinked](retain-linked.md) | [common]<br>fun [retainLinked](retain-linked.md)(): [Options](index.md) |

## Properties

| Name | Summary |
|---|---|
| [elements](elements.md) | [common]<br>val [elements](elements.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[OptionElement](../../com.squareup.wire.schema.internal.parser/-option-element/index.md)&gt; |
| [map](map.md) | [common]<br>val [map](map.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[ProtoMember](../-proto-member/index.md), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?&gt; |
