//[wire-gson-support](../../index.md)/[com.squareup.wire](index.md)

# Package com.squareup.wire

## Types

| Name | Summary |
|---|---|
| [AnyMessageTypeAdapter](-any-message-type-adapter/index.md) | [jvm]<br>class [AnyMessageTypeAdapter](-any-message-type-adapter/index.md)(gson: Gson, typeUrlToAdapter: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ProtoAdapter&lt;*&gt;&gt;) : TypeAdapter&lt;AnyMessage&gt; |
| [WireTypeAdapterFactory](-wire-type-adapter-factory/index.md) | [jvm]<br>class [WireTypeAdapterFactory](-wire-type-adapter-factory/index.md)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(typeUrlToAdapter: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ProtoAdapter&lt;*&gt;&gt;, writeIdentityValues: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) : TypeAdapterFactory<br>A TypeAdapterFactory that allows Wire messages to be serialized and deserialized using the GSON Json library. To create a Gson instance that works with Wire, use the com.google.gson.GsonBuilder interface: |
