//[wire-moshi-adapter](../../../index.md)/[com.squareup.wire](../index.md)/[WireJsonAdapterFactory](index.md)/[plus](plus.md)

# plus

[jvm]\
fun [plus](plus.md)(adapters: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;ProtoAdapter&lt;*&gt;&gt;): [WireJsonAdapterFactory](index.md)

Returns a new WireJsonAdapterFactory that can encode the messages for [adapters](plus.md) if they're used with AnyMessage.

[jvm]\
fun [plus](plus.md)(adapter: ProtoAdapter&lt;*&gt;): [WireJsonAdapterFactory](index.md)

Returns a new WireJsonAdapterFactory that can encode the messages for [adapter](plus.md) if they're used with AnyMessage.
