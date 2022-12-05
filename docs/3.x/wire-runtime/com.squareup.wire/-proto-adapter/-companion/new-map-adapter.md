//[wire-runtime](../../../../index.md)/[com.squareup.wire](../../index.md)/[ProtoAdapter](../index.md)/[Companion](index.md)/[newMapAdapter](new-map-adapter.md)

# newMapAdapter

[common, js, native]\
[common]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun &lt;[K](new-map-adapter.md), [V](new-map-adapter.md)&gt; [newMapAdapter](new-map-adapter.md)(keyAdapter: [ProtoAdapter](../index.md)&lt;[K](new-map-adapter.md)&gt;, valueAdapter: [ProtoAdapter](../index.md)&lt;[V](new-map-adapter.md)&gt;): [ProtoAdapter](../index.md)&lt;[Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[K](new-map-adapter.md), [V](new-map-adapter.md)&gt;&gt;

[js, native]\
fun &lt;[K](new-map-adapter.md), [V](new-map-adapter.md)&gt; [newMapAdapter](new-map-adapter.md)(keyAdapter: [ProtoAdapter](../index.md)&lt;[K](new-map-adapter.md)&gt;, valueAdapter: [ProtoAdapter](../index.md)&lt;[V](new-map-adapter.md)&gt;): [ProtoAdapter](../index.md)&lt;[Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[K](new-map-adapter.md), [V](new-map-adapter.md)&gt;&gt;

Creates a new proto adapter for a map using keyAdapter and valueAdapter.

Note: Map entries are not required to be encoded sequentially. Thus, when decoding using the returned adapter, only single-element maps will be returned and it is the caller's responsibility to merge them into the final map.

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun &lt;[K](new-map-adapter.md), [V](new-map-adapter.md)&gt; [newMapAdapter](new-map-adapter.md)(keyAdapter: [ProtoAdapter](../index.md)&lt;[K](new-map-adapter.md)&gt;, valueAdapter: [ProtoAdapter](../index.md)&lt;[V](new-map-adapter.md)&gt;): [ProtoAdapter](../index.md)&lt;[Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[K](new-map-adapter.md), [V](new-map-adapter.md)&gt;&gt;
