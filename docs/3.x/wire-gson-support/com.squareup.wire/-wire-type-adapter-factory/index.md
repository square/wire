//[wire-gson-support](../../../index.md)/[com.squareup.wire](../index.md)/[WireTypeAdapterFactory](index.md)

# WireTypeAdapterFactory

[jvm]\
class [WireTypeAdapterFactory](index.md)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(typeUrlToAdapter: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ProtoAdapter&lt;*&gt;&gt;, writeIdentityValues: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) : TypeAdapterFactory

A TypeAdapterFactory that allows Wire messages to be serialized and deserialized using the GSON Json library. To create a Gson instance that works with Wire, use the com.google.gson.GsonBuilder interface:

Gson gson = new GsonBuilder()\
    .registerTypeAdapterFactory(new WireTypeAdapterFactory())\
    .create();

The resulting Gson instance will be able to serialize and deserialize any Wire Message type, including extensions and unknown field values. The JSON encoding is intended to be compatible with the [protobuf-java-format](https://code.google.com/p/protobuf-java-format/) library. Note that version 1.2 of that API has a [bug](https://code.google.com/p/protobuf-java-format/issues/detail?id=47) in the way it serializes unknown fields, so we use our own approach for this case.

In Proto3, if a field is set to its default (or identity) value, it will be omitted in the JSON-encoded data. Set writeIdentityValues to true if you want Wire to always write values, including default ones.

## Constructors

| | |
|---|---|
| [WireTypeAdapterFactory](-wire-type-adapter-factory.md) | [jvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [WireTypeAdapterFactory](-wire-type-adapter-factory.md)(typeUrlToAdapter: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ProtoAdapter&lt;*&gt;&gt; = mapOf(), writeIdentityValues: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) |

## Functions

| Name | Summary |
|---|---|
| [create](create.md) | [jvm]<br>open override fun &lt;[T](create.md)&gt; [create](create.md)(gson: Gson, type: TypeToken&lt;[T](create.md)&gt;): TypeAdapter&lt;[T](create.md)&gt;? |
| [plus](plus.md) | [jvm]<br>fun [plus](plus.md)(adapter: ProtoAdapter&lt;*&gt;): [WireTypeAdapterFactory](index.md)<br>Returns a new WireTypeAdapterFactory that can encode the messages for [adapter](plus.md) if they're used with AnyMessage.<br>[jvm]<br>fun [plus](plus.md)(adapters: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;ProtoAdapter&lt;*&gt;&gt;): [WireTypeAdapterFactory](index.md)<br>Returns a new WireJsonAdapterFactory that can encode the messages for [adapters](plus.md) if they're used with AnyMessage. |
