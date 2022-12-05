//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[MessageType](index.md)/[retainLinked](retain-linked.md)

# retainLinked

[common]\
open override fun [retainLinked](retain-linked.md)(linkedTypes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[ProtoType](../-proto-type/index.md)&gt;, linkedFields: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Field](../-field/index.md)&gt;): [Type](../-type/index.md)?

Returns a copy of this containing only the types in [linkedTypes](retain-linked.md) and extensions in [linkedFields](retain-linked.md), or null if that set is empty. This will return an [EnclosingType](../-enclosing-type/index.md) if it is itself not linked, but its nested types are linked.

The returned type is a shadow of its former self. It it useful for linking against, but lacks most of the members of the original type.
