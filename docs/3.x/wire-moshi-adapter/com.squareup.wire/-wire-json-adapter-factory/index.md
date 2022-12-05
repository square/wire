//[wire-moshi-adapter](../../../index.md)/[com.squareup.wire](../index.md)/[WireJsonAdapterFactory](index.md)

# WireJsonAdapterFactory

[jvm]\
class [WireJsonAdapterFactory](index.md)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(typeUrlToAdapter: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ProtoAdapter&lt;*&gt;&gt;, writeIdentityValues: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) : JsonAdapter.Factory

A JsonAdapter.Factory that allows Wire messages to be serialized and deserialized using the Moshi Json library.

Moshi moshi = new Moshi.Builder()\
    .add(new WireJsonAdapterFactory())\
    .build();

The resulting Moshi instance will be able to serialize and deserialize Wire Message types, including extensions. It ignores unknown field values. The JSON encoding is intended to be compatible with the [protobuf-java-format](https://code.google.com/p/protobuf-java-format/) library.

In Proto3, if a field is set to its default (or identity) value, it will be omitted in the JSON-encoded data. Set writeIdentityValues to true if you want Wire to always write values, including default ones.

## Constructors

| | |
|---|---|
| [WireJsonAdapterFactory](-wire-json-adapter-factory.md) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [WireJsonAdapterFactory](-wire-json-adapter-factory.md)(typeUrlToAdapter: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ProtoAdapter&lt;*&gt;&gt; = mapOf(), writeIdentityValues: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) |

## Functions

| Name | Summary |
|---|---|
| [create](create.md) | [jvm]<br>open override fun [create](create.md)(type: [Type](https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Type.html), annotations: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[Annotation](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-annotation/index.html)&gt;, moshi: Moshi): JsonAdapter&lt;*&gt;? |
| [plus](plus.md) | [jvm]<br>fun [plus](plus.md)(adapter: ProtoAdapter&lt;*&gt;): [WireJsonAdapterFactory](index.md)<br>Returns a new WireJsonAdapterFactory that can encode the messages for [adapter](plus.md) if they're used with AnyMessage.<br>[jvm]<br>fun [plus](plus.md)(adapters: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;ProtoAdapter&lt;*&gt;&gt;): [WireJsonAdapterFactory](index.md)<br>Returns a new WireJsonAdapterFactory that can encode the messages for [adapters](plus.md) if they're used with AnyMessage. |
