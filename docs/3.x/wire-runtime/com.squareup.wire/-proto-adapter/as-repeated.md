//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoAdapter](index.md)/[asRepeated](as-repeated.md)

# asRepeated

[common, js, native]\
[common, js, native]\
fun [asRepeated](as-repeated.md)(): [ProtoAdapter](index.md)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)&gt;&gt;

Returns an adapter for E but as a repeated value.

Note: Repeated items are not required to be encoded sequentially. Thus, when decoding using the returned adapter, only single-element lists will be returned and it is the caller's responsibility to merge them into the final list.

[jvm]\
fun [asRepeated](as-repeated.md)(): [ProtoAdapter](index.md)&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[E](index.md)&gt;&gt;
