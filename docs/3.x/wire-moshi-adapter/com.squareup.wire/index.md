//[wire-moshi-adapter](../../index.md)/[com.squareup.wire](index.md)

# Package com.squareup.wire

## Types

| Name | Summary |
|---|---|
| [WireJsonAdapterFactory](-wire-json-adapter-factory/index.md) | [jvm]<br>class [WireJsonAdapterFactory](-wire-json-adapter-factory/index.md)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(typeUrlToAdapter: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ProtoAdapter&lt;*&gt;&gt;, writeIdentityValues: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) : JsonAdapter.Factory<br>A JsonAdapter.Factory that allows Wire messages to be serialized and deserialized using the Moshi Json library. |

## Functions

| Name | Summary |
|---|---|
| [redacting](redacting.md) | [jvm]<br>fun &lt;[T](redacting.md)&gt; JsonAdapter&lt;[T](redacting.md)&gt;.[redacting](redacting.md)(): JsonAdapter&lt;[T](redacting.md)&gt; |
