//[wire-gson-support](../../../index.md)/[com.squareup.wire](../index.md)/[AnyMessageTypeAdapter](index.md)

# AnyMessageTypeAdapter

[jvm]\
class [AnyMessageTypeAdapter](index.md)(gson: Gson, typeUrlToAdapter: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ProtoAdapter&lt;*&gt;&gt;) : TypeAdapter&lt;AnyMessage&gt;

## Functions

| Name | Summary |
|---|---|
| [fromJson](index.md#-84750346%2FFunctions%2F-2088091651) | [jvm]<br>fun [fromJson](index.md#-84750346%2FFunctions%2F-2088091651)(p0: [Reader](https://docs.oracle.com/javase/8/docs/api/java/io/Reader.html)): AnyMessage<br>fun [fromJson](index.md#-2098220177%2FFunctions%2F-2088091651)(p0: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): AnyMessage |
| [fromJsonTree](index.md#1807865266%2FFunctions%2F-2088091651) | [jvm]<br>fun [fromJsonTree](index.md#1807865266%2FFunctions%2F-2088091651)(p0: JsonElement): AnyMessage |
| [nullSafe](index.md#8677363%2FFunctions%2F-2088091651) | [jvm]<br>fun [nullSafe](index.md#8677363%2FFunctions%2F-2088091651)(): TypeAdapter&lt;AnyMessage&gt; |
| [read](read.md) | [jvm]<br>open override fun [read](read.md)(reader: JsonReader): AnyMessage? |
| [toJson](index.md#2113254441%2FFunctions%2F-2088091651) | [jvm]<br>fun [toJson](index.md#2113254441%2FFunctions%2F-2088091651)(p0: AnyMessage): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>fun [toJson](index.md#402942447%2FFunctions%2F-2088091651)(p0: [Writer](https://docs.oracle.com/javase/8/docs/api/java/io/Writer.html), p1: AnyMessage) |
| [toJsonTree](index.md#1223453611%2FFunctions%2F-2088091651) | [jvm]<br>fun [toJsonTree](index.md#1223453611%2FFunctions%2F-2088091651)(p0: AnyMessage): JsonElement |
| [write](write.md) | [jvm]<br>open override fun [write](write.md)(writer: JsonWriter, value: AnyMessage?) |
