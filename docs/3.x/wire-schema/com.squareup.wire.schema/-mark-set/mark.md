//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[MarkSet](index.md)/[mark](mark.md)

# mark

[common]\
fun [mark](mark.md)(type: [ProtoType](../-proto-type/index.md), reference: [ProtoMember](../-proto-member/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Marks a type as transitively reachable by the includes set. Returns true if the mark is new, the type will be retained, and reachable objects should be traversed.

If there is an exclude for [type](mark.md), non-root members referencing it will be pruned. The type itself will also be pruned unless it is referenced by a root member.

[common]\
fun [mark](mark.md)(type: [ProtoType](../-proto-type/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Marks a type as transitively reachable by the includes set. Returns true if the mark is new, the type will be retained, and reachable objects should be traversed.

[common]\
fun [mark](mark.md)(protoMember: [ProtoMember](../-proto-member/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Marks a member as transitively reachable by the includes set. Returns true if the mark is new, the member will be retained, and reachable objects should be traversed.
