//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[PruningRules](index.md)/[prunes](prunes.md)

# prunes

[common]\
fun [prunes](prunes.md)(type: [ProtoType](../-proto-type/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Returns true if [type](prunes.md) should be pruned, even if it is a transitive dependency of a root. In that case, the referring member is also pruned.

[common]\
fun [prunes](prunes.md)(protoMember: [ProtoMember](../-proto-member/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Returns true if [protoMember](prunes.md) should be pruned.

[common]\
val [prunes](prunes.md): [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;
