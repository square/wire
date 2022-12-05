//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[MarkSet](index.md)

# MarkSet

[common]\
class [MarkSet](index.md)(pruningRules: [PruningRules](../-pruning-rules/index.md))

A mark set is used in three phases:

<ol><li>Marking root types and root members. These are the identifiers specifically identified by     the user in the includes set. In this phase it is an error to mark a type that is excluded,     or to mark both a type and one of its members.</li><li>Marking members transitively reachable by those roots. In this phase if a member is visited,     the member's enclosing type is marked instead, unless it is of a type that has a specific     member already marked.</li><li>Retaining which members and types have been marked.</li></ol>

## Constructors

| | |
|---|---|
| [MarkSet](-mark-set.md) | [common]<br>fun [MarkSet](-mark-set.md)(pruningRules: [PruningRules](../-pruning-rules/index.md)) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [contains](contains.md) | [common]<br>operator fun [contains](contains.md)(protoMember: [ProtoMember](../-proto-member/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if member is marked and should be retained.<br>[common]<br>operator fun [contains](contains.md)(type: [ProtoType](../-proto-type/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if type is marked and should be retained. |
| [mark](mark.md) | [common]<br>fun [mark](mark.md)(protoMember: [ProtoMember](../-proto-member/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Marks a member as transitively reachable by the includes set. Returns true if the mark is new, the member will be retained, and reachable objects should be traversed.<br>[common]<br>fun [mark](mark.md)(type: [ProtoType](../-proto-type/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>fun [mark](mark.md)(type: [ProtoType](../-proto-type/index.md), reference: [ProtoMember](../-proto-member/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Marks a type as transitively reachable by the includes set. Returns true if the mark is new, the type will be retained, and reachable objects should be traversed. |
| [root](root.md) | [common]<br>fun [root](root.md)(protoMember: [ProtoMember](../-proto-member/index.md))<br>Marks protoMember, throwing if it is explicitly excluded. This implicitly excludes other members of the same type.<br>[common]<br>fun [root](root.md)(type: [ProtoType](../-proto-type/index.md))<br>Marks type, throwing if it is explicitly excluded. |

## Properties

| Name | Summary |
|---|---|
| [members](members.md) | [common]<br>val [members](members.md): [MutableMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-map/index.html)&lt;[ProtoType](../-proto-type/index.md), [MutableSet](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-set/index.html)&lt;[ProtoMember](../-proto-member/index.md)&gt;&gt;<br>The members to retain. Any member not in here should be pruned! |
| [pruningRules](pruning-rules.md) | [common]<br>val [pruningRules](pruning-rules.md): [PruningRules](../-pruning-rules/index.md) |
| [types](types.md) | [common]<br>val [types](types.md): [MutableSet](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-set/index.html)&lt;[ProtoType](../-proto-type/index.md)&gt;<br>The types to retain. We may retain a type but not all of its members. |
